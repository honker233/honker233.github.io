<template>
  <div class="testcase-management">
    <div class="page-header">
      <h2>📋 测试用例管理</h2>
      <div class="header-actions">
        <el-select
          v-model="selectedRepository"
          placeholder="选择仓库"
          style="width: 200px; margin-right: 10px"
          @change="loadTestCases"
        >
          <el-option
            v-for="repo in repositories"
            :key="repo.id"
            :label="repo.name"
            :value="repo.id"
          />
        </el-select>
        <el-button 
          type="primary" 
          @click="handleUploadClick" 
          :disabled="repositories.length === 0"
        >
          <el-icon><Upload /></el-icon>
          上传用例
        </el-button>
      </div>
    </div>

    <el-card class="content-card">
      <el-table :data="testCases" stripe v-loading="loading">
        <el-table-column prop="caseName" label="用例名称" width="200" />
        <el-table-column prop="caseDescription" label="描述" show-overflow-tooltip />
        <el-table-column prop="caseType" label="类型" width="120">
          <template #default="scope">
            <el-tag :type="getTypeColor(scope.row.caseType)">
              {{ getTypeText(scope.row.caseType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="类名" width="150" />
        <el-table-column prop="methodName" label="方法名" width="150" />
        <el-table-column prop="priority" label="优先级" width="80">
          <template #default="scope">
            <el-rate v-model="scope.row.priority" :max="3" size="small" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="updatedTime" label="更新时间" width="160">
          <template #default="scope">
            {{ formatTime(scope.row.updatedTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button size="small" type="danger" @click="deleteTestCase(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传用例对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传测试用例"
      width="600px"
    >
      <div class="upload-section">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :on-change="handleFileChange"
          :limit="1"
          accept=".java,.csv,.xmind,.xlsx,.xls"
          drag
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持Java源码文件(.java)、CSV格式(.csv)、XMind文件(.xmind)、Excel文件(.xlsx/.xls)，文件大小不超过10MB
            </div>
          </template>
        </el-upload>
        
        <div v-if="uploadFile" class="file-info">
          <p><strong>选择的文件：</strong>{{ uploadFile.name }}</p>
          <p><strong>文件大小：</strong>{{ formatFileSize(uploadFile.size) }}</p>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showUploadDialog = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="uploadTestCases" 
            :loading="uploading"
            :disabled="!uploadFile"
          >
            上传
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

export default {
  name: 'TestCaseManagement',
  setup() {
    const repositories = ref([])
    const testCases = ref([])
    const selectedRepository = ref()
    const loading = ref(false)
    const uploading = ref(false)
    const showUploadDialog = ref(false)
    const uploadFile = ref(null)
    const uploadRef = ref()

    const getTypeColor = (type) => {
      const colorMap = {
        'UNIT_TEST': 'success',
        'INTEGRATION_TEST': 'warning',
        'MANUAL_TEST': 'info',
        'XMIND_TEST': 'primary'
      }
      return colorMap[type] || 'info'
    }

    const getTypeText = (type) => {
      const textMap = {
        'UNIT_TEST': '单元测试',
        'INTEGRATION_TEST': '集成测试',
        'MANUAL_TEST': '手动测试',
        'XMIND_TEST': 'XMind用例'
      }
      return textMap[type] || '未知'
    }

    const formatTime = (time) => {
      if (!time) return '-'
      return new Date(time).toLocaleString('zh-CN')
    }

    const formatFileSize = (size) => {
      if (size < 1024) return size + ' B'
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
      return (size / (1024 * 1024)).toFixed(1) + ' MB'
    }

    const loadRepositories = async () => {
      try {
        const response = await api.getRepositories()
        repositories.value = response.data.filter(repo => repo.status === 'READY')
      } catch (error) {
        console.error('Failed to load repositories:', error)
      }
    }

    const loadTestCases = async () => {
      if (!selectedRepository.value) {
        testCases.value = []
        return
      }
      
      try {
        loading.value = true
        const response = await api.getTestCases(selectedRepository.value)
        testCases.value = response.data
      } catch (error) {
        console.error('Failed to load test cases:', error)
      } finally {
        loading.value = false
      }
    }

    const handleFileChange = (file) => {
      uploadFile.value = file.raw
    }

    const handleUploadClick = () => {
      if (!selectedRepository.value && repositories.value.length > 0) {
        ElMessage.warning('请先选择一个仓库')
        return
      }
      showUploadDialog.value = true
    }

    const uploadTestCases = async () => {
      if (!uploadFile.value || !selectedRepository.value) return
      
      // 验证文件类型
      const fileName = uploadFile.value.name
      const allowedExtensions = ['.java', '.csv', '.xmind', '.xlsx', '.xls']
      const hasValidExtension = allowedExtensions.some(ext => fileName.toLowerCase().endsWith(ext))
      
      if (!hasValidExtension) {
        ElMessage.error('不支持的文件类型。请上传 .java, .csv, .xmind, .xlsx 或 .xls 文件')
        return
      }
      
      // 验证文件大小 (10MB)
      if (uploadFile.value.size > 10 * 1024 * 1024) {
        ElMessage.error('文件大小不能超过10MB')
        return
      }
      
      try {
        uploading.value = true
        console.log('Uploading file:', fileName, 'Size:', uploadFile.value.size, 'Type:', uploadFile.value.type)
        
        const response = await api.uploadTestCases(uploadFile.value, selectedRepository.value)
        ElMessage.success(response.data)
        showUploadDialog.value = false
        uploadFile.value = null
        uploadRef.value?.clearFiles()
        loadTestCases()
      } catch (error) {
        console.error('Failed to upload test cases:', error)
        const errorMsg = error.response?.data || error.message || '上传失败'
        ElMessage.error('上传失败: ' + errorMsg)
      } finally {
        uploading.value = false
      }
    }

    const deleteTestCase = async (testCase) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除测试用例 "${testCase.caseName}" 吗？`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        await api.deleteTestCase(testCase.id)
        ElMessage.success('测试用例删除成功')
        loadTestCases()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Failed to delete test case:', error)
        }
      }
    }

    onMounted(() => {
      loadRepositories()
    })

    return {
      repositories,
      testCases,
      selectedRepository,
      loading,
      uploading,
      showUploadDialog,
      uploadFile,
      uploadRef,
      getTypeColor,
      getTypeText,
      formatTime,
      formatFileSize,
      loadRepositories,
      loadTestCases,
      handleFileChange,
      handleUploadClick,
      uploadTestCases,
      deleteTestCase
    }
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
}

.content-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.upload-section {
  margin-bottom: 20px;
}

.file-info {
  margin-top: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.file-info p {
  margin: 5px 0;
  color: #606266;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
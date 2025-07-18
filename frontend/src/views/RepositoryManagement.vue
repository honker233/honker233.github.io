<template>
  <div class="repository-management">
    <div class="page-header">
      <h2>📁 代码仓库管理</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        添加仓库
      </el-button>
    </div>

    <el-card class="content-card">
      <el-table :data="repositories" stripe v-loading="loading">
        <el-table-column prop="name" label="仓库名称" width="150" />
        <el-table-column prop="gitUrl" label="Git地址" show-overflow-tooltip />
        <el-table-column prop="branch" label="分支" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastCommitId" label="最新提交" width="120">
          <template #default="scope">
            <span v-if="scope.row.lastCommitId">
              {{ scope.row.lastCommitId.substring(0, 8) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedTime" label="更新时间" width="160">
          <template #default="scope">
            {{ formatTime(scope.row.updatedTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button 
              size="small" 
              type="success" 
              @click="cloneRepository(scope.row)"
              :disabled="scope.row.status === 'CLONING' || scope.row.status === 'UPDATING'"
            >
              {{ scope.row.status === 'CREATED' ? '克隆' : '拉取' }}
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="deleteRepository(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加仓库对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加代码仓库"
      width="600px"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入仓库名称" />
        </el-form-item>
        <el-form-item label="Git地址" prop="gitUrl">
          <el-input v-model="form.gitUrl" placeholder="请输入Git仓库地址" />
        </el-form-item>
        <el-form-item label="分支" prop="branch">
          <el-input v-model="form.branch" placeholder="请输入分支名称，默认为main" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

export default {
  name: 'RepositoryManagement',
  setup() {
    const repositories = ref([])
    const loading = ref(false)
    const submitting = ref(false)
    const showAddDialog = ref(false)
    const formRef = ref()

    const form = reactive({
      name: '',
      gitUrl: '',
      branch: 'main'
    })

    const validateGitUrl = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入Git仓库地址'))
        return
      }
      
      // Git URL 格式验证
      const gitUrlPatterns = [
        /^https?:\/\/[a-zA-Z0-9.-]+\/[a-zA-Z0-9._/-]+\.git$/,  // HTTPS
        /^git@[a-zA-Z0-9.-]+:[a-zA-Z0-9._/-]+\.git$/,         // SSH
        /^https?:\/\/[a-zA-Z0-9.-]+\/[a-zA-Z0-9._/-]+$/       // HTTPS without .git
      ]
      
      const isValidGitUrl = gitUrlPatterns.some(pattern => pattern.test(value))
      
      if (!isValidGitUrl) {
        callback(new Error('请输入有效的Git仓库地址，例如：https://github.com/user/repo.git'))
        return
      }
      
      // 检查是否是常见的Git托管平台
      const supportedHosts = ['github.com', 'gitlab.com', 'bitbucket.org', 'gitee.com']
      const hasValidHost = supportedHosts.some(host => value.includes(host))
      
      if (!hasValidHost) {
        // 允许其他域名，但给出提示
        console.warn('使用了非常见Git托管平台的地址')
      }
      
      callback()
    }

    const rules = {
      name: [
        { required: true, message: '请输入仓库名称', trigger: 'blur' },
        { min: 2, max: 50, message: '仓库名称长度在2到50个字符', trigger: 'blur' },
        { pattern: /^[a-zA-Z0-9._-]+$/, message: '仓库名称只能包含字母、数字、点、下划线和连字符', trigger: 'blur' }
      ],
      gitUrl: [
        { required: true, message: '请输入Git仓库地址', trigger: 'blur' },
        { validator: validateGitUrl, trigger: 'blur' }
      ],
      branch: [
        { required: true, message: '请输入分支名称', trigger: 'blur' },
        { min: 1, max: 100, message: '分支名称长度在1到100个字符', trigger: 'blur' },
        { pattern: /^[a-zA-Z0-9._/-]+$/, message: '分支名称格式不正确', trigger: 'blur' }
      ]
    }

    const getStatusType = (status) => {
      const statusMap = {
        'CREATED': 'info',
        'CLONING': 'warning',
        'UPDATING': 'warning',
        'READY': 'success',
        'ERROR': 'danger'
      }
      return statusMap[status] || 'info'
    }

    const getStatusText = (status) => {
      const statusMap = {
        'CREATED': '已创建',
        'CLONING': '克隆中',
        'UPDATING': '更新中',
        'READY': '就绪',
        'ERROR': '错误'
      }
      return statusMap[status] || '未知'
    }

    const formatTime = (time) => {
      if (!time) return '-'
      return new Date(time).toLocaleString('zh-CN')
    }

    const loadRepositories = async () => {
      try {
        loading.value = true
        const response = await api.getRepositories()
        repositories.value = response.data
      } catch (error) {
        console.error('Failed to load repositories:', error)
      } finally {
        loading.value = false
      }
    }

    const submitForm = async () => {
      try {
        await formRef.value.validate()
        submitting.value = true
        
        await api.createRepository(form)
        ElMessage.success('仓库添加成功')
        showAddDialog.value = false
        loadRepositories()
      } catch (error) {
        console.error('Failed to create repository:', error)
      } finally {
        submitting.value = false
      }
    }

    const cloneRepository = async (repository) => {
      try {
        const action = repository.status === 'CREATED' ? '克隆' : '拉取'
        await ElMessageBox.confirm(
          `确定要${action}仓库 "${repository.name}" 吗？`,
          '确认操作',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const apiCall = repository.status === 'CREATED' 
          ? api.cloneRepository 
          : api.pullRepository
        
        await apiCall(repository.id)
        ElMessage.success(`${action}操作已开始，请稍候...`)
        
        // 定期检查状态
        const checkStatus = setInterval(async () => {
          try {
            const response = await api.getRepository(repository.id)
            const updatedRepo = response.data
            
            const index = repositories.value.findIndex(r => r.id === repository.id)
            if (index !== -1) {
              repositories.value[index] = updatedRepo
            }
            
            if (updatedRepo.status === 'READY' || updatedRepo.status === 'ERROR') {
              clearInterval(checkStatus)
              if (updatedRepo.status === 'READY') {
                ElMessage.success(`${action}完成`)
              } else {
                ElMessage.error(`${action}失败`)
              }
            }
          } catch (error) {
            clearInterval(checkStatus)
          }
        }, 2000)
        
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Failed to clone/pull repository:', error)
        }
      }
    }

    const deleteRepository = async (repository) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除仓库 "${repository.name}" 吗？`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        await api.deleteRepository(repository.id)
        ElMessage.success('仓库删除成功')
        loadRepositories()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Failed to delete repository:', error)
        }
      }
    }

    const resetForm = () => {
      form.name = ''
      form.gitUrl = ''
      form.branch = 'main'
      if (formRef.value) {
        formRef.value.clearValidate()
      }
    }

    onMounted(() => {
      loadRepositories()
    })

    return {
      repositories,
      loading,
      submitting,
      showAddDialog,
      formRef,
      form,
      rules,
      getStatusType,
      getStatusText,
      formatTime,
      loadRepositories,
      submitForm,
      cloneRepository,
      deleteRepository,
      resetForm
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

.content-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
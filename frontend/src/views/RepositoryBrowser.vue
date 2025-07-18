<template>
  <div class="repository-browser">
    <div class="page-header">
      <h2>📁 仓库文件浏览器</h2>
    </div>

    <!-- 仓库选择和统计信息 -->
    <el-card class="repo-selector-card">
      <div class="repo-selector">
        <el-select
          v-model="selectedRepository"
          placeholder="选择要浏览的仓库"
          style="width: 300px"
          @change="onRepositoryChange"
        >
          <el-option
            v-for="repo in repositories"
            :key="repo.id"
            :label="repo.name"
            :value="repo.id"
          />
        </el-select>
        
        <el-button 
          type="info" 
          @click="refreshStats"
          :loading="loadingStats"
          :disabled="!selectedRepository"
        >
          <el-icon><Refresh /></el-icon>
          刷新统计
        </el-button>
      </div>
      
      <!-- 仓库统计信息 -->
      <div v-if="repoStats" class="repo-stats">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="总文件数" :value="repoStats.totalFiles" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="目录数" :value="repoStats.totalDirectories" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="代码文件" :value="repoStats.codeFiles" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="总大小" :value="formatFileSize(repoStats.totalSize)" />
          </el-col>
        </el-row>
        
        <!-- 文件类型分布 -->
        <div class="file-types">
          <h4>文件类型分布</h4>
          <div class="type-tags">
            <el-tag
              v-for="(count, type) in repoStats.fileTypes"
              :key="type"
              :type="getTypeColor(type)"
              size="small"
              style="margin: 2px"
            >
              {{ type }}: {{ count }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 文件浏览器 -->
    <el-card v-if="selectedRepository" class="file-browser-card">
      <template #header>
        <div class="browser-header">
          <div class="breadcrumb">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item @click="navigateTo('')">
                <el-icon><HomeFilled /></el-icon>
                {{ currentRepoName }}
              </el-breadcrumb-item>
              <el-breadcrumb-item
                v-for="(part, index) in pathParts"
                :key="index"
                @click="navigateTo(pathParts.slice(0, index + 1).join('/'))"
              >
                {{ part }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="current-path">
            <el-input
              v-model="currentPath"
              placeholder="当前路径"
              readonly
              style="width: 300px"
            >
              <template #prepend>路径</template>
            </el-input>
          </div>
        </div>
      </template>

      <!-- 文件列表 -->
      <el-table 
        :data="fileList" 
        v-loading="loadingFiles"
        @row-click="handleFileClick"
        style="cursor: pointer"
      >
        <el-table-column label="名称" min-width="200">
          <template #default="scope">
            <div class="file-item">
              <el-icon v-if="scope.row.type === 'directory'" class="file-icon">
                <Folder />
              </el-icon>
              <el-icon v-else class="file-icon">
                <Document />
              </el-icon>
              <span class="file-name">{{ scope.row.name }}</span>
              <el-tag 
                v-if="scope.row.isCodeFile" 
                type="success" 
                size="small"
                style="margin-left: 10px"
              >
                代码
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.type === 'directory' ? 'primary' : 'info'" size="small">
              {{ scope.row.type === 'directory' ? '目录' : '文件' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="120">
          <template #default="scope">
            {{ scope.row.type === 'directory' ? '-' : formatFileSize(scope.row.size) }}
          </template>
        </el-table-column>
        <el-table-column label="修改时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.lastModified) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button
              v-if="scope.row.type === 'file' && scope.row.name !== '..'"
              size="small"
              type="primary"
              @click.stop="viewFile(scope.row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 文件内容查看对话框 -->
    <el-dialog
      v-model="showFileDialog"
      :title="currentFile?.fileName || '文件内容'"
      width="80%"
      :before-close="closeFileDialog"
    >
      <div v-if="currentFile">
        <div class="file-info">
          <el-descriptions :column="2" size="small">
            <el-descriptions-item label="文件名">{{ currentFile.fileName }}</el-descriptions-item>
            <el-descriptions-item label="大小">{{ formatFileSize(currentFile.size) }}</el-descriptions-item>
            <el-descriptions-item label="扩展名">{{ currentFile.extension || '无' }}</el-descriptions-item>
            <el-descriptions-item label="修改时间">{{ formatTime(currentFile.lastModified) }}</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div class="file-content">
          <pre><code>{{ currentFile.content }}</code></pre>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="closeFileDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

export default {
  name: 'RepositoryBrowser',
  setup() {
    const repositories = ref([])
    const selectedRepository = ref(null)
    const currentPath = ref('')
    const fileList = ref([])
    const repoStats = ref(null)
    const currentRepoName = ref('')
    const loadingFiles = ref(false)
    const loadingStats = ref(false)
    const showFileDialog = ref(false)
    const currentFile = ref(null)

    const pathParts = computed(() => {
      return currentPath.value ? currentPath.value.split('/').filter(p => p) : []
    })

    const loadRepositories = async () => {
      try {
        const response = await api.getRepositories()
        repositories.value = response.data.filter(repo => repo.status === 'READY')
      } catch (error) {
        console.error('Failed to load repositories:', error)
        ElMessage.error('加载仓库列表失败')
      }
    }

    const onRepositoryChange = () => {
      currentPath.value = ''
      fileList.value = []
      repoStats.value = null
      currentRepoName.value = repositories.value.find(r => r.id === selectedRepository.value)?.name || ''
      
      if (selectedRepository.value) {
        loadFiles('')
        loadStats()
      }
    }

    const loadFiles = async (path = '') => {
      if (!selectedRepository.value) return
      
      try {
        loadingFiles.value = true
        const response = await api.browseRepository(selectedRepository.value, path)
        fileList.value = response.data.items
        currentPath.value = response.data.currentPath
        currentRepoName.value = response.data.repositoryName
      } catch (error) {
        console.error('Failed to load files:', error)
        ElMessage.error('加载文件列表失败: ' + (error.response?.data || error.message))
      } finally {
        loadingFiles.value = false
      }
    }

    const loadStats = async () => {
      if (!selectedRepository.value) return
      
      try {
        loadingStats.value = true
        const response = await api.getRepositoryStats(selectedRepository.value)
        repoStats.value = response.data
      } catch (error) {
        console.error('Failed to load stats:', error)
        ElMessage.error('加载统计信息失败')
      } finally {
        loadingStats.value = false
      }
    }

    const refreshStats = () => {
      loadStats()
    }

    const navigateTo = (path) => {
      loadFiles(path)
    }

    const handleFileClick = (row) => {
      if (row.type === 'directory') {
        loadFiles(row.path)
      }
    }

    const viewFile = async (file) => {
      try {
        const response = await api.getFileContent(selectedRepository.value, file.path)
        currentFile.value = response.data
        showFileDialog.value = true
      } catch (error) {
        console.error('Failed to load file content:', error)
        ElMessage.error('加载文件内容失败: ' + (error.response?.data || error.message))
      }
    }

    const closeFileDialog = () => {
      showFileDialog.value = false
      currentFile.value = null
    }

    const formatFileSize = (size) => {
      if (size < 1024) return size + ' B'
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
      return (size / (1024 * 1024)).toFixed(1) + ' MB'
    }

    const formatTime = (timestamp) => {
      if (!timestamp) return '-'
      return new Date(timestamp).toLocaleString('zh-CN')
    }

    const getTypeColor = (type) => {
      const colors = {
        'java': 'danger',
        'js': 'warning',
        'ts': 'primary',
        'vue': 'success',
        'css': 'info',
        'html': 'warning',
        'md': 'info',
        'json': 'primary'
      }
      return colors[type] || ''
    }

    onMounted(() => {
      loadRepositories()
    })

    return {
      repositories,
      selectedRepository,
      currentPath,
      fileList,
      repoStats,
      currentRepoName,
      pathParts,
      loadingFiles,
      loadingStats,
      showFileDialog,
      currentFile,
      onRepositoryChange,
      navigateTo,
      handleFileClick,
      viewFile,
      closeFileDialog,
      refreshStats,
      formatFileSize,
      formatTime,
      getTypeColor
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
  font-weight: 600;
}

.repo-selector-card,
.file-browser-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.repo-selector {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.repo-stats {
  margin-top: 20px;
}

.file-types {
  margin-top: 15px;
}

.file-types h4 {
  margin-bottom: 10px;
  color: #303133;
}

.type-tags {
  max-height: 60px;
  overflow-y: auto;
}

.browser-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.breadcrumb .el-breadcrumb-item {
  cursor: pointer;
}

.file-item {
  display: flex;
  align-items: center;
}

.file-icon {
  margin-right: 8px;
  color: #409eff;
}

.file-name {
  flex: 1;
}

.file-info {
  margin-bottom: 20px;
}

.file-content {
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 15px;
  max-height: 500px;
  overflow: auto;
}

.file-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
}
</style>
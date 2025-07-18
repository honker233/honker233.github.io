<template>
  <div class="change-analysis">
    <div class="page-header">
      <h2>🔍 代码变更分析</h2>
    </div>

    <el-card class="analysis-card">
      <div class="analysis-form">
        <el-form :model="analysisForm" label-width="120px" :inline="false">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="选择仓库" required>
                <el-select
                  v-model="analysisForm.repositoryId"
                  placeholder="请选择仓库"
                  style="width: 100%"
                  @change="onRepositoryChange"
                >
                  <el-option
                    v-for="repo in repositories"
                    :key="repo.id"
                    :label="repo.name"
                    :value="repo.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :span="8">
              <el-form-item label="基础提交" required>
                <el-select
                  v-model="analysisForm.fromCommit"
                  filterable
                  allow-create
                  placeholder="选择或输入commit"
                  style="width: 100%"
                >
                  <el-option label="HEAD~1 (前一个提交)" value="HEAD~1" />
                  <el-option label="HEAD~2 (前两个提交)" value="HEAD~2" />
                  <el-option label="HEAD~3 (前三个提交)" value="HEAD~3" />
                  <el-option label="HEAD~5 (前五个提交)" value="HEAD~5" />
                  <el-option label="HEAD~10 (前十个提交)" value="HEAD~10" />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :span="8">
              <el-form-item label="目标提交" required>
                <el-select
                  v-model="analysisForm.toCommit"
                  filterable
                  allow-create
                  placeholder="选择或输入commit"
                  style="width: 100%"
                >
                  <el-option label="HEAD (最新提交)" value="HEAD" />
                  <el-option label="HEAD~1 (前一个提交)" value="HEAD~1" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row>
            <el-col :span="24">
              <el-form-item>
                <el-button 
                  type="primary" 
                  @click="analyzeChanges" 
                  :loading="analyzing"
                  :disabled="!canAnalyze"
                >
                  <el-icon><DataAnalysis /></el-icon>
                  分析变更
                </el-button>
                <el-button 
                  type="info" 
                  @click="refreshRepository"
                  :loading="refreshing"
                  :disabled="!analysisForm.repositoryId"
                >
                  <el-icon><Refresh /></el-icon>
                  刷新仓库
                </el-button>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>
    </el-card>

    <!-- 分析结果 -->
    <el-card v-if="codeChanges.length > 0" class="results-card">
      <template #header>
        <div class="card-header">
          <span>变更分析结果</span>
          <div class="stats">
            <el-tag type="info">共 {{ codeChanges.length }} 个文件变更</el-tag>
            <el-tag type="success">新增 {{ totalLinesAdded }} 行</el-tag>
            <el-tag type="warning">删除 {{ totalLinesDeleted }} 行</el-tag>
          </div>
        </div>
      </template>

      <!-- 变更详情表格 -->
      <div class="changes-table">
        <el-table :data="codeChanges" stripe style="width: 100%">
          <el-table-column prop="filePath" label="文件路径" show-overflow-tooltip />
          <el-table-column prop="changeType" label="变更类型" width="120">
            <template #default="scope">
              <el-tag :type="getChangeTypeColor(scope.row.changeType)">
                {{ getChangeTypeText(scope.row.changeType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="modulePath" label="模块" width="150" />
          <el-table-column prop="changedClasses" label="变更的类" width="200">
            <template #default="scope">
              <el-tag
                v-for="cls in getChangedItems(scope.row.changedClasses)"
                :key="cls"
                size="small"
                style="margin-right: 5px"
              >
                {{ cls }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="changedMethods" label="变更的方法" width="200">
            <template #default="scope">
              <el-tag
                v-for="method in getChangedItems(scope.row.changedMethods)"
                :key="method"
                size="small"
                type="success"
                style="margin-right: 5px"
              >
                {{ method }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="代码行数" width="120">
            <template #default="scope">
              <div class="lines-info">
                <span class="added">+{{ scope.row.linesAdded || 0 }}</span>
                <span class="deleted">-{{ scope.row.linesDeleted || 0 }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 空状态 -->
    <el-empty 
      v-if="!analyzing && codeChanges.length === 0" 
      description="暂无分析结果，请选择仓库并执行分析"
      :image-size="100"
    />
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

export default {
  name: 'ChangeAnalysis',
  setup() {
    const repositories = ref([])
    const codeChanges = ref([])
    const analyzing = ref(false)
    const refreshing = ref(false)

    const analysisForm = reactive({
      repositoryId: null,
      fromCommit: 'HEAD~1',
      toCommit: 'HEAD'
    })

    const canAnalyze = computed(() => {
      return analysisForm.repositoryId && 
             analysisForm.fromCommit && 
             analysisForm.toCommit &&
             analysisForm.fromCommit !== analysisForm.toCommit
    })

    const totalLinesAdded = computed(() => {
      return codeChanges.value.reduce((sum, change) => sum + (change.linesAdded || 0), 0)
    })

    const totalLinesDeleted = computed(() => {
      return codeChanges.value.reduce((sum, change) => sum + (change.linesDeleted || 0), 0)
    })

    const getChangeTypeColor = (type) => {
      const colorMap = {
        'ADD': 'success',
        'MODIFY': 'warning',
        'DELETE': 'danger',
        'RENAME': 'info',
        'COPY': 'info'
      }
      return colorMap[type] || 'info'
    }

    const getChangeTypeText = (type) => {
      const textMap = {
        'ADD': '新增',
        'MODIFY': '修改',
        'DELETE': '删除',
        'RENAME': '重命名',
        'COPY': '复制'
      }
      return textMap[type] || type
    }

    const getChangedItems = (items) => {
      if (!items) return []
      return items.split(',').map(item => item.trim()).filter(item => item)
    }

    const loadRepositories = async () => {
      try {
        const response = await api.getRepositories()
        repositories.value = response.data.filter(repo => repo.status === 'READY')
      } catch (error) {
        console.error('Failed to load repositories:', error)
      }
    }

    const onRepositoryChange = () => {
      // 清空之前的分析结果
      codeChanges.value = []
    }

    const refreshRepository = async () => {
      if (!analysisForm.repositoryId) return
      
      try {
        refreshing.value = true
        await api.pullRepository(analysisForm.repositoryId)
        ElMessage.success('仓库刷新成功')
        
        // 刷新后清空分析结果
        codeChanges.value = []
      } catch (error) {
        console.error('Failed to refresh repository:', error)
        ElMessage.error('仓库刷新失败')
      } finally {
        refreshing.value = false
      }
    }

    const analyzeChanges = async () => {
      if (!canAnalyze.value) {
        ElMessage.warning('请选择仓库并确保提交信息正确')
        return
      }

      if (analysisForm.fromCommit === analysisForm.toCommit) {
        ElMessage.warning('基础提交和目标提交不能相同')
        return
      }

      try {
        analyzing.value = true
        
        // 先确保仓库是最新的
        const selectedRepo = repositories.value.find(r => r.id === analysisForm.repositoryId)
        if (selectedRepo && selectedRepo.status !== 'READY') {
          ElMessage.warning('仓库未就绪，请先等待仓库克隆完成')
          return
        }
        
        const response = await api.analyzeGitChanges({
          repositoryId: analysisForm.repositoryId,
          fromCommit: analysisForm.fromCommit,
          toCommit: analysisForm.toCommit
        })
        
        codeChanges.value = response.data || []
        
        if (codeChanges.value.length === 0) {
          ElMessage.info('未发现代码变更，可能是：\n1. 两个提交之间没有差异\n2. 提交ID不存在\n3. 只有非代码文件的变更')
        } else {
          ElMessage.success(`分析完成！发现 ${codeChanges.value.length} 个文件变更`)
        }
      } catch (error) {
        console.error('Failed to analyze changes:', error)
        const errorMsg = error.response?.data || error.message || '未知错误'
        ElMessage.error(`代码变更分析失败：${errorMsg}`)
        codeChanges.value = []
      } finally {
        analyzing.value = false
      }
    }

    onMounted(() => {
      loadRepositories()
    })

    return {
      repositories,
      codeChanges,
      analyzing,
      refreshing,
      analysisForm,
      canAnalyze,
      totalLinesAdded,
      totalLinesDeleted,
      getChangeTypeColor,
      getChangeTypeText,
      getChangedItems,
      loadRepositories,
      onRepositoryChange,
      refreshRepository,
      analyzeChanges
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

.analysis-card,
.results-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.analysis-form {
  padding: 10px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats {
  display: flex;
  gap: 10px;
}

.changes-table h4 {
  margin-bottom: 15px;
  color: #303133;
}

.lines-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.lines-info .added {
  color: #67c23a;
  font-weight: bold;
}

.lines-info .deleted {
  color: #f56c6c;
  font-weight: bold;
}
</style>
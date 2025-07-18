<template>
  <div class="testcase-recommendations">
    <div class="page-header">
      <h2>⭐ 智能用例推荐</h2>
    </div>

    <el-card class="recommendation-card">
      <div class="recommendation-form">
        <el-form :model="recommendationForm" label-width="120px" :inline="true">
          <el-form-item label="选择仓库">
            <el-select
              v-model="recommendationForm.repositoryId"
              placeholder="请选择仓库"
              style="width: 200px"
            >
              <el-option
                v-for="repo in repositories"
                :key="repo.id"
                :label="repo.name"
                :value="repo.id"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="基础提交">
            <el-input
              v-model="recommendationForm.fromCommit"
              placeholder="如: HEAD~1 或 commit hash"
              style="width: 200px"
            />
          </el-form-item>
          
          <el-form-item label="目标提交">
            <el-input
              v-model="recommendationForm.toCommit"
              placeholder="如: HEAD 或 commit hash"
              style="width: 200px"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              @click="getRecommendations" 
              :loading="recommending"
              :disabled="!recommendationForm.repositoryId"
            >
              <el-icon><Star /></el-icon>
              获取推荐
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 推荐结果 -->
    <el-card v-if="recommendations.length > 0" class="results-card">
      <template #header>
        <div class="card-header">
          <span>测试用例推荐结果</span>
          <div class="stats">
            <el-tag type="info">推荐用例: {{ recommendations.length }}</el-tag>
            <el-tag type="danger">高影响: {{ getCountByImpact('HIGH') }}</el-tag>
            <el-tag type="warning">中影响: {{ getCountByImpact('MEDIUM') }}</el-tag>
            <el-tag type="success">低影响: {{ getCountByImpact('LOW') }}</el-tag>
          </div>
        </div>
      </template>

      <!-- 推荐列表 -->
      <div class="recommendations-list">
        <el-table :data="recommendations" stripe style="width: 100%">
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="testCase.caseName" label="用例名称" width="200" />
          <el-table-column prop="testCase.caseDescription" label="用例描述" show-overflow-tooltip />
          <el-table-column prop="matchScore" label="匹配分数" width="120">
            <template #default="scope">
              <div class="score-display">
                <el-progress 
                  :percentage="Math.round(scope.row.matchScore * 100)" 
                  :color="getScoreColor(scope.row.matchScore)"
                  :show-text="false"
                  style="width: 60px"
                />
                <span class="score-text">{{ (scope.row.matchScore * 100).toFixed(1) }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="impactLevel" label="影响级别" width="100">
            <template #default="scope">
              <el-tag :type="getImpactColor(scope.row.impactLevel)">
                {{ getImpactText(scope.row.impactLevel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="testCase.caseType" label="用例类型" width="120">
            <template #default="scope">
              <el-tag :type="getTypeColor(scope.row.testCase.caseType)">
                {{ getTypeText(scope.row.testCase.caseType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="matchReason" label="匹配原因" show-overflow-tooltip>
            <template #default="scope">
              <el-tooltip placement="top">
                <template #content>
                  <div style="max-width: 400px;">
                    {{ scope.row.matchReason }}
                  </div>
                </template>
                <span class="reason-text">{{ scope.row.matchReason }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button 
                size="small" 
                type="primary" 
                @click="addToTestPlan(scope.row)"
              >
                <el-icon><Plus /></el-icon>
                加入计划
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 空状态 -->
    <el-empty 
      v-if="!recommending && recommendations.length === 0" 
      description="暂无推荐结果，请选择仓库并获取推荐"
      :image-size="100"
    />

    <!-- 测试计划显示 -->
    <el-card v-if="selectedTestCases.length > 0" class="test-plan-card">
      <template #header>
        <div class="card-header">
          <span>📝 测试执行计划 ({{ selectedTestCases.length }} 个用例)</span>
          <el-button @click="clearTestPlan" type="danger" size="small">
            清空计划
          </el-button>
        </div>
      </template>
      
      <el-table :data="selectedTestCases" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="testCase.caseName" label="用例名称" />
        <el-table-column prop="matchScore" label="匹配分数" width="120">
          <template #default="scope">
            {{ (scope.row.matchScore * 100).toFixed(1) }}%
          </template>
        </el-table-column>
        <el-table-column prop="impactLevel" label="影响级别" width="100">
          <template #default="scope">
            <el-tag :type="getImpactColor(scope.row.impactLevel)">
              {{ getImpactText(scope.row.impactLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="scope">
            <el-button 
              size="small" 
              type="danger" 
              @click="removeFromTestPlan(scope.$index)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="test-plan-actions" style="margin-top: 20px; text-align: center;">
        <el-button type="success" size="large" @click="executeTestPlan">
          ▶️ 执行测试计划
        </el-button>
        <el-button @click="exportTestPlan">
          📄 导出计划
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

export default {
  name: 'TestCaseRecommendations',
  setup() {
    const repositories = ref([])
    const recommendations = ref([])
    const selectedTestCases = ref([])
    const recommending = ref(false)

    const recommendationForm = reactive({
      repositoryId: null,
      fromCommit: 'HEAD~1',
      toCommit: 'HEAD'
    })

    const getCountByImpact = (impact) => {
      return recommendations.value.filter(rec => rec.impactLevel === impact).length
    }

    const getScoreColor = (score) => {
      if (score >= 0.7) return '#67c23a'
      if (score >= 0.4) return '#e6a23c'
      return '#f56c6c'
    }

    const getImpactColor = (impact) => {
      const colorMap = {
        'HIGH': 'danger',
        'MEDIUM': 'warning',
        'LOW': 'info'
      }
      return colorMap[impact] || 'info'
    }

    const getImpactText = (impact) => {
      const textMap = {
        'HIGH': '高影响',
        'MEDIUM': '中影响',
        'LOW': '低影响'
      }
      return textMap[impact] || impact
    }

    const getTypeColor = (type) => {
      const colorMap = {
        'UNIT_TEST': 'success',
        'INTEGRATION_TEST': 'warning',
        'MANUAL_TEST': 'info'
      }
      return colorMap[type] || 'info'
    }

    const getTypeText = (type) => {
      const textMap = {
        'UNIT_TEST': '单元测试',
        'INTEGRATION_TEST': '集成测试',
        'MANUAL_TEST': '手动测试'
      }
      return textMap[type] || '未知'
    }

    const loadRepositories = async () => {
      try {
        const response = await api.getRepositories()
        repositories.value = response.data.filter(repo => repo.status === 'READY')
      } catch (error) {
        console.error('Failed to load repositories:', error)
      }
    }

    const getRecommendations = async () => {
      if (!recommendationForm.repositoryId) {
        ElMessage.warning('请先选择一个仓库')
        return
      }

      if (!recommendationForm.fromCommit || !recommendationForm.toCommit) {
        ElMessage.warning('请输入有效的提交ID')
        return
      }

      try {
        recommending.value = true
        const response = await api.recommendTestCases({
          repositoryId: recommendationForm.repositoryId,
          fromCommit: recommendationForm.fromCommit,
          toCommit: recommendationForm.toCommit
        })
        
        recommendations.value = response.data
        
        if (recommendations.value.length === 0) {
          ElMessage.info('未找到相关的测试用例推荐')
        } else {
          ElMessage.success(`获取到 ${recommendations.value.length} 个测试用例推荐`)
        }
      } catch (error) {
        console.error('Failed to get recommendations:', error)
        ElMessage.error('获取推荐失败，请检查提交ID是否正确')
      } finally {
        recommending.value = false
      }
    }

    const addToTestPlan = (recommendation) => {
      const exists = selectedTestCases.value.find(
        tc => tc.testCase.id === recommendation.testCase.id
      )
      
      if (exists) {
        ElMessage.warning('该用例已在测试计划中')
        return
      }
      
      selectedTestCases.value.push(recommendation)
      ElMessage.success('已添加到测试计划')
    }

    const removeFromTestPlan = (index) => {
      selectedTestCases.value.splice(index, 1)
      ElMessage.success('已从测试计划中移除')
    }

    const clearTestPlan = () => {
      selectedTestCases.value = []
      ElMessage.success('测试计划已清空')
    }

    const executeTestPlan = async () => {
      if (selectedTestCases.value.length === 0) {
        ElMessage.warning('测试计划为空')
        return
      }
      
      try {
        await ElMessageBox.confirm(
          `确定要执行包含 ${selectedTestCases.value.length} 个用例的测试计划吗？`,
          '确认执行',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        ElMessage.success('测试计划已开始执行（演示功能）')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Failed to execute test plan:', error)
        }
      }
    }

    const exportTestPlan = () => {
      const data = selectedTestCases.value.map(rec => ({
        '用例名称': rec.testCase.caseName,
        '用例描述': rec.testCase.caseDescription,
        '匹配分数': (rec.matchScore * 100).toFixed(1) + '%',
        '影响级别': getImpactText(rec.impactLevel),
        '用例类型': getTypeText(rec.testCase.caseType),
        '匹配原因': rec.matchReason
      }))
      
      const csv = [
        Object.keys(data[0]).join(','),
        ...data.map(row => Object.values(row).join(','))
      ].join('\n')
      
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `测试计划_${new Date().toISOString().slice(0, 10)}.csv`
      link.click()
      
      ElMessage.success('测试计划已导出')
    }

    onMounted(() => {
      loadRepositories()
    })

    return {
      repositories,
      recommendations,
      selectedTestCases,
      recommending,
      recommendationForm,
      getCountByImpact,
      getScoreColor,
      getImpactColor,
      getImpactText,
      getTypeColor,
      getTypeText,
      loadRepositories,
      getRecommendations,
      addToTestPlan,
      removeFromTestPlan,
      clearTestPlan,
      executeTestPlan,
      exportTestPlan
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

.recommendation-card,
.results-card,
.test-plan-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.recommendation-form {
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

.score-display {
  display: flex;
  align-items: center;
  gap: 10px;
}

.score-text {
  font-weight: bold;
  min-width: 50px;
}

.reason-text {
  display: block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
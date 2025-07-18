import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.response.use(
  response => response,
  error => {
    const message = error.response?.data || error.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default {
  // 仓库管理
  getRepositories() {
    return api.get('/repositories')
  },
  
  getRepository(id) {
    return api.get(`/repositories/${id}`)
  },
  
  createRepository(repository) {
    return api.post('/repositories', repository)
  },
  
  cloneRepository(id) {
    return api.post(`/repositories/${id}/clone`)
  },
  
  pullRepository(id) {
    return api.post(`/repositories/${id}/pull`)
  },
  
  deleteRepository(id) {
    return api.delete(`/repositories/${id}`)
  },

  // 测试用例管理
  getTestCases(repositoryId) {
    const params = repositoryId ? { repositoryId } : {}
    return api.get('/testcases', { params })
  },
  
  uploadTestCases(file, repositoryId) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('repositoryId', repositoryId)
    return api.post('/testcases/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  createTestCase(testCase) {
    return api.post('/testcases', testCase)
  },
  
  updateTestCase(id, testCase) {
    return api.put(`/testcases/${id}`, testCase)
  },
  
  deleteTestCase(id) {
    return api.delete(`/testcases/${id}`)
  },

  // 分析服务
  analyzeGitChanges(request) {
    return api.post('/analysis/git-changes', request)
  },
  
  recommendTestCases(request) {
    return api.post('/analysis/recommend-testcases', request)
  },
  
  getCodeChanges(repositoryId, commitId) {
    const params = { repositoryId }
    if (commitId) params.commitId = commitId
    return api.get('/analysis/code-changes', { params })
  },

  // 文件浏览
  browseRepository(repositoryId, path = '') {
    const params = path ? { path } : {}
    return api.get(`/files/browse/${repositoryId}`, { params })
  },
  
  getFileContent(repositoryId, filePath) {
    return api.get(`/files/content/${repositoryId}`, { params: { filePath } })
  },
  
  getRepositoryStats(repositoryId) {
    return api.get(`/files/stats/${repositoryId}`)
  }
}
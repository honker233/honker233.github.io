import { createRouter, createWebHistory } from 'vue-router'
import RepositoryManagement from '../views/RepositoryManagement.vue'
import TestCaseManagement from '../views/TestCaseManagement.vue'
import ChangeAnalysis from '../views/ChangeAnalysis.vue'
import TestCaseRecommendations from '../views/TestCaseRecommendations.vue'
import RepositoryBrowser from '../views/RepositoryBrowser.vue'

const routes = [
  {
    path: '/',
    name: 'RepositoryManagement',
    component: RepositoryManagement
  },
  {
    path: '/testcases',
    name: 'TestCaseManagement',
    component: TestCaseManagement
  },
  {
    path: '/analysis',
    name: 'ChangeAnalysis',
    component: ChangeAnalysis
  },
  {
    path: '/recommendations',
    name: 'TestCaseRecommendations',
    component: TestCaseRecommendations
  },
  {
    path: '/browser',
    name: 'RepositoryBrowser',
    component: RepositoryBrowser
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
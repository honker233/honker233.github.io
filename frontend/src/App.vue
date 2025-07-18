<template>
  <div id="app">
    <el-container class="layout-container">
      <el-header class="header">
        <div class="header-content">
          <h1 class="title">🧠 智能测试用例选择工具</h1>
          <div class="header-right">
            <span class="version">本地版</span>
          </div>
        </div>
      </el-header>
      
      <el-container>
        <el-aside width="250px" class="sidebar">
          <el-menu
            :default-active="$route.path"
            class="sidebar-menu"
            router
          >
            <el-menu-item index="/">
              <el-icon><Folder /></el-icon>
              <span>仓库管理</span>
            </el-menu-item>
            <el-menu-item index="/testcases">
              <el-icon><Document /></el-icon>
              <span>测试用例</span>
            </el-menu-item>
            <el-menu-item index="/analysis">
              <el-icon><DataAnalysis /></el-icon>
              <span>变更分析</span>
            </el-menu-item>
            <el-menu-item index="/browser">
              <el-icon><Files /></el-icon>
              <span>文件浏览</span>
            </el-menu-item>
            <el-menu-item index="/recommendations">
              <el-icon><Star /></el-icon>
              <span>智能推荐</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.layout-container {
  height: 100vh;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.title {
  font-size: 24px;
  font-weight: 500;
}

.version {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.sidebar {
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
}

.sidebar-menu {
  border: none;
  background: transparent;
}

.main-content {
  background: #f9fafc;
  padding: 20px;
  overflow-y: auto;
}

.el-menu-item {
  margin: 8px 12px;
  border-radius: 8px;
}

.el-menu-item:hover {
  background: #e8f4ff;
  color: #409eff;
}

.el-menu-item.is-active {
  background: #409eff;
  color: white;
}
</style>
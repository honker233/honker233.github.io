# 智能测试用例选择工具 - 本地版

🧠 基于代码变更的智能测试用例推荐系统，无需Docker，本地直接运行！

## ✨ 功能特性

- 🔄 **Git代码变更分析** - 自动分析两个提交间的代码差异
- 🧠 **智能用例匹配** - 基于模块、类、方法等多维度算法推荐相关测试用例  
- 📊 **可视化分析** - 变更统计和影响分析
- 📁 **测试用例管理** - 支持Java源码和CSV格式用例上传
- 🎯 **精准推荐** - 根据影响级别智能排序
- 📈 **测试计划生成** - 自动生成优化的测试执行计划

## 🛠 技术架构

### 后端技术栈
- ☕ Java 11 + Spring Boot 2.7
- 🗃️ H2内存数据库（无需安装数据库）
- 🔧 JGit（Git操作库）
- 📦 Maven（项目管理）

### 前端技术栈  
- 🖼️ Vue 3 + JavaScript
- 🎨 Element Plus（UI组件库）
- 📡 Axios（HTTP请求）
- ⚡ Vite（开发服务器）

### 核心算法
- **模块匹配权重**: 40% - 基于包路径和模块结构
- **类匹配权重**: 30% - 基于类名相似度和继承关系
- **方法匹配权重**: 20% - 基于方法名和测试方法模式
- **文件路径权重**: 10% - 基于文件路径相似度

## 🚀 快速开始

### 环境要求
- ☕ Java 11+ 
- 📦 Maven 3.6+
- 📱 Node.js 16+
- 🔧 Git

### 一键启动

1. **克隆项目到本地**
```bash
# 假设你已经有了这个项目文件夹
cd smart-testcase-local
```

2. **一键启动所有服务**
```bash
./start.sh
```

3. **访问应用**
- 🖥️ **前端界面**: http://localhost:3000
- 🔧 **后端API**: http://localhost:8080
- 🗃️ **数据库控制台**: http://localhost:8080/h2-console

### 停止服务
```bash
./stop.sh
```

## 📖 使用指南

### 1. 📁 添加Git仓库
- 点击"添加仓库"按钮
- 输入仓库名称、Git地址和分支
- 点击"克隆"按钮拉取代码到本地

### 2. 📋 上传测试用例
- 选择已克隆的仓库
- 点击"上传用例"按钮
- 支持格式：
  - **Java源码文件** (.java) - 自动解析@Test注解
  - **CSV文件** (.csv) - 按指定格式上传

### 3. 🔍 分析代码变更
- 选择要分析的仓库
- 输入提交范围（如 HEAD~1 到 HEAD）
- 点击"分析变更"查看详细变更信息

### 4. ⭐ 获取智能推荐
- 在推荐页面选择仓库和提交范围
- 点击"获取推荐"
- 查看按匹配分数排序的测试用例推荐
- 将推荐用例加入测试计划

## 📄 测试用例格式

### Java源码文件示例
```java
package com.example.test;

import org.junit.Test;

public class UserServiceTest {
    
    @Test
    public void testUserLogin() {
        // 测试用户登录功能
    }
    
    @Test  
    public void testUserRegister() {
        // 测试用户注册功能
    }
}
```

### CSV格式示例
```csv
name,description,type,class,method,module,priority
testUserLogin,测试用户登录,UNIT_TEST,UserServiceTest,testUserLogin,com.example.user,2
testUserRegister,测试用户注册,UNIT_TEST,UserServiceTest,testUserRegister,com.example.user,1
testPasswordValidation,测试密码验证,UNIT_TEST,UserServiceTest,testPasswordValidation,com.example.user,3
```

## 🗃️ 数据库管理

本工具使用H2内存数据库，无需安装额外软件：

- **访问地址**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:testcase_db`
- **用户名**: `sa`
- **密码**: （空）

## 🐛 故障排除

### 端口冲突
如果8080或3000端口被占用：
```bash
# 查看端口占用
lsof -i :8080
lsof -i :3000

# 停止占用进程或修改配置文件中的端口
```

### Java环境问题
```bash
# 检查Java版本
java -version

# 需要Java 11或以上版本
```

### Node.js环境问题
```bash
# 检查Node.js版本
node -v

# 需要Node.js 16或以上版本
```

### 查看日志
```bash
# 查看后端日志
tail -f backend.log

# 查看前端日志  
tail -f frontend.log
```

## 📊 项目结构

```
smart-testcase-local/
├── backend/              # Spring Boot后端
│   ├── src/main/java/   # Java源码
│   ├── src/main/resources/ # 配置文件
│   └── pom.xml          # Maven依赖
├── frontend/            # Vue前端
│   ├── src/            # 前端源码
│   ├── package.json    # Node.js依赖
│   └── vite.config.js  # Vite配置
├── start.sh            # 启动脚本
├── stop.sh             # 停止脚本
└── README.md           # 本文档
```

## 🔧 高级配置

### 修改数据库配置
编辑 `backend/src/main/resources/application.yml` 文件：
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testcase_db
    # 可以改为文件数据库持久化数据
    # url: jdbc:h2:file:./data/testcase_db
```

### 修改服务端口
```yaml
# 后端端口 (backend/src/main/resources/application.yml)
server:
  port: 8080

# 前端端口 (frontend/vite.config.js)
server:
  port: 3000
```

## 🎯 核心优势

✅ **无Docker依赖** - 直接在本地环境运行  
✅ **零配置数据库** - 使用H2内存数据库  
✅ **一键启动** - 自动检查环境并启动所有服务  
✅ **智能算法** - 多维度匹配算法提高推荐准确性  
✅ **可视化界面** - 现代化Web界面，操作简单直观  
✅ **实时分析** - 快速分析Git代码变更  
✅ **测试计划** - 自动生成和导出测试执行计划  

## 📞 技术支持

遇到问题？请检查：
1. 🔍 查看控制台输出和日志文件
2. 🌐 确认网络连接正常
3. 🔧 验证Git仓库地址可访问
4. 📋 检查测试用例文件格式

---

**🎉 开始使用智能测试用例选择工具，提升你的测试效率！**
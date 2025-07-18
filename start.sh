#!/bin/bash

echo "=========================================="
echo "    智能测试用例选择工具 - 本地版本"
echo "=========================================="

# 检查Java环境
echo "检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到Java环境，请先安装Java 11或以上版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "❌ 错误: Java版本过低，需要Java 11或以上版本"
    exit 1
fi
echo "✅ Java环境检查通过"

# 检查Maven环境
echo "检查Maven环境..."
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: 未找到Maven，请先安装Maven"
    exit 1
fi
echo "✅ Maven环境检查通过"

# 检查Node.js环境
echo "检查Node.js环境..."
if ! command -v node &> /dev/null; then
    echo "❌ 错误: 未找到Node.js，请先安装Node.js 16或以上版本"
    exit 1
fi

NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 16 ]; then
    echo "❌ 错误: Node.js版本过低，需要Node.js 16或以上版本"
    exit 1
fi
echo "✅ Node.js环境检查通过"

# 启动后端服务
echo ""
echo "=========================================="
echo "正在启动后端服务..."
echo "=========================================="

cd backend

# 检查是否已编译
if [ ! -f "target/smart-testcase-local-1.0.0.jar" ]; then
    echo "首次运行，正在编译项目..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ 后端编译失败"
        exit 1
    fi
fi

# 启动Spring Boot应用
echo "启动Spring Boot应用..."
nohup java -jar target/smart-testcase-local-1.0.0.jar > ../backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > ../backend.pid

# 等待后端启动
echo "等待后端服务启动..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/repositories > /dev/null 2>&1; then
        echo "✅ 后端服务启动成功"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ 后端服务启动超时"
        kill $BACKEND_PID 2>/dev/null
        exit 1
    fi
    sleep 2
done

cd ..

# 启动前端服务
echo ""
echo "=========================================="
echo "正在启动前端服务..."
echo "=========================================="

cd frontend

# 检查依赖是否已安装
if [ ! -d "node_modules" ]; then
    echo "首次运行，正在安装前端依赖..."
    npm install
    if [ $? -ne 0 ]; then
        echo "❌ 前端依赖安装失败"
        kill $(cat ../backend.pid) 2>/dev/null
        exit 1
    fi
fi

# 启动前端开发服务器
echo "启动前端开发服务器..."
nohup npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > ../frontend.pid

# 等待前端启动
echo "等待前端服务启动..."
for i in {1..20}; do
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        echo "✅ 前端服务启动成功"
        break
    fi
    if [ $i -eq 20 ]; then
        echo "❌ 前端服务启动超时"
        kill $FRONTEND_PID 2>/dev/null
        kill $(cat ../backend.pid) 2>/dev/null
        exit 1
    fi
    sleep 2
done

cd ..

echo ""
echo "=========================================="
echo "🎉 智能测试用例选择工具启动成功！"
echo "=========================================="
echo ""
echo "📱 前端地址: http://localhost:3000"
echo "🔧 后端API: http://localhost:8080"
echo "🗃️  数据库控制台: http://localhost:8080/h2-console"
echo "   (JDBC URL: jdbc:h2:mem:testcase_db)"
echo "   (用户名: sa, 密码: 空)"
echo ""
echo "📋 使用说明:"
echo "1. 在浏览器中打开 http://localhost:3000"
echo "2. 添加Git仓库并克隆代码"
echo "3. 上传测试用例文件"
echo "4. 分析代码变更"
echo "5. 获取智能测试用例推荐"
echo ""
echo "⏹️  停止服务: ./stop.sh"
echo "📜 查看日志: tail -f backend.log 或 tail -f frontend.log"
echo ""
echo "=========================================="

# 保存服务信息
echo "BACKEND_PID=$(cat backend.pid)" > service.info
echo "FRONTEND_PID=$(cat frontend.pid)" >> service.info
echo "START_TIME=$(date)" >> service.info
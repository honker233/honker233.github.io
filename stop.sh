#!/bin/bash

echo "=========================================="
echo "    停止智能测试用例选择工具"
echo "=========================================="

# 停止后端服务
if [ -f "backend.pid" ]; then
    BACKEND_PID=$(cat backend.pid)
    echo "停止后端服务 (PID: $BACKEND_PID)..."
    kill $BACKEND_PID 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✅ 后端服务已停止"
    else
        echo "⚠️  后端服务可能已经停止"
    fi
    rm -f backend.pid
else
    echo "⚠️  未找到后端服务PID文件"
fi

# 停止前端服务
if [ -f "frontend.pid" ]; then
    FRONTEND_PID=$(cat frontend.pid)
    echo "停止前端服务 (PID: $FRONTEND_PID)..."
    kill $FRONTEND_PID 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✅ 前端服务已停止"
    else
        echo "⚠️  前端服务可能已经停止"
    fi
    rm -f frontend.pid
else
    echo "⚠️  未找到前端服务PID文件"
fi

# 清理临时文件
rm -f service.info

# 强制停止可能残留的进程
echo "清理可能残留的进程..."
pkill -f "smart-testcase-local-1.0.0.jar" 2>/dev/null
pkill -f "vite.*3000" 2>/dev/null

echo ""
echo "=========================================="
echo "🛑 智能测试用例选择工具已停止"
echo "=========================================="
#!/bin/bash

echo "========================================"
echo "      聊天系统启动脚本"
echo "========================================"
echo

echo "正在检查环境..."

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "[错误] 未找到Java环境，请先安装Java 17或更高版本"
    exit 1
fi

# 检查Node.js环境
if ! command -v node &> /dev/null; then
    echo "[错误] 未找到Node.js环境，请先安装Node.js"
    exit 1
fi

# 检查npm环境
if ! command -v npm &> /dev/null; then
    echo "[错误] 未找到npm，请确保Node.js安装正确"
    exit 1
fi

echo "[信息] 环境检查通过"

# 检查MySQL服务
echo "正在检查MySQL服务..."
if ! pgrep -x "mysqld" > /dev/null; then
    echo "[警告] MySQL服务未运行，请确保MySQL已启动"
    echo "请手动启动MySQL服务后继续"
    read -p "按Enter键继续..."
fi

echo
echo "========================================"
echo "      启动后端服务"
echo "========================================"

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# 启动后端
echo "正在启动后端服务..."
cd "$SCRIPT_DIR/backend"

if [ ! -d "target" ]; then
    echo "正在编译后端项目..."
    mvn clean compile
    if [ $? -ne 0 ]; then
        echo "[错误] 后端编译失败"
        exit 1
    fi
fi

echo "启动Spring Boot应用..."
# 在新终端窗口启动后端（适用于大多数Linux桌面环境）
if command -v gnome-terminal &> /dev/null; then
    gnome-terminal --title="Chat Backend" -- bash -c "mvn spring-boot:run; exec bash"
elif command -v xterm &> /dev/null; then
    xterm -title "Chat Backend" -e "mvn spring-boot:run; bash" &
elif command -v konsole &> /dev/null; then
    konsole --title "Chat Backend" -e bash -c "mvn spring-boot:run; exec bash" &
else
    # 如果没有图形终端，在后台启动
    echo "在后台启动后端服务..."
    nohup mvn spring-boot:run > backend.log 2>&1 &
    echo "后端日志输出到: backend.log"
fi

echo "等待后端服务启动..."
sleep 10

echo
echo "========================================"
echo "      启动前端服务"
echo "========================================"

# 启动前端
cd "$SCRIPT_DIR"
echo "正在检查前端依赖..."
if [ ! -d "node_modules" ]; then
    echo "正在安装前端依赖..."
    npm install
    if [ $? -ne 0 ]; then
        echo "[错误] 前端依赖安装失败"
        exit 1
    fi
fi

echo "启动前端开发服务器..."
# 在新终端窗口启动前端
if command -v gnome-terminal &> /dev/null; then
    gnome-terminal --title="Chat Frontend" -- bash -c "npm run dev; exec bash"
elif command -v xterm &> /dev/null; then
    xterm -title "Chat Frontend" -e "npm run dev; bash" &
elif command -v konsole &> /dev/null; then
    konsole --title "Chat Frontend" -e bash -c "npm run dev; exec bash" &
else
    # 如果没有图形终端，在后台启动
    echo "在后台启动前端服务..."
    nohup npm run dev > frontend.log 2>&1 &
    echo "前端日志输出到: frontend.log"
fi

echo
echo "========================================"
echo "      启动完成"
echo "========================================"
echo
echo "后端服务: http://localhost:8080"
echo "前端服务: http://localhost:5173"
echo "WebSocket: ws://localhost:8080/ws"
echo
echo "测试账号:"
echo "  用户名: zhangsan  密码: 123456"
echo "  用户名: lisi      密码: 123456"
echo "  用户名: wangwu    密码: 123456"
echo "  用户名: admin     密码: 123456"
echo
echo "请等待服务完全启动后访问前端地址进行测试"
echo "按Ctrl+C退出脚本"

# 等待用户输入
read -p "按Enter键退出..."
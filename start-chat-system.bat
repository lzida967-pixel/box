@echo off
echo ========================================
echo      聊天系统启动脚本
echo ========================================
echo.

echo 正在检查环境...

:: 检查Java环境
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Java环境，请先安装Java 17或更高版本
    pause
    exit /b 1
)

:: 检查Node.js环境
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Node.js环境，请先安装Node.js
    pause
    exit /b 1
)

:: 检查npm环境
npm --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到npm，请确保Node.js安装正确
    pause
    exit /b 1
)

echo [信息] 环境检查通过

:: 检查MySQL服务
echo 正在检查MySQL服务...
sc query mysql >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] MySQL服务未运行，请确保MySQL已启动
    echo 请手动启动MySQL服务后继续
    pause
)

echo.
echo ========================================
echo      启动后端服务
echo ========================================

:: 启动后端
echo 正在启动后端服务...
cd /d "%~dp0backend"
if not exist "target" (
    echo 正在编译后端项目...
    call mvn clean compile
    if %errorlevel% neq 0 (
        echo [错误] 后端编译失败
        pause
        exit /b 1
    )
)

echo 启动Spring Boot应用...
start "Chat Backend" cmd /k "mvn spring-boot:run"

echo 等待后端服务启动...
timeout /t 10 /nobreak >nul

echo.
echo ========================================
echo      启动前端服务
echo ========================================

:: 启动前端
cd /d "%~dp0"
echo 正在检查前端依赖...
if not exist "node_modules" (
    echo 正在安装前端依赖...
    call npm install
    if %errorlevel% neq 0 (
        echo [错误] 前端依赖安装失败
        pause
        exit /b 1
    )
)

echo 启动前端开发服务器...
start "Chat Frontend" cmd /k "npm run dev"

echo.
echo ========================================
echo      启动完成
echo ========================================
echo.
echo 后端服务: http://localhost:8080
echo 前端服务: http://localhost:5173
echo WebSocket: ws://localhost:8080/ws
echo.
echo 测试账号:
echo   用户名: zhangsan  密码: 123456
echo   用户名: lisi      密码: 123456
echo   用户名: wangwu    密码: 123456
echo   用户名: admin     密码: 123456
echo.
echo 请等待服务完全启动后访问前端地址进行测试
echo 按任意键退出...
pause >nul
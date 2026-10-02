@echo off
title 校园学习资源推荐平台 - 一键启动
echo ============================================
echo   校园学习资源推荐平台  一键启动
echo ============================================
echo.

echo [1/4] 检查 MySQL...
sc query MySQL80 | findstr /i "RUNNING" >nul
if %errorlevel%==0 (
    echo         MySQL 已在运行
) else (
    echo         正在启动 MySQL...
    net start MySQL80 >nul 2>&1
)

echo [2/4] 检查 Neo4j...
netstat -ano | findstr ":7687" | findstr "LISTENING" >nul
if %errorlevel%==0 (
    echo         Neo4j 已在运行
) else (
    echo         正在启动 Neo4j，请稍候...
    set "JAVA_HOME=D:\jaeger\jdk-21"
    call "D:\jaeger\neo4j\neo4j-community-2026.08.1\bin\neo4j.bat" start
)

echo [3/4] 检查后端...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if %errorlevel%==0 (
    echo         后端已在运行
) else (
    echo         正在启动后端，后台运行约需 10 秒...
    set "JAVA_HOME=C:\Program Files\Java\latest\jdk-26"
    start "backend" /min cmd /c "java -jar D:\jaeger\campus-recommend-1.0.0.jar --server.port=8080"
)

echo [4/4] 检查前端...
netstat -ano | findstr ":3000" | findstr "LISTENING" >nul
if %errorlevel%==0 (
    echo         前端已在运行
) else (
    echo         正在启动前端，将弹出 frontend 窗口，请勿关闭...
    start "frontend" cmd /k "cd /d D:\d盘桌面\基于知识图谱+协同过滤算法的校园学习资源推荐平台设计与实现\frontend && npm run dev"
)

echo.
echo ============================================
echo   启动指令已全部发出！
echo   请等待 15-20 秒，然后浏览器访问：
echo       http://localhost:3000
echo   学生账号：linhaoran / 123456
echo   管理员账号：admin / admin123
echo ============================================
echo   注意：frontend 窗口请保持打开，本窗口可关闭。
echo.
pause

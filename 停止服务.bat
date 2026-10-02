@echo off
title 校园学习资源推荐平台 - 停止服务
echo ============================================
echo   停止校园学习资源推荐平台
echo ============================================
echo.

echo 正在停止后端（8080端口）...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo 正在停止前端（3000端口）...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo   后端和前端已停止。MySQL 和 Neo4j 仍作为后台服务运行。
echo   如需完全停止，请在 PowerShell 执行：
echo     Stop-Service MySQL80
echo     D:\jaeger\neo4j\neo4j-community-2026.08.1\bin\neo4j.bat stop
echo.
pause

@echo off
setlocal EnableExtensions EnableDelayedExpansion
title 校园学习资源推荐平台 - 停止服务

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "NOPAUSE=0"
if /i "%~1"=="/nopause" set "NOPAUSE=1"

echo ============================================
echo   停止校园学习资源推荐平台
echo ============================================
echo.

rem ---------- 后端：只停命令行含 campus-recommend 的 java 进程 ----------
echo [后端]
call :port_busy 8080
if errorlevel 1 (
  echo   后端本来就没在运行
  goto :be_done
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*campus-recommend*' } | ForEach-Object { Write-Host ('   已停止后端  PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force }" 2>nul
call :wait_port_gone 8080 20
if errorlevel 1 (
  echo   没停掉。8080 上的进程不是本项目的 java，或者不是用 jar 启动的
  echo   这种情况脚本不硬杀，请自己确认后处理
) else (
  echo   后端已停止
)
:be_done

echo.
rem ---------- 前端：只杀本项目自己的 Vite ----------
echo [前端]
set "FE_FOUND=0"
for %%P in (3000 3001 3002 3003 3004) do (
  call :is_ours %%P
  if not errorlevel 1 (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr /c:":%%P " ^| findstr /i "LISTENING"') do (
      taskkill /F /T /PID %%a >nul 2>&1
      echo   已停止前端  PID %%a  端口 %%P
      set "FE_FOUND=1"
    )
  )
)
if "%FE_FOUND%"=="0" echo   本项目前端本来就没在运行

echo.
rem ---------- 明确告诉用户 3000 上可能有别人的东西 ----------
call :port_busy 3000
if not errorlevel 1 (
  call :is_ours 3000
  if errorlevel 1 echo   注意：3000 端口仍有别的程序占用，本脚本没有动它
)

echo ============================================
echo   MySQL 和 Neo4j 是系统服务，这次不停。
echo   要停 Neo4j：
echo     D:\jaeger\neo4j\neo4j-community-2026.08.1\bin\neo4j.bat stop
echo ============================================
echo.
if "%NOPAUSE%"=="0" pause
endlocal
exit /b 0

:port_busy
netstat -ano | findstr /c:":%~1 " | findstr /i "LISTENING" >nul 2>&1
if errorlevel 1 exit /b 1
exit /b 0

:is_ours
curl -s -m 3 "http://127.0.0.1:%~1/" 2>nul | findstr /c:"/@vite/client" >nul 2>&1
if errorlevel 1 exit /b 1
exit /b 0

:wait_port_gone
setlocal EnableDelayedExpansion
set /a _n=0
:wpg_loop
call :port_busy %~1
if errorlevel 1 ( endlocal & exit /b 0 )
set /a _n+=1
if !_n! GEQ %~2 ( endlocal & exit /b 1 )
ping -n 2 127.0.0.1 >nul
goto wpg_loop

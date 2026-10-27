@echo off
setlocal EnableExtensions EnableDelayedExpansion
title 校园学习资源推荐平台 - 一键启动

rem ============================================================
rem  用法：
rem    双击运行            正常启动
rem    一键启动.bat /diag  只体检，不启动任何东西
rem ============================================================

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "FE=%ROOT%\frontend"
set "BE=%ROOT%\backend"

set "NEO4J_HOME=D:\jaeger\neo4j\neo4j-community-2026.08.1"
set "NEO4J_JAVA=D:\jaeger\jdk-21"

set "MODE=start"
set "NOPAUSE=0"
if /i "%~1"=="/diag" set "MODE=diag"
if /i "%~1"=="/nopause" set "NOPAUSE=1"
if /i "%~2"=="/nopause" set "NOPAUSE=1"

set "FAIL=0"
set "S_DB=FAIL"
set "S_KG=FAIL"
set "S_BE=FAIL"
set "S_FE=FAIL"
set "FE_PORT="
set "FE_ACTION="

rem ---------- 先确认工具链 ----------
set "JAVA_EXE=C:\Program Files\Java\latest\jdk-26\bin\java.exe"
if exist "%JAVA_EXE%" goto :java_resolved
set "JAVA_EXE="
for /f "delims=" %%i in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%i"
:java_resolved

set "NPM_EXE="
for /f "delims=" %%i in ('where npm.cmd 2^>nul') do if not defined NPM_EXE set "NPM_EXE=%%i"
if not defined NPM_EXE for /f "delims=" %%i in ('where npm 2^>nul') do if not defined NPM_EXE set "NPM_EXE=%%i"

set "JAR="
if exist "%BE%\target\campus-recommend-1.0.0.jar" set "JAR=%BE%\target\campus-recommend-1.0.0.jar"
if not defined JAR if exist "D:\jaeger\campus-recommend-1.0.0.jar" set "JAR=D:\jaeger\campus-recommend-1.0.0.jar"

echo ============================================
echo   校园学习资源推荐平台  一键启动
if /i "%MODE%"=="diag" echo   模式：仅体检，不启动任何服务
echo ============================================
echo   项目目录：%ROOT%
echo   Java    ：%JAVA_EXE%
echo   npm     ：%NPM_EXE%
echo   后端 jar：%JAR%
echo ============================================
echo.

if not defined JAVA_EXE (
  echo [X] 找不到 java.exe，后端无法启动。
  echo     请安装 JDK 26，或把 java 加入系统 PATH。
  set "FAIL=1"
)
if not defined JAR (
  echo [X] 找不到后端 jar，请先在 backend 目录执行打包。
  set "FAIL=1"
)
if not defined NPM_EXE (
  echo [X] 找不到 npm，前端无法启动。
  echo     请安装 Node.js，或把 npm 加入系统 PATH。
  set "FAIL=1"
)
if "%FAIL%"=="1" (
  echo.
  echo 上面这些问题不解决，启动一定失败。
  goto :finish
)

rem ---------- 1/4 MySQL ----------
echo [1/4] MySQL
call :port_busy 3306
if not errorlevel 1 (
  echo         OK  MySQL 已在运行，端口 3306
  set "S_DB=OK"
  goto :mysql_done
)
if /i "%MODE%"=="diag" (
  echo         FAIL MySQL 未运行，3306 没有监听
  goto :mysql_done
)
echo         ...  MySQL 未运行，尝试启动 MySQL80 服务
net start MySQL80 >nul 2>&1
call :wait_port 3306 20
if errorlevel 1 (
  echo         FAIL 启动失败。请用管理员身份运行本脚本，或手动启动 MySQL 服务。
  set "FAIL=1"
) else (
  echo         OK  MySQL 已启动
  set "S_DB=OK"
)
:mysql_done
echo.

rem ---------- 2/4 Neo4j ----------
echo [2/4] Neo4j
call :port_busy 7687
if not errorlevel 1 (
  echo         OK  Neo4j 已在运行，端口 7687
  set "S_KG=OK"
  goto :neo4j_done
)
if /i "%MODE%"=="diag" (
  echo         FAIL Neo4j 未运行，7687 没有监听
  goto :neo4j_done
)
echo         ...  正在启动 Neo4j，通常需要 20 到 40 秒
rem 优先按 Windows 服务启动；服务不存在时退回官方脚本
net start neo4j >nul 2>&1
if errorlevel 1 (
  if exist "%NEO4J_HOME%\bin\neo4j.bat" (
    set "JAVA_HOME=%NEO4J_JAVA%"
    call "%NEO4J_HOME%\bin\neo4j.bat" start
  )
)
call :wait_port 7687 120
if errorlevel 1 (
  echo         FAIL Neo4j 启动超时，请查看 Neo4j 日志
  set "FAIL=1"
) else (
  echo         OK  Neo4j 已启动
  set "S_KG=OK"
)
:neo4j_done
echo.

rem ---------- 3/4 后端 ----------
echo [3/4] 后端
call :port_busy 8080
if not errorlevel 1 (
  call :is_our_backend 8080
  if errorlevel 1 (
    echo         WARN 8080 被别的程序占用，不是本项目后端
    echo              可能是之前没关掉的旧进程，建议先跑 停止服务.bat
    set "FAIL=1"
  ) else (
    echo         OK  后端已在运行，端口 8080
    set "S_BE=OK"
  )
  goto :backend_done
)
if /i "%MODE%"=="diag" (
  echo         FAIL 后端未运行，8080 没有监听
  goto :backend_done
)
if exist "%ROOT%\backend\local-env.cmd" (
    call "%ROOT%\backend\local-env.cmd"
) else (
    echo         WARN 缺少 backend\local-env.cmd，后端会因缺 MYSQL_PASSWORD 启动失败
    echo              请把 backend\local-env.example.cmd 复制为 local-env.cmd 并填值
)
echo         ...  正在启动后端，约需 15 到 25 秒
start "后端-8080" /min /d "%ROOT%" "%JAVA_EXE%" -jar "%JAR%" --server.port=8080
call :wait_backend 8080 90
if errorlevel 1 (
  echo         FAIL 后端启动失败，看那个最小化的“后端-8080”窗口里的报错
  set "FAIL=1"
) else (
  echo         OK  后端已启动
  set "S_BE=OK"
)
:backend_done
echo.

rem ---------- 4/4 前端 ----------
echo [4/4] 前端
rem 先说明 3000 的情况：被别的程序占着时，脚本换端口，但绝不会去动那个程序
call :port_busy 3000
if not errorlevel 1 (
  call :is_ours 3000
  if errorlevel 1 (
    echo         WARN 3000 被别的程序占用，而且不是本项目
    echo              下面会自动换一个端口启动，不会去动那个程序
  )
)

rem 在 3000 到 3004 之间找一个能用的端口：
rem   空闲        -> 用这个端口启动
rem   已是本项目   -> 直接复用，不重复启动
rem   被别人占着   -> 跳过，换下一个
for %%P in (3000 3001 3002 3003 3004) do (
  if not defined FE_PORT (
    call :port_busy %%P
    if errorlevel 1 (
      set "FE_PORT=%%P"
      set "FE_ACTION=start"
    ) else (
      call :is_ours %%P
      if not errorlevel 1 (
        set "FE_PORT=%%P"
        set "FE_ACTION=reuse"
      )
    )
  )
)

if not defined FE_PORT (
  echo         FAIL 3000 到 3004 都被别的程序占用，找不到可用端口
  echo              可以先运行 停止服务.bat，或手动关掉占端口的程序
  set "FAIL=1"
  goto :frontend_done
)

if /i "%FE_ACTION%"=="reuse" (
  echo         OK  本项目前端已在 %FE_PORT% 端口运行，直接使用
  set "S_FE=OK"
  goto :frontend_done
)

if /i "%MODE%"=="diag" (
  echo         FAIL 前端未运行，将使用端口 %FE_PORT% 启动
  goto :frontend_done
)

echo         ...  正在启动前端，端口 %FE_PORT%
start "前端-%FE_PORT%" /d "%FE%" cmd /k "%NPM_EXE% run dev -- --port %FE_PORT% --strictPort"
call :wait_ours %FE_PORT% 90
if errorlevel 1 (
  echo         FAIL 前端启动失败，看“前端-%FE_PORT%”窗口里的报错
  echo              常见原因：node_modules 缺失，先在 frontend 目录执行 npm install
  set "FAIL=1"
) else (
  echo         OK  前端已启动
  set "S_FE=OK"
)
:frontend_done
echo.

rem ---------- 汇总 ----------
echo ============================================
echo   检查结果
echo --------------------------------------------
call :status_line "MySQL"   %S_DB%
call :status_line "Neo4j"   %S_KG%
call :status_line "后端"    %S_BE%
call :status_line "前端"    %S_FE%
echo --------------------------------------------
if defined FE_PORT (
  echo   访问地址：http://localhost:%FE_PORT%
) else (
  echo   访问地址：前端未就绪
)
echo   学生账号：linhaoran / 123456
echo   管理员  ：admin / admin123
echo ============================================

if "%FAIL%"=="1" (
  echo.
  echo   有项目没起来，请看上面标着 FAIL 的那几行。
  echo   只想看体检结果、不启动任何东西：一键启动.bat /diag
) else (
  echo.
  echo   全部就绪。前端窗口请保持打开，本窗口可以关闭。
)

:finish
echo.
if "%NOPAUSE%"=="0" pause
endlocal
exit /b 0

rem ============================================================
rem  子过程
rem ============================================================

:port_busy
netstat -ano | findstr /c:":%~1 " | findstr /i "LISTENING" >nul 2>&1
if errorlevel 1 exit /b 1
exit /b 0

:is_ours
curl -s -m 3 "http://127.0.0.1:%~1/" 2>nul | findstr /c:"/@vite/client" >nul 2>&1
if errorlevel 1 exit /b 1
exit /b 0

:is_our_backend
curl -s -m 3 "http://127.0.0.1:%~1/api/auth/login" 2>nul | findstr /c:"code" >nul 2>&1
if errorlevel 1 exit /b 1
exit /b 0

:wait_port
setlocal EnableDelayedExpansion
set /a _n=0
:wp_loop
call :port_busy %~1
if not errorlevel 1 ( endlocal & exit /b 0 )
set /a _n+=1
if !_n! GEQ %~2 ( endlocal & exit /b 1 )
ping -n 2 127.0.0.1 >nul
goto wp_loop

:wait_ours
setlocal EnableDelayedExpansion
set /a _n=0
:wo_loop
call :is_ours %~1
if not errorlevel 1 ( endlocal & exit /b 0 )
set /a _n+=1
if !_n! GEQ %~2 ( endlocal & exit /b 1 )
ping -n 2 127.0.0.1 >nul
goto wo_loop

:wait_backend
setlocal EnableDelayedExpansion
set /a _n=0
:wb_loop
call :is_our_backend %~1
if not errorlevel 1 ( endlocal & exit /b 0 )
set /a _n+=1
if !_n! GEQ %~2 ( endlocal & exit /b 1 )
ping -n 2 127.0.0.1 >nul
goto wb_loop

:status_line
if /i "%~2"=="OK" (
  echo   [OK]   %~1
) else (
  echo   [FAIL] %~1
)
exit /b 0

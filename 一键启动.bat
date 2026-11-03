@echo off
setlocal EnableExtensions
cd /d "%~dp0"
set "NOPAUSE=0"
:scan_args
if "%~1"=="" goto run_launcher
if /i "%~1"=="/nopause" set "NOPAUSE=1"
shift
goto scan_args
:run_launcher
where python >nul 2>nul
if errorlevel 1 goto no_python
python "%~dp0.startup-safe\launch.py" %*
set "RESULT=%errorlevel%"
if "%NOPAUSE%"=="0" pause
exit /b %RESULT%
:no_python
echo [FAIL] Python 3.10 or newer is required. See docs/quickstart.md.
if "%NOPAUSE%"=="0" pause
exit /b 1

@echo off
rem 启动股票行情本地服务（推荐直接双击「股票服务.exe」）
rem 解释器 / 服务目录 / 端口 / 依赖清单 / lib 目录 / 缓存 TTL 都在启动器的「设置…」里配置并记忆
rem （%APPDATA%\DevLauncher\stock.ini）。本脚本优先委派给启动器，保证两条入口同一份配置；
rem 启动器 exe 不存在时，回落到原来的 .venv 直启方式。
cd /d %~dp0

if exist "股票服务.exe" (
    echo 已交给「股票服务.exe」启动，配置见 %APPDATA%\DevLauncher\stock.ini
    start "" "股票服务.exe"
    exit /b 0
)

if not exist ".venv\Scripts\python.exe" (
    echo 未找到 .venv\Scripts\python.exe
    echo 请先执行：python -m venv .venv
    echo          .venv\Scripts\python.exe -m pip install -r requirements.txt
    pause
    exit /b 1
)

.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 7300

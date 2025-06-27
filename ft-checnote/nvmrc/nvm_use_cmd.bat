@echo off

:: 命令行执行
:: echo "$(cat .nvmrc)"

:: 读取.nvmrc文件内容
set /p NVMRC_VERSION=<.nvmrc

:: 去除可能的"v"前缀（如果有）
set NVMRC_VERSION=%NVMRC_VERSION:v=%

:: 执行nvm use
nvm use %NVMRC_VERSION%
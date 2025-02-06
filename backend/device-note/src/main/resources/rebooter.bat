@echo off
set port=%~1
for /f "tokens=5" %%i in ('netstat -ano | findstr :%port% | findstr LISTENING') do (
    taskkill /F /PID %%i
)
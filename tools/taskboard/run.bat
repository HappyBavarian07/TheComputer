@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo   SimpleChatApp Developer Kanban Board
echo ==========================================

cd /d "%~dp0"

:: Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python is not installed or not in your PATH.
    echo Please install Python 3.10+ and try again.
    pause
    exit /b 1
)

:: Create virtual environment if it doesn't exist
if not exist .venv (
    echo [INFO] Creating Python virtual environment in .venv...
    python -m venv .venv
    if errorlevel 1 (
        echo [ERROR] Failed to create virtual environment.
        pause
        exit /b 1
    )
    
    echo [INFO] Activating virtual environment and installing dependencies...
    call .venv\Scripts\activate.bat
    python -m pip install --upgrade pip
    pip install -r requirements.txt
    if errorlevel 1 (
        echo [ERROR] Failed to install dependencies.
        pause
        exit /b 1
    )
) else (
    call .venv\Scripts\activate.bat
)

echo [INFO] Starting Developer Kanban Board...
python main.py
if errorlevel 1 (
    echo.
    echo [WARNING] Application exited with an error code.
)

echo Done.
pause

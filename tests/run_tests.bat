@echo off
setlocal enabledelayedexpansion

REM Set color codes for better output
set GREEN=92m
set YELLOW=93m
set RED=91m
set BLUE=94m
set RESET=0m

REM Default port configuration
set API_PORT=3000
set MAILHOG_PORT=8025

REM Parse command-line arguments
:parse_args
if "%~1"=="" goto end_parse_args
if "%~1"=="--port" (
    set API_PORT=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="--mailhog-port" (
    set MAILHOG_PORT=%~2
    shift 
    shift
    goto parse_args
)
shift
goto parse_args
:end_parse_args

echo [%BLUE%INFO[%RESET%] Flashcard API Test Runner
echo [%BLUE%INFO[%RESET%] API port: %API_PORT%
echo [%BLUE%INFO[%RESET%] MailHog port: %MAILHOG_PORT%

REM Check if venv directory exists
if exist "venv\" (
    echo [%GREEN%SUCCESS[%RESET%] Virtual environment found.
) else (
    echo [%YELLOW%WARNING[%RESET%] Virtual environment not found. Creating one...
    python -m venv venv
    if !errorlevel! neq 0 (
        echo [%RED%ERROR[%RESET%] Failed to create virtual environment. Make sure Python is installed.
        goto :error
    )
    echo [%GREEN%SUCCESS[%RESET%] Virtual environment created.
)

REM Activate virtual environment
echo [%BLUE%INFO[%RESET%] Activating virtual environment...
call venv\Scripts\activate.bat
if %errorlevel% neq 0 (
    echo [%RED%ERROR[%RESET%] Failed to activate virtual environment.
    goto :error
)

REM Install dependencies if requirements.txt exists
if exist requirements.txt (
    echo [%BLUE%INFO[%RESET%] Installing dependencies from requirements.txt...
    pip install -r requirements.txt
    if !errorlevel! neq 0 (
        echo [%RED%ERROR[%RESET%] Failed to install dependencies.
        goto :error
    )
    echo [%GREEN%SUCCESS[%RESET%] Dependencies installed.
) else (
    echo [%BLUE%INFO[%RESET%] Installing required dependencies...
    pip install requests
    if !errorlevel! neq 0 (
        echo [%RED%ERROR[%RESET%] Failed to install dependencies.
        goto :error
    )
    echo [%GREEN%SUCCESS[%RESET%] Dependencies installed.
)

REM Run the test script with port parameters
echo [%BLUE%INFO[%RESET%] Running test_flashcard_api.py...
python test_flashcard_api.py --port %API_PORT% --mailhog-port %MAILHOG_PORT%
if %errorlevel% neq 0 (
    echo [%RED%ERROR[%RESET%] Test script execution failed.
    goto :error
)

:end
echo [%GREEN%SUCCESS[%RESET%] Script execution completed.
call venv\Scripts\deactivate.bat
pause
exit /b 0

:error
echo [%RED%ERROR[%RESET%] Script execution failed.
pause
exit /b 1
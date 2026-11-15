@echo off
cd /d "%~dp0"
if exist "build\wall-crack-detector.jar" (
    start "" javaw -jar "build\wall-crack-detector.jar"
) else if exist "CSC360-Group-01\build\wall-crack-detector.jar" (
    cd CSC360-Group-01
    start "" javaw -jar "build\wall-crack-detector.jar"
) else (
    echo [ERROR] wall-crack-detector.jar not found. Please build the project first.
    pause
)

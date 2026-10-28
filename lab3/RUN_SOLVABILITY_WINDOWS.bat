@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist "bin" mkdir "bin"
(for /r src %%F in (*.java) do @echo "%%~fF") > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
if errorlevel 1 (pause & exit /b 1)
java -Dfile.encoding=UTF-8 -cp bin app.Main --solvability
pause

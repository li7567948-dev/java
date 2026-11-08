@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist "bin" mkdir "bin"
if exist "sources.list" del "sources.list"
for /R "src" %%F in (*.java) do @echo "%%~fF">>sources.list
javac -encoding UTF-8 -source 8 -target 8 -d bin @sources.list
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp bin tests.Lab3Tests
pause

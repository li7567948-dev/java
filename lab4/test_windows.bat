@echo off
cd /d "%~dp0"
chcp 65001 >nul
if not exist bin mkdir bin
(for /r src %%f in (*.java) do @echo "%%f") > sources.txt
javac -encoding UTF-8 -Xlint:all -d bin @sources.txt
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp bin tests.AutoTests

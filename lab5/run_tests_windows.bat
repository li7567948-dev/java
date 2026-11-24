@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src\lab5\model\Measurement.java src\lab5\model\ViewSettings.java src\lab5\model\DataFile.java src\lab5\io\ErrorHandler.java src\lab5\io\MeasurementFileService.java src\lab5\view\GraphPanel.java src\lab5\view\MainFrame.java src\lab5\Main.java src\lab5\tests\Lab5Tests.java src\lab5\tests\GuiSmokeTest.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
java -ea -Dfile.encoding=UTF-8 -cp bin lab5.tests.Lab5Tests
pause
endlocal

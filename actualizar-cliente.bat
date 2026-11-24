@echo off
rem Vuelve a generar el .jar del juego. Usalo despues de cambiar el codigo.
cd /d "%~dp0"
call gradlew.bat lwjgl3:jar
echo.
if errorlevel 1 (echo Hubo un error.) else (echo Listo: lwjgl3\build\libs\OverZone-1.0.0.jar)
pause

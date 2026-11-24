@echo off
rem Abre el juego OverZone con doble click.
rem Si el .jar no existe todavia, lo genera primero.
title OverZone
cd /d "%~dp0"

set JAR=OverZone-1.0.0.jar
if exist "%JAR%" goto abrir
set JAR=lwjgl3\build\libs\OverZone-1.0.0.jar
if exist "%JAR%" goto abrir

echo Generando el juego por primera vez (puede tardar unos minutos)...
call gradlew.bat lwjgl3:jar
if errorlevel 1 goto error

:abrir
java -jar "%JAR%"
if errorlevel 1 goto error
exit /b 0

:error
echo.
echo Ocurrio un error. Revisa el mensaje de arriba.
pause

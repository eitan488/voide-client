@echo off
REM Void Client - Windows build script
REM Requires Java 21 (JDK). The finished JAR is written to build\libs\
call gradlew.bat build
pause

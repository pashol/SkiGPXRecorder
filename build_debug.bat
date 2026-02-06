@echo off
setlocal

:: Change to project directory
cd /d "%~dp0"

:: Set Java to Android Studio JDK
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Verify Java version
java -version

:: Run gradle build
call "%~dp0gradlew.bat" :app:assembleDebug

if %errorlevel% neq 0 (
    echo Build failed with error code %errorlevel%
    exit /b %errorlevel%
)

echo.
echo Build successful!
echo APK location: app\build\outputs\apk\debug\app-debug.apk
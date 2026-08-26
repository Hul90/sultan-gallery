@echo off
setlocal
set "APP_HOME=%~dp0"
set "PROP_FILE=%APP_HOME%gradle\wrapper\gradle-wrapper.properties"

for /f "usebackq tokens=1,* delims==" %%A in ("%PROP_FILE%") do if "%%A"=="distributionUrl" set "DIST_URL=%%B"
set "DIST_URL=%DIST_URL:\:=:%"
for %%F in ("%DIST_URL%") do set "DIST_NAME=%%~nxF"
set "DIST_VERSION=%DIST_NAME:gradle-=%"
set "DIST_VERSION=%DIST_VERSION:-bin.zip=%"
set "GRADLE_USER_HOME=%GRADLE_USER_HOME%"
if "%GRADLE_USER_HOME%"=="" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "INSTALL_DIR=%GRADLE_USER_HOME%\wrapper\dists\sultan-gallery-gradle\%DIST_VERSION%"
set "GRADLE_BIN=%INSTALL_DIR%\gradle-%DIST_VERSION%\bin\gradle.bat"
set "ZIP_FILE=%INSTALL_DIR%\%DIST_NAME%"

if not exist "%GRADLE_BIN%" (
  if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
  if not exist "%ZIP_FILE%" (
    echo Downloading Gradle %DIST_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile '%ZIP_FILE%'"
    if errorlevel 1 exit /b 1
  )
  if exist "%INSTALL_DIR%\.extracting" rmdir /s /q "%INSTALL_DIR%\.extracting"
  mkdir "%INSTALL_DIR%\.extracting"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP_FILE%' -DestinationPath '%INSTALL_DIR%\.extracting' -Force"
  if errorlevel 1 exit /b 1
  if exist "%INSTALL_DIR%\gradle-%DIST_VERSION%" rmdir /s /q "%INSTALL_DIR%\gradle-%DIST_VERSION%"
  move "%INSTALL_DIR%\.extracting\gradle-%DIST_VERSION%" "%INSTALL_DIR%\gradle-%DIST_VERSION%" >nul
  rmdir /s /q "%INSTALL_DIR%\.extracting"
)

call "%GRADLE_BIN%" %*
exit /b %ERRORLEVEL%

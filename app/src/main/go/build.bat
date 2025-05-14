@echo off
setlocal enabledelayedexpansion

REM Default values
set ANDROID_API=24
set OUTPUT_DIR=%1

REM Check if OUTPUT_DIR is set
if "%OUTPUT_DIR%"=="" (
  echo Error: Output directory not specified.
  echo Usage: %0 ^<output_directory^>
  exit /b 1
)

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Change to the Go package directory
cd /d %~dp0

REM Check if SRT libraries have been built for all the required architectures
for %%A in (arm64-v8a armeabi-v7a x86 x86_64) do (
  if not exist "third_party\srt\scripts\build-android\%%A\lib\libsrt.so" (
    echo Warning: SRT library not found for %%A. The build might fail.
    echo Make sure to run the native build with CMake first.
  )
)

REM Ensure we have gomobile
where gomobile >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo gomobile not found, installing...
  go install golang.org/x/mobile/cmd/gomobile@latest
  gomobile init
) else (
  gomobile version >nul 2>&1
  if %ERRORLEVEL% neq 0 (
    echo Initializing gomobile...
    gomobile init
  )
)

REM Build the AAR
echo Building kinetic AAR for Android API %ANDROID_API%...
set GO111MODULE=on
gomobile bind -target=android -androidapi=%ANDROID_API% -o "%OUTPUT_DIR%\kinetic.aar" .

echo Build complete. AAR file at: %OUTPUT_DIR%\kinetic.aar
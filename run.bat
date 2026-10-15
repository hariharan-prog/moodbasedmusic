@echo off
setlocal
where javac >nul 2>nul
if errorlevel 1 (
  echo Java JDK 17 or newer is required. Install a JDK and add it to PATH.
  pause
  exit /b 1
)
javac MoodRecommenderServer2.java
if errorlevel 1 (
  echo Compilation failed. Check the errors above.
  pause
  exit /b 1
)
java MoodRecommenderServer2
pause

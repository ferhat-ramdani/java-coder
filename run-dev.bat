@echo off
echo Building the project...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Build failed!
    exit /b %errorlevel%
)
echo Starting the application...
java -jar target\java_coder.jar

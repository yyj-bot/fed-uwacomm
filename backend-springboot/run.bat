@echo off
echo ========================================
echo 启动水声联邦学习后端服务
echo ========================================

echo 检查Java版本...
java -version
if %errorlevel% neq 0 (
    echo 错误：未找到Java，请确保已安装JDK 17+
    pause
    exit /b 1
)

echo 检查Maven版本...
mvn -version
if %errorlevel% neq 0 (
    echo 错误：未找到Maven，请确保已安装Maven 3.6+
    pause
    exit /b 1
)

echo 清理并编译项目...
mvn clean compile

echo 启动Spring Boot应用...
mvn -pl feduwacomm-server spring-boot:run

pause 
/clear@echo off
chcp 65001 >nul
echo =====================================================
echo 数据库脚本同步工具
echo =====================================================
echo.

set "SOURCE_SCRIPT=docs\shared\database\mysql\init\init_mysql.sql"
set "TARGET_SCRIPT=backend-springboot\feduwacomm-server\src\main\resources\db\init_mysql.sql"

if not exist "%SOURCE_SCRIPT%" (
    echo ❌ 源脚本文件不存在: %SOURCE_SCRIPT%
    pause
    exit /b 1
)

echo 📂 源脚本: %SOURCE_SCRIPT%
echo 📂 目标脚本: %TARGET_SCRIPT%
echo.

if exist "%TARGET_SCRIPT%" (
    echo ℹ️  目标脚本已存在，将被覆盖
)

echo 🔄 正在同步数据库脚本...
copy "%SOURCE_SCRIPT%" "%TARGET_SCRIPT%" >nul

if %errorlevel% equ 0 (
    echo ✅ 数据库脚本同步成功！
    echo.
    echo 💡 现在你只需要维护一个脚本文件:
    echo    %SOURCE_SCRIPT%
    echo.
    echo 🚀 Spring Boot启动时将自动使用此脚本进行数据库初始化
) else (
    echo ❌ 数据库脚本同步失败！
)

echo.
pause
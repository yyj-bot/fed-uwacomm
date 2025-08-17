#!/usr/bin/env python3
"""
简单的环境测试脚本
"""

import os
import sys
from pathlib import Path

print("=== 环境配置测试 ===")

# 测试Python版本
print(f"Python版本: {sys.version}")

# 测试核心库
try:
    import numpy as np
    print(f"✅ NumPy: {np.__version__}")
except ImportError:
    print("❌ NumPy: 未安装")

try:
    import pandas as pd
    print(f"✅ Pandas: {pd.__version__}")
except ImportError:
    print("❌ Pandas: 未安装")

try:
    import sklearn
    print(f"✅ Scikit-learn: {sklearn.__version__}")
except ImportError:
    print("❌ Scikit-learn: 未安装")

try:
    import pymysql
    print(f"✅ PyMySQL: {pymysql.__version__}")
except ImportError:
    print("❌ PyMySQL: 未安装")

try:
    from dotenv import load_dotenv
    print("✅ python-dotenv: 已安装")
    
    # 手动加载.env文件
    env_path = Path('.env')
    if env_path.exists():
        print(f"📁 找到.env文件: {env_path.absolute()}")
        load_dotenv(env_path)
        print("✅ .env文件已加载")
    else:
        print("❌ .env文件不存在")
        
except ImportError:
    print("❌ python-dotenv: 未安装")

# 测试环境变量
print("\n=== 环境变量测试 ===")
print(f"DB_HOST: {os.getenv('DB_HOST', '未设置')}")
print(f"DB_PORT: {os.getenv('DB_PORT', '未设置')}")
print(f"DB_USER: {os.getenv('DB_USER', '未设置')}")
print(f"DB_PASSWORD: {'已设置' if os.getenv('DB_PASSWORD') else '未设置'}")
print(f"DB_NAME: {os.getenv('DB_NAME', '未设置')}")

# 测试数据库连接
print("\n=== 数据库连接测试 ===")
try:
    import pymysql
    
    # 从环境变量获取配置
    host = os.getenv('DB_HOST', 'localhost')
    port = int(os.getenv('DB_PORT', 3306))
    user = os.getenv('DB_USER', 'root')
    password = os.getenv('DB_PASSWORD', '')
    database = os.getenv('DB_NAME', 'bellhop_data')
    
    print(f"尝试连接到: {host}:{port}")
    print(f"数据库: {database}")
    print(f"用户: {user}")
    
    # 尝试连接
    connection = pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
        charset='utf8mb4'
    )
    
    print("✅ 数据库连接成功!")
    
    # 测试查询
    with connection.cursor() as cursor:
        cursor.execute("SELECT VERSION()")
        version = cursor.fetchone()
        print(f"MySQL版本: {version[0]}")
    
    connection.close()
    
except Exception as e:
    print(f"❌ 数据库连接失败: {e}")

print("\n=== 测试完成 ===")

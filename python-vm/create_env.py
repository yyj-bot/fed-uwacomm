#!/usr/bin/env python3
"""
创建.env文件的脚本
"""

env_content = """DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=119921
DB_NAME=bellhop_data"""

# 写入.env文件
with open('.env', 'w', encoding='utf-8') as f:
    f.write(env_content)

print("✅ .env文件已创建")
print("文件内容:")
print(env_content)


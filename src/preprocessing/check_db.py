#!/usr/bin/env python3
"""
检查数据库结构和数据的脚本
"""

import os
import pymysql
import dotenv
from pathlib import Path

# 加载.env文件
dotenv_path = Path(os.path.dirname(os.path.dirname(os.path.dirname(__file__)))) / '.env'
dotenv.load_dotenv(dotenv_path=dotenv_path)

# MySQL连接参数
MYSQL_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', '123456Lrn.'),
    'database': os.getenv('DB_NAME', 'bellhop_data'),
    'table': os.getenv('DB_TABLE', 'features')
}

def check_database_structure():
    """检查数据库结构和数据"""
    try:
        # 连接数据库
        conn = pymysql.connect(
            host=MYSQL_CONFIG['host'],
            port=MYSQL_CONFIG['port'],
            user=MYSQL_CONFIG['user'],
            password=MYSQL_CONFIG['password'],
            database=MYSQL_CONFIG['database'],
            charset='utf8'
        )
        cursor = conn.cursor()
        
        # 查询表结构
        print("===== 表结构 =====")
        cursor.execute(f"SHOW COLUMNS FROM {MYSQL_CONFIG['table']}")
        columns = cursor.fetchall()
        for col in columns:
            print(f"字段: {col[0]}, 类型: {col[1]}, 可为空: {col[2]}, 键: {col[3]}, 默认值: {col[4]}")
        
        # 查询数据示例
        print("\n===== 数据示例 =====")
        cursor.execute(f"SELECT * FROM {MYSQL_CONFIG['table']} LIMIT 1")
        rows = cursor.fetchall()
        
        # 获取列名
        cursor.execute(f"SHOW COLUMNS FROM {MYSQL_CONFIG['table']}")
        column_names = [column[0] for column in cursor.fetchall()]
        
        # 打印数据
        for row in rows:
            print("\n数据行:")
            for i, value in enumerate(row):
                print(f"{column_names[i]}: {value}")
        
        # 统计数据
        print("\n===== 数据统计 =====")
        cursor.execute(f"SELECT COUNT(*) FROM {MYSQL_CONFIG['table']}")
        count_result = cursor.fetchone()
        count = count_result[0] if count_result else 0
        print(f"总记录数: {count}")
        
        # 检查是否包含传播损失和射线路径特征
        has_tl = False
        has_ray = False
        for col in columns:
            if 'tl_' in col[0] or 'shd_' in col[0]:
                has_tl = True
            if 'ray_' in col[0]:
                has_ray = True
        
        print(f"\n包含传播损失特征: {'是' if has_tl else '否'}")
        print(f"包含射线路径特征: {'是' if has_ray else '否'}")
        
        # 关闭连接
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"检查数据库时出错: {str(e)}")

if __name__ == '__main__':
    check_database_structure() 
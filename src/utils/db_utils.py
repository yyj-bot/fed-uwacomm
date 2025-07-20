"""
数据库工具模块，提供数据库连接和操作功能
"""

import os
import pymysql
from dotenv import load_dotenv
from pathlib import Path

# 加载环境变量
dotenv_path = Path(os.path.dirname(os.path.dirname(os.path.dirname(__file__)))) / '.env'
load_dotenv(dotenv_path=dotenv_path)

class DatabaseManager:
    """数据库管理器，提供数据库连接和操作功能"""
    
    @staticmethod
    def get_connection():
        """获取数据库连接"""
        try:
            conn = pymysql.connect(
                host=os.getenv('DB_HOST', 'localhost'),
                port=int(os.getenv('DB_PORT', 3306)),
                user=os.getenv('DB_USER', 'root'),
                password=os.getenv('DB_PASSWORD', '123456Lrn.'),
                database=os.getenv('DB_NAME', 'bellhop_data'),
                charset='utf8'
            )
            return conn
        except Exception as e:
            print(f"数据库连接错误: {str(e)}")
            return None
    
    @staticmethod
    def execute_query(query, params=None, fetch=True):
        """执行SQL查询"""
        conn = None
        cursor = None
        result = None
        
        try:
            conn = DatabaseManager.get_connection()
            if conn:
                cursor = conn.cursor()
                cursor.execute(query, params)
                
                if fetch:
                    result = cursor.fetchall()
                else:
                    conn.commit()
                    result = cursor.rowcount
        except Exception as e:
            print(f"执行SQL错误: {str(e)}")
            if conn:
                conn.rollback()
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
        
        return result
    
    @staticmethod
    def create_table(table_name, fields):
        """创建数据表"""
        # 构建字段定义
        field_defs = []
        for name, type_def in fields.items():
            field_defs.append(f"{name} {type_def}")
        
        # 构建SQL
        sql = f"""
        CREATE TABLE IF NOT EXISTS {table_name} (
            {', '.join(field_defs)}
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
        """
        
        # 执行SQL
        return DatabaseManager.execute_query(sql, fetch=False)
    
    @staticmethod
    def insert_data(table_name, data):
        """插入数据"""
        # 构建SQL
        fields = list(data.keys())
        placeholders = ', '.join(['%s'] * len(fields))
        values = [data[field] for field in fields]
        
        sql = f"""
        INSERT INTO {table_name} ({', '.join(fields)})
        VALUES ({placeholders})
        """
        
        # 执行SQL
        return DatabaseManager.execute_query(sql, values, fetch=False)
    
    @staticmethod
    def batch_insert(table_name, data_list):
        """批量插入数据"""
        if not data_list:
            return 0
        
        # 获取所有字段
        all_fields = set()
        for data in data_list:
            all_fields.update(data.keys())
        
        fields = list(all_fields)
        placeholders = ', '.join(['%s'] * len(fields))
        
        # 构建SQL
        sql = f"""
        INSERT INTO {table_name} ({', '.join(fields)})
        VALUES ({placeholders})
        """
        
        # 执行批量插入
        conn = None
        cursor = None
        rows_affected = 0
        
        try:
            conn = DatabaseManager.get_connection()
            if conn:
                cursor = conn.cursor()
                
                for data in data_list:
                    values = [data.get(field, None) for field in fields]
                    cursor.execute(sql, values)
                    rows_affected += cursor.rowcount
                
                conn.commit()
        except Exception as e:
            print(f"批量插入数据错误: {str(e)}")
            if conn:
                conn.rollback()
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
        
        return rows_affected 
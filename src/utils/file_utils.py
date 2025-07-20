"""
文件工具模块，提供文件操作功能
"""

import os
import shutil
import glob
import json
import csv
from pathlib import Path

class FileUtils:
    """文件工具类，提供文件操作功能"""
    
    @staticmethod
    def ensure_dir(directory):
        """确保目录存在，如果不存在则创建"""
        if not os.path.exists(directory):
            os.makedirs(directory)
        return directory
    
    @staticmethod
    def list_files(directory, pattern="*.*"):
        """列出目录中匹配模式的所有文件"""
        return glob.glob(os.path.join(directory, pattern))
    
    @staticmethod
    def copy_file(src, dst, overwrite=True):
        """复制文件"""
        if os.path.exists(dst) and not overwrite:
            return False
        
        try:
            shutil.copy2(src, dst)
            return True
        except Exception as e:
            print(f"复制文件错误: {str(e)}")
            return False
    
    @staticmethod
    def move_file(src, dst, overwrite=True):
        """移动文件"""
        if os.path.exists(dst) and not overwrite:
            return False
        
        try:
            shutil.move(src, dst)
            return True
        except Exception as e:
            print(f"移动文件错误: {str(e)}")
            return False
    
    @staticmethod
    def delete_file(path):
        """删除文件"""
        try:
            if os.path.exists(path):
                os.remove(path)
                return True
            return False
        except Exception as e:
            print(f"删除文件错误: {str(e)}")
            return False
    
    @staticmethod
    def read_text(file_path, encoding='utf-8'):
        """读取文本文件"""
        try:
            with open(file_path, 'r', encoding=encoding, errors='ignore') as f:
                return f.read()
        except Exception as e:
            print(f"读取文件错误: {str(e)}")
            return None
    
    @staticmethod
    def write_text(file_path, content, encoding='utf-8'):
        """写入文本文件"""
        try:
            with open(file_path, 'w', encoding=encoding) as f:
                f.write(content)
            return True
        except Exception as e:
            print(f"写入文件错误: {str(e)}")
            return False
    
    @staticmethod
    def read_json(file_path, encoding='utf-8'):
        """读取JSON文件"""
        try:
            with open(file_path, 'r', encoding=encoding) as f:
                return json.load(f)
        except Exception as e:
            print(f"读取JSON文件错误: {str(e)}")
            return None
    
    @staticmethod
    def write_json(file_path, data, encoding='utf-8', indent=4):
        """写入JSON文件"""
        try:
            with open(file_path, 'w', encoding=encoding) as f:
                json.dump(data, f, ensure_ascii=False, indent=indent)
            return True
        except Exception as e:
            print(f"写入JSON文件错误: {str(e)}")
            return False
    
    @staticmethod
    def read_csv(file_path, delimiter=',', has_header=True, encoding='utf-8'):
        """读取CSV文件"""
        try:
            data = []
            with open(file_path, 'r', encoding=encoding, newline='') as f:
                reader = csv.reader(f, delimiter=delimiter)
                if has_header:
                    header = next(reader)
                    data.append(header)
                for row in reader:
                    data.append(row)
            return data
        except Exception as e:
            print(f"读取CSV文件错误: {str(e)}")
            return None
    
    @staticmethod
    def write_csv(file_path, data, delimiter=',', encoding='utf-8'):
        """写入CSV文件"""
        try:
            with open(file_path, 'w', encoding=encoding, newline='') as f:
                writer = csv.writer(f, delimiter=delimiter)
                for row in data:
                    writer.writerow(row)
            return True
        except Exception as e:
            print(f"写入CSV文件错误: {str(e)}")
            return False 
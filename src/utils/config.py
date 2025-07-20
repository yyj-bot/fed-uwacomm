"""
配置工具模块，提供配置加载和管理功能
"""

import os
import json
from pathlib import Path
from dotenv import load_dotenv

# 项目根目录
ROOT_DIR = Path(os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

# 数据目录
DATA_DIR = os.path.join(ROOT_DIR, 'data')
BELLHOP_DIR = os.path.join(DATA_DIR, 'bellhop')

# 环境文件模板
ENV_TEMPLATE = """'Realistic Ocean Environment B{}'
{:.1f}
1
'C'
51  0.0  {:.1f}
{}
'L'
{:.1f} {:.2f} 0.0 {:.1f} /
1
{:.1f} /
1
{:.1f} /
101
0.0 {:.1f} /
'A'
101
-20.0 20.0 /
0.0 {:.1f} 101.0
"""

def load_config(env_path=None):
    """加载配置"""
    if env_path is None:
        env_path = os.path.join(ROOT_DIR, '.env')
    
    # 加载环境变量
    load_dotenv(dotenv_path=env_path)
    
    # 返回配置字典
    return {
        'db': {
            'host': os.getenv('DB_HOST', 'localhost'),
            'port': int(os.getenv('DB_PORT', 3306)),
            'user': os.getenv('DB_USER', 'root'),
            'password': os.getenv('DB_PASSWORD', '123456Lrn.'),
            'database': os.getenv('DB_NAME', 'bellhop_data'),
            'table': os.getenv('DB_TABLE', 'features')
        },
        'paths': {
            'root': str(ROOT_DIR),
            'data': DATA_DIR,
            'bellhop': BELLHOP_DIR
        }
    }

class Config:
    """配置类，提供配置管理功能"""
    
    _instance = None
    _config = None
    
    def __new__(cls):
        """单例模式"""
        if cls._instance is None:
            cls._instance = super(Config, cls).__new__(cls)
            cls._config = load_config()
        return cls._instance
    
    @classmethod
    def get(cls, key, default=None):
        """获取配置项"""
        if cls._config is None:
            cls._config = load_config()
        
        # 支持点号分隔的多级键
        if '.' in key:
            parts = key.split('.')
            value = cls._config
            for part in parts:
                if isinstance(value, dict) and part in value:
                    value = value[part]
                else:
                    return default
            return value
        
        return cls._config.get(key, default)
    
    @classmethod
    def set(cls, key, value):
        """设置配置项"""
        if cls._config is None:
            cls._config = load_config()
        
        # 支持点号分隔的多级键
        if '.' in key:
            parts = key.split('.')
            config = cls._config
            for i, part in enumerate(parts[:-1]):
                if part not in config:
                    config[part] = {}
                config = config[part]
            config[parts[-1]] = value
        else:
            cls._config[key] = value
    
    @classmethod
    def save(cls, config_path=None):
        """保存配置到文件"""
        if config_path is None:
            config_path = os.path.join(ROOT_DIR, 'config.json')
        
        try:
            with open(config_path, 'w', encoding='utf-8') as f:
                json.dump(cls._config, f, ensure_ascii=False, indent=4)
            return True
        except Exception as e:
            print(f"保存配置错误: {str(e)}")
            return False
    
    @classmethod
    def load(cls, config_path=None):
        """从文件加载配置"""
        if config_path is None:
            config_path = os.path.join(ROOT_DIR, 'config.json')
        
        try:
            if os.path.exists(config_path):
                with open(config_path, 'r', encoding='utf-8') as f:
                    cls._config = json.load(f)
                return True
            else:
                cls._config = load_config()
                return False
        except Exception as e:
            print(f"加载配置错误: {str(e)}")
            cls._config = load_config()
            return False 
"""
项目配置设置

包含数据库配置、模型参数等设置。
"""

import os
from pathlib import Path

# 项目根目录
PROJECT_ROOT = Path(__file__).parent.parent

# 数据库配置
DATABASE_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'database': os.getenv('DB_NAME', 'bellhop_data'),
    'charset': 'utf8mb4'
}

# 数据路径
DATA_DIR = PROJECT_ROOT / 'data'
BELLHOP_DATA_DIR = DATA_DIR / 'bellhop'
FEATURES_FILE = PROJECT_ROOT / 'bellhop_features_final.csv'

# 模型配置
MODEL_CONFIG = {
    'random_forest': {
        'n_estimators': 100,
        'random_state': 42,
        'max_depth': None,
        'min_samples_split': 2,
        'min_samples_leaf': 1
    }
}

# 结果输出路径
RESULTS_DIR = PROJECT_ROOT / 'results'
MODELS_DIR = PROJECT_ROOT / 'models'

# 日志配置
LOGGING_CONFIG = {
    'level': 'INFO',
    'format': '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    'file': PROJECT_ROOT / 'results' / 'logs' / 'feduwacomm.log'
} 
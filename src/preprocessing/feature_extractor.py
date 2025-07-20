import os
import re
import csv
import pymysql
import numpy as np
import dotenv
from pathlib import Path

# 加载.env文件
dotenv_path = Path(os.path.dirname(os.path.dirname(os.path.dirname(__file__)))) / '.env'
dotenv.load_dotenv(dotenv_path=dotenv_path)

# MySQL连接参数（从环境变量加载）
MYSQL_HOST = os.getenv('DB_HOST', 'localhost')
MYSQL_PORT = int(os.getenv('DB_PORT', 3306))
MYSQL_USER = os.getenv('DB_USER', 'root')
MYSQL_PASS = os.getenv('DB_PASSWORD', '123456Lrn.')
MYSQL_DB = os.getenv('DB_NAME', 'bellhop_data')
MYSQL_TABLE = os.getenv('DB_TABLE', 'features')

# 目标目录
DATA_DIR = os.getenv('DATA_DIR', 'data/bellhop')

# 提取单个.prt文件特征
def extract_prt_features(prt_path):
    features = {
        'filename': os.path.basename(prt_path),
        'success': 1,
        'error_msg': '',
        'freq': None,
        'ssp_points': 0,
        'ssp_min': None,
        'ssp_max': None,
        'ssp_mean': None,
        'depth_min': None,
        'depth_max': None
    }
    ssp_depths = []
    ssp_speeds = []
    with open(prt_path, 'r', encoding='utf-8', errors='ignore') as f:
        lines = f.readlines()
    for line in lines:
        if 'frequency' in line:
            m = re.search(r'([0-9.]+)', line)
            if m:
                features['freq'] = float(m.group(1))
        if re.match(r'\s*\d+\.\d+\s+\d+\.\d+', line):
            parts = line.strip().split()
            if len(parts) >= 2:
                try:
                    d, c = float(parts[0]), float(parts[1])
                    ssp_depths.append(d)
                    ssp_speeds.append(c)
                except Exception:
                    continue
        if 'FATAL ERROR' in line:
            features['success'] = 0
            features['error_msg'] = line.strip()
        if 'Bad depth in SSP' in line:
            features['success'] = 0
            features['error_msg'] = 'Bad depth in SSP'
    if ssp_speeds:
        features['ssp_points'] = len(ssp_speeds)
        features['ssp_min'] = float(np.min(ssp_speeds))
        features['ssp_max'] = float(np.max(ssp_speeds))
        features['ssp_mean'] = float(np.mean(ssp_speeds))
    if ssp_depths:
        features['depth_min'] = float(np.min(ssp_depths))
        features['depth_max'] = float(np.max(ssp_depths))
    return features

# 预留：提取.shd/.arr等其它文件特征接口
def extract_shd_features(shd_path):
    # TODO: 可根据实际需求扩展
    return {}

def extract_arr_features(arr_path):
    # TODO: 可根据实际需求扩展
    return {}

# 批量处理所有仿真输出文件
def batch_extract_features():
    all_features = []
    for fname in os.listdir(DATA_DIR):
        if fname.endswith('.prt'):
            fpath = os.path.join(DATA_DIR, fname)
            feat = extract_prt_features(fpath)
            all_features.append(feat)
        # 预留：.shd/.arr等
        # elif fname.endswith('.shd'):
        #     ...
        # elif fname.endswith('.arr'):
        #     ...
    return all_features

# 保存为CSV
def save_to_csv(features, csv_path):
    if not features:
        print('无特征数据可保存')
        return
    keys = list(features[0].keys())
    with open(csv_path, 'w', newline='', encoding='utf-8') as fout:
        writer = csv.DictWriter(fout, fieldnames=keys)
        writer.writeheader()
        writer.writerows(features)
    print(f'已保存特征到 {csv_path}')

# 导入MySQL数据库
def import_to_mysql(features):
    conn = pymysql.connect(host=MYSQL_HOST, port=MYSQL_PORT, user=MYSQL_USER, password=MYSQL_PASS, database=MYSQL_DB, charset='utf8')
    cursor = conn.cursor()
    # 自动建表
    create_sql = f'''
    CREATE TABLE IF NOT EXISTS {MYSQL_TABLE} (
        id INT AUTO_INCREMENT PRIMARY KEY,
        filename VARCHAR(128),
        success TINYINT,
        error_msg VARCHAR(256),
        freq FLOAT,
        ssp_points INT,
        ssp_min FLOAT,
        ssp_max FLOAT,
        ssp_mean FLOAT,
        depth_min FLOAT,
        depth_max FLOAT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;'''
    cursor.execute(create_sql)
    # 新增：清空表，保证每次导入都是最新数据
    cursor.execute(f"TRUNCATE TABLE {MYSQL_TABLE}")
    # 插入数据
    insert_sql = f'''
    INSERT INTO {MYSQL_TABLE} (filename, success, error_msg, freq, ssp_points, ssp_min, ssp_max, ssp_mean, depth_min, depth_max)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    '''
    for feat in features:
        cursor.execute(insert_sql, (
            feat['filename'], feat['success'], feat['error_msg'], feat['freq'],
            feat['ssp_points'], feat['ssp_min'], feat['ssp_max'], feat['ssp_mean'],
            feat['depth_min'], feat['depth_max']
        ))
    conn.commit()
    cursor.close()
    conn.close()
    print(f'已导入 {len(features)} 条特征数据到MySQL数据库 {MYSQL_DB}.{MYSQL_TABLE}')

if __name__ == '__main__':
    features = batch_extract_features()
    save_to_csv(features, os.path.join(DATA_DIR, 'bellhop_features.csv'))
    import_to_mysql(features) 
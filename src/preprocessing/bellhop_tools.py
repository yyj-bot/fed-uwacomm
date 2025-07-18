"""
统一的Bellhop工具脚本，用于环境文件处理、生成和数据提取。
整合了各个预处理脚本的功能。
"""

import os
import re
import csv
import shutil
import random
import numpy as np
import glob
import pymysql
import dotenv
from pathlib import Path

# 加载.env文件
dotenv_path = Path(os.path.dirname(os.path.dirname(os.path.dirname(__file__)))) / '.env'
dotenv.load_dotenv(dotenv_path=dotenv_path)

# Bellhop环境文件标准模板 - 修正版本
ENV_TEMPLATE = """'Realistic Ocean Environment B{}'
{:.1f}
1
'CVW'
51  0.0  {:.1f}
{}
'A' 0.0
'A' 0.0
{:.1f} {:.2f} 0.0 {:.1f} {:.1f} /
1
{:.1f} /
1
{:.1f} /
101
0.0 {:.1f} /
'R'
101
-20.0 20.0 /
0.0 5500.0 101.0
"""

# 数据库连接参数
MYSQL_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', '123456Lrn.'),
    'database': os.getenv('DB_NAME', 'bellhop_data'),
    'table': os.getenv('DB_TABLE', 'features')
}

class OceanEnvironmentGenerator:
    """海洋环境数据生成器，用于生成真实的声速剖面等环境参数"""
    
    @staticmethod
    def generate_realistic_ssp(min_depth=0.0, max_depth=5000.0, num_points=20):
        """生成真实的声速剖面"""
        # 定义典型海洋环境中的关键层深度
        surface_layer_depth = random.uniform(100, 300)
        thermocline_depth = random.uniform(800, 1200)
        
        # 随机选择海洋区域类型
        ocean_type = random.choice(['tropical', 'temperate', 'arctic'])
        
        # 根据海洋类型设置声速参数
        if ocean_type == 'tropical':
            # 热带水域
            surface_speed = random.uniform(1535, 1545)
            min_speed = random.uniform(1480, 1490)
            deep_speed = random.uniform(1500, 1520)
        elif ocean_type == 'temperate':
            # 温带水域
            surface_speed = random.uniform(1505, 1525)
            min_speed = random.uniform(1475, 1485)
            deep_speed = random.uniform(1500, 1515)
        else:  # arctic
            # 极地水域
            surface_speed = random.uniform(1435, 1465)
            min_speed = random.uniform(1440, 1455)
            deep_speed = random.uniform(1470, 1490)
        
        # 生成严格递增的深度点
        depths = []
        depth_step = max_depth / (num_points - 1)
        
        for i in range(num_points):
            depth = min_depth + i * depth_step
            # 确保不超过最大深度
            if depth > max_depth:
                depth = max_depth
            depths.append(depth)
        
        # 生成声速值
        speeds = []
        
        for depth in depths:
            if depth <= surface_layer_depth:
                # 表层，声速随深度略微减小
                ratio = depth / surface_layer_depth
                speed = surface_speed - ratio * (surface_speed - min_speed) * 0.3
            elif depth <= thermocline_depth:
                # 热力跃变层，声速急剧减小然后开始增加
                ratio = (depth - surface_layer_depth) / (thermocline_depth - surface_layer_depth)
                if ratio < 0.5:
                    speed = surface_speed - 0.3 * (surface_speed - min_speed) - ratio * 2 * (surface_speed - min_speed) * 0.7
                else:
                    speed = min_speed + (ratio - 0.5) * 2 * (deep_speed - min_speed) * 0.3
            else:
                # 深海层，声速随深度缓慢增加
                ratio = (depth - thermocline_depth) / (max_depth - thermocline_depth)
                speed = min_speed + 0.3 * (deep_speed - min_speed) + ratio * (deep_speed - min_speed) * 0.7
                
            # 添加一些随机波动，模拟真实海洋环境的变化
            speed += random.uniform(-1.0, 1.0)
            speeds.append(speed)
        
        # 确保第一个和最后一个点符合预期
        speeds[0] = surface_speed
        speeds[-1] = deep_speed
        
        return list(zip(depths, speeds)), ocean_type
    
    @staticmethod
    def generate_bottom_params():
        """生成海底参数"""
        # 随机选择海底类型
        bottom_types = {
            'sand': (1650, 1750, 1.8, 2.1, 0.8, 1.0),
            'silt': (1550, 1650, 1.7, 1.9, 0.7, 0.9),
            'clay': (1500, 1600, 1.5, 1.7, 0.2, 0.6),
            'rock': (2000, 4000, 2.2, 2.6, 0.1, 0.3),
            'gravel': (1800, 2000, 2.0, 2.2, 0.6, 0.8)
        }
        bottom_type = random.choice(list(bottom_types.keys()))
        speed_range, density_range, atten_range = bottom_types[bottom_type][0:2], bottom_types[bottom_type][2:4], bottom_types[bottom_type][4:6]
        
        bottom_speed = random.uniform(*speed_range)
        bottom_density = random.uniform(*density_range)
        bottom_atten = random.uniform(*atten_range)
        
        return {
            'type': bottom_type,
            'speed': bottom_speed,
            'density': bottom_density,
            'atten': bottom_atten
        }
    
    @staticmethod
    def generate_frequency():
        """生成水声通信频率"""
        freq_type = random.choice(['low', 'medium', 'high'])
        
        if freq_type == 'low':
            # 低频，远距离传输
            return random.uniform(50, 500), 'low'
        elif freq_type == 'medium':
            # 中频，中等距离
            return random.uniform(500, 5000), 'medium'
        else:  # high
            # 高频，短距离高数据率
            return random.uniform(5000, 15000), 'high'

class BellhopFileManager:
    """Bellhop环境文件管理器，用于生成、修复和处理环境文件"""
    
    @staticmethod
    def generate_env_file(output_path, index=1, meta_data=None):
        """生成单个环境文件，并返回元数据"""
        # 如果没有提供元数据，生成新的
        if meta_data is None:
            meta_data = {}
            
            # 生成频率
            freq, freq_type = OceanEnvironmentGenerator.generate_frequency()
            meta_data['freq'] = freq
            meta_data['freq_type'] = freq_type
            
            # 设置水深
            water_depth = 5000.0
            meta_data['water_depth'] = water_depth
            
            # 生成声速剖面
            ssp_data, ocean_type = OceanEnvironmentGenerator.generate_realistic_ssp(0.0, water_depth, 25)
            meta_data['ocean_type'] = ocean_type
            meta_data['ssp_data'] = ssp_data
            
            # 生成海底参数
            bottom_params = OceanEnvironmentGenerator.generate_bottom_params()
            meta_data['bottom_type'] = bottom_params['type']
            meta_data['bottom_speed'] = bottom_params['speed']
            meta_data['bottom_density'] = bottom_params['density']
            meta_data['bottom_atten'] = bottom_params['atten']
            
            # 设置声源和接收器的深度
            meta_data['src_depth'] = random.uniform(50, 200)
            meta_data['rcv_depth'] = random.uniform(50, 200)
            
            # 设置最大通信范围
            meta_data['max_range'] = random.uniform(10.0, 20.0)  # km
        
        # 格式化声速剖面
        ssp_lines = ""
        for depth, speed in meta_data['ssp_data']:
            ssp_lines += f"  {depth:.2f}  {speed:.2f} /\n"
        
        # 生成环境文件
        env_content = ENV_TEMPLATE.format(
            index,
            meta_data['freq'],
            meta_data['water_depth'],
            ssp_lines,
            meta_data['water_depth'],
            meta_data['bottom_speed'],
            meta_data['bottom_density'],
            meta_data['bottom_atten'],
            meta_data['src_depth'],
            meta_data['rcv_depth'],
            meta_data['max_range']
        )
        
        # 写入文件
        with open(output_path, 'w') as f:
            f.write(env_content)
        
        print(f"已生成环境文件: {output_path}")
        return meta_data
    
    @staticmethod
    def batch_generate_files(output_dir='data/bellhop', num_files=15, backup=False):
        """批量生成环境文件"""
        # 创建输出目录
        os.makedirs(output_dir, exist_ok=True)
        
        # 删除所有旧的B*.env和B*.prt文件
        print("清理旧文件...")
        old_env_files = glob.glob(os.path.join(output_dir, 'B*.env'))
        old_prt_files = glob.glob(os.path.join(output_dir, 'B*.prt'))
        
        for f in old_env_files + old_prt_files:
            try:
                os.remove(f)
                print(f"已删除: {os.path.basename(f)}")
            except Exception as e:
                print(f"无法删除 {os.path.basename(f)}: {str(e)}")
        
        # 备份原始文件
        if backup:
            backup_dir = os.path.join(output_dir, 'original')
            os.makedirs(backup_dir, exist_ok=True)
            try:
                for env_file in [f for f in os.listdir('data/bellhop') if f.endswith('.env')]:
                    shutil.copy2(os.path.join('data/bellhop', env_file), 
                                 os.path.join(backup_dir, env_file))
            except Exception as e:
                print(f"备份原始文件时出错: {str(e)}")
        
        # 生成新文件
        success_count = 0
        meta_data_collection = []
        
        for i in range(1, num_files + 1):
            output_path = os.path.join(output_dir, f'B{i:02d}.env')  # 使用B01.env格式
            meta_data = BellhopFileManager.generate_env_file(output_path, i)
            meta_data['file_name'] = f'B{i:02d}'  # 使用B01格式
            meta_data_collection.append(meta_data)
            success_count += 1
        
        # 复制bellhopf.exe到输出目录
        try:
            shutil.copy2('data/bellhop/bellhopf.exe', os.path.join(output_dir, 'bellhopf.exe'))
        except Exception as e:
            print(f"无法复制bellhopf.exe: {str(e)}")
        
        print(f"共生成 {success_count} 个环境文件")
        return meta_data_collection
    
    @staticmethod
    def fix_env_file(env_path, backup=True):
        """修复环境文件中的声速剖面"""
        try:
            # 读取原始文件内容
            with open(env_path, 'r') as file:
                lines = file.readlines()
            
            # 识别原文件中的声速剖面段
            ssp_start = -1
            ssp_end = -1
            water_depth = 5000.0  # 默认水深
            
            # 寻找水深信息
            for i, line in enumerate(lines):
                if "DEPTH of bottom" in line:
                    parts = line.split()
                    if len(parts) >= 3:
                        try:
                            water_depth = float(parts[2])
                        except:
                            pass
            
            # 寻找声速剖面段
            for i, line in enumerate(lines):
                if i > 4 and "/" in line and ssp_start == -1:
                    ssp_start = i
                    continue
                if ssp_start != -1 and "'A'" in line:
                    ssp_end = i
                    break
            
            # 如果未找到声速剖面段，返回错误
            if ssp_start == -1 or ssp_end == -1:
                print(f"警告: 无法在 {env_path} 中找到声速剖面段")
                return False
            
            # 生成新的声速剖面
            num_points = ssp_end - ssp_start
            ssp_data, _ = OceanEnvironmentGenerator.generate_realistic_ssp(0.0, int(water_depth), num_points)
            
            # 创建新的行
            new_lines = lines[:ssp_start]
            
            for depth, speed in ssp_data:
                new_lines.append(f"  {depth:.2f}  {speed:.2f} /\n")
            
            new_lines.extend(lines[ssp_end:])
            
            # 备份原文件
            if backup:
                backup_path = env_path + ".bak"
                shutil.copy2(env_path, backup_path)
            
            # 写入修改后的内容
            with open(env_path, 'w') as file:
                file.writelines(new_lines)
            
            print(f"已成功修复 {env_path}")
            return True
        
        except Exception as e:
            print(f"修复 {env_path} 时出错: {str(e)}")
            return False
    
    @staticmethod
    def batch_fix_env_files(data_dir='data/bellhop'):
        """批量修复目录下所有的环境文件"""
        env_files = [f for f in os.listdir(data_dir) if f.endswith('.env')]
        
        success = 0
        for env_file in env_files:
            env_path = os.path.join(data_dir, env_file)
            if BellhopFileManager.fix_env_file(env_path):
                success += 1
        
        print(f"共处理 {len(env_files)} 个文件，成功修复 {success} 个")
        return success

class BellhopFeatureExtractor:
    """Bellhop特征提取器，用于从输出文件中提取有用信息"""
    
    @staticmethod
    def extract_prt_features(prt_path):
        """提取.prt文件中的声速剖面、频率等特征"""
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
    
    @staticmethod
    def batch_extract_features(data_dir='data/bellhop'):
        """批量处理所有仿真输出文件"""
        all_features = []
        for fname in os.listdir(data_dir):
            if fname.endswith('.prt'):
                fpath = os.path.join(data_dir, fname)
                feat = BellhopFeatureExtractor.extract_prt_features(fpath)
                all_features.append(feat)
        return all_features
    
    @staticmethod
    def import_to_mysql(features):
        """导入特征到MySQL数据库"""
        try:
            conn = pymysql.connect(
                host=os.getenv('DB_HOST', 'localhost'),
                port=int(os.getenv('DB_PORT', 3306)),
                user=os.getenv('DB_USER', 'root'),
                password=os.getenv('DB_PASSWORD', '123456Lrn.'),
                database=os.getenv('DB_NAME', 'bellhop_data'),
                charset='utf8'
            )
            cursor = conn.cursor()
            # 自动建表
            create_sql = f'''
            CREATE TABLE IF NOT EXISTS {os.getenv('DB_TABLE', 'features')} (
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
            cursor.execute(f"TRUNCATE TABLE {os.getenv('DB_TABLE', 'features')}")
            # 插入数据
            insert_sql = f'''
            INSERT INTO {os.getenv('DB_TABLE', 'features')} (filename, success, error_msg, freq, ssp_points, ssp_min, ssp_max, ssp_mean, depth_min, depth_max)
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
            print(f'已导入 {len(features)} 条特征数据到MySQL数据库 {os.getenv("DB_NAME", "bellhop_data")}.{os.getenv("DB_TABLE", "features")}')
        except Exception as e:
            print(f"导入MySQL时出错: {str(e)}")
            return False

class BellhopManager:
    """Bellhop模型管理器，用于执行完整的工作流程"""
    
    @staticmethod
    def run_complete_workflow(output_dir='data/bellhop', num_files=15):
        """执行完整的工作流程：生成环境文件、处理Bellhop、提取特征、导入数据库"""
        print("1. 生成真实环境数据文件...")
        meta_data_collection = BellhopFileManager.batch_generate_files(output_dir, num_files, backup=False)
        
        print("\n2. 运行Bellhop处理环境文件...")
        BellhopManager.run_bellhop_batch(output_dir)
        
        print("\n3. 提取声速剖面特征...")
        features = BellhopFeatureExtractor.batch_extract_features(output_dir)
        
        print("\n4. 导入数据到MySQL...")
        BellhopFeatureExtractor.import_to_mysql(features)
        
        print("\n工作流程完成！")
    
    @staticmethod
    def run_bellhop_batch(env_dir):
        """批量运行Bellhop处理环境文件"""
        env_files = [f for f in os.listdir(env_dir) if f.endswith('.env')]
        current_dir = os.getcwd()
        success_count = 0
        
        try:
            os.chdir(env_dir)
            for env_file in env_files:
                env_name = os.path.splitext(env_file)[0]  # 获取不带扩展名的文件名
                print(f"处理: {env_name}")
                os.system(f"bellhopf.exe {env_name}")
                
                # 检查是否生成了输出文件
                if os.path.exists(f"{env_name}.prt"):
                    print(f"成功处理环境文件 {env_file}")
                    success_count += 1
                
                # 清理旧格式的文件（如果存在）
                # 从B01格式转换为可能的B1_strict格式
                if env_name.startswith("B0"):
                    old_format = env_name.replace("B0", "B") + "_strict"
                    if os.path.exists(f"{old_format}.env"):
                        os.remove(f"{old_format}.env")
                    if os.path.exists(f"{old_format}.prt"):
                        os.remove(f"{old_format}.prt")
                    
        except Exception as e:
            print(f"批量处理环境文件时出错: {str(e)}")
        finally:
            os.chdir(current_dir)
        
        print(f"共处理 {len(env_files)} 个环境文件，成功 {success_count} 个")
        return success_count

def main():
    """主函数，提供命令行接口"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Bellhop环境数据处理工具')
    parser.add_argument('--action', choices=['generate', 'fix', 'extract', 'workflow'], 
                      default='workflow', help='执行的操作类型')
    parser.add_argument('--dir', default='data/bellhop_real', help='输入/输出目录')
    parser.add_argument('--num', type=int, default=15, help='生成文件数量')
    
    args = parser.parse_args()
    
    if args.action == 'generate':
        BellhopFileManager.batch_generate_files(args.dir, args.num)
    elif args.action == 'fix':
        BellhopFileManager.batch_fix_env_files(args.dir)
    elif args.action == 'extract':
        features = BellhopFeatureExtractor.batch_extract_features(args.dir)
        BellhopFeatureExtractor.import_to_mysql(features)
    elif args.action == 'workflow':
        BellhopManager.run_complete_workflow(args.dir, args.num)

if __name__ == '__main__':
    main() 
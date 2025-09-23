#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
FedUWAComm 完整标准工作流
按照标准流程：环境生成 → BELLHOP仿真 → 特征提取 → 数据库存储 → 机器学习训练
"""

import sys
import os
import time
import multiprocessing
from pathlib import Path
import logging
import pandas as pd
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import List, Dict, Optional, Tuple, Any

# 设置环境变量以支持UTF-8编码
os.environ['PYTHONIOENCODING'] = 'utf-8'

# 在Windows下尝试重新配置控制台编码
if sys.platform == 'win32':
    try:
        # 尝试设置控制台代码页为UTF-8
        os.system('chcp 65001 > nul 2>&1')
        # 检查是否在交互式环境中
        if hasattr(sys.stdout, 'isatty') and sys.stdout.isatty():
            # 重新配置stdout，但要检查是否有buffer属性
            if hasattr(sys.stdout, 'buffer'):
                import io
                sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    except Exception:
        # 如果设置失败，继续使用默认编码
        pass

# 尝试导入进度条库
try:
    from tqdm import tqdm
    HAS_TQDM = True
except ImportError:
    HAS_TQDM = False
    # 简单的进度显示函数
    def tqdm(iterable, desc="处理中", **kwargs):
        total = len(iterable) if hasattr(iterable, '__len__') else None
        for i, item in enumerate(iterable):
            if total and (i + 1) % max(1, total // 10) == 0:
                print(f"\r{desc}: {i+1}/{total} ({(i+1)/total*100:.1f}%)", end="", flush=True)
            yield item
        print()  # 换行

# 添加src目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root / 'src'))

from feduwacomm.acoustic.generate_environments import BellhopEnvGenerator
from feduwacomm.acoustic.run_bellhop import BellhopProcessor
from feduwacomm.ml.feature_extractor import BellhopFeatureExtractor
from feduwacomm.database.database import DatabaseManager
from feduwacomm.ml.random_forest_trainer import RandomForestTrainer
from feduwacomm.ml.model_evaluator import ModelEvaluator

# 设置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def get_data_directory() -> Path:
    """智能确定数据目录路径"""
    # 优先使用环境变量
    if 'FEDUWACOMM_DATA_DIR' in os.environ:
        data_dir = Path(os.environ['FEDUWACOMM_DATA_DIR'])
        if data_dir.exists():
            return data_dir
    
    # 尝试多个可能的路径
    possible_paths = [
        Path.cwd() / 'data' / 'bellhop',                    # 当前目录下
        Path.cwd() / 'python-vm' / 'data' / 'bellhop',      # 从项目根目录运行
        Path(__file__).parent.parent / 'data' / 'bellhop',  # 相对于脚本位置
        Path.cwd().parent / 'python-vm' / 'data' / 'bellhop', # 从子目录运行
    ]
    
    # 查找已存在的目录
    for path in possible_paths:
        if path.exists() and path.is_dir():
            return path.resolve()
    
    # 如果都不存在，创建默认路径
    default_path = Path.cwd() / 'data' / 'bellhop'
    default_path.mkdir(parents=True, exist_ok=True)
    return default_path.resolve()

def validate_python_dependencies() -> List[str]:
    """验证Python依赖包"""
    required_modules = {
        'numpy': 'numpy',
        'pandas': 'pandas', 
        'sklearn': 'scikit-learn',
        'joblib': 'joblib',
        'matplotlib': 'matplotlib',
        'seaborn': 'seaborn'
    }
    
    missing = []
    for module, package in required_modules.items():
        try:
            __import__(module)
        except ImportError:
            missing.append(f"{package} (import {module})")
    
    return missing

def validate_bellhop_setup(data_dir: Path) -> Tuple[bool, List[str]]:
    """验证BELLHOP设置"""
    issues = []
    
    # 检查BELLHOP可执行文件
    bellhop_exes = [
        data_dir / "bellhop.exe",
        data_dir / "bellhopf.exe",
        data_dir / "bellhop3d.exe"
    ]
    
    found_exe = None
    for exe in bellhop_exes:
        if exe.exists() and exe.is_file():
            found_exe = exe
            break
    
    if found_exe is None:
        issues.append(f"BELLHOP可执行文件未找到，请在 {data_dir} 目录下放置以下文件之一:")
        for exe in bellhop_exes:
            issues.append(f"  - {exe.name}")
        return False, issues
    
    return True, [f"找到BELLHOP可执行文件: {found_exe.name}"]

def check_environment() -> bool:
    """全面的环境配置检查"""
    print("🔍 检查环境配置...")
    print("="*50)
    
    all_good = True
    
    # 1. 检查Python依赖
    print("📦 检查Python依赖包...")
    missing_deps = validate_python_dependencies()
    if missing_deps:
        print("❌ 缺少以下Python包:")
        for dep in missing_deps:
            print(f"  - {dep}")
        print("\n请运行: pip install -r requirements.txt")
        all_good = False
    else:
        print("✅ 所有Python依赖包已安装")
    
    # 2. 检查数据目录
    print("\n📁 检查数据目录...")
    try:
        data_dir = get_data_directory()
        print(f"✅ 数据目录: {data_dir}")
        
        # 确保目录存在
        data_dir.mkdir(parents=True, exist_ok=True)
        
    except Exception as e:
        print(f"❌ 数据目录设置失败: {e}")
        all_good = False
    
    # 3. 检查BELLHOP设置
    print("\n🌊 检查BELLHOP设置...")
    bellhop_ok, bellhop_messages = validate_bellhop_setup(data_dir)
    for msg in bellhop_messages:
        print(f"{'✅' if bellhop_ok else '❌'} {msg}")
    
    if not bellhop_ok:
        all_good = False
    
    # 4. 检查.env文件 (可选)
    print("\n⚙️  检查配置文件...")
    env_paths = [
        Path('.env'),
        Path('python-vm/.env'),
        Path('../.env'),
    ]
    
    env_file = None
    for path in env_paths:
        if path.exists():
            env_file = path
            break
    
    if env_file:
        print(f"✅ 找到配置文件: {env_file}")
    else:
        print("⚠️  未找到.env配置文件 (将使用默认配置)")
        print("   可选: 创建.env文件自定义数据库等配置")
    
    print("\n" + "="*50)
    if all_good:
        print("🎉 环境检查通过，可以开始运行工作流!")
    else:
        print("❌ 环境检查失败，请解决上述问题后重试")
    
    return all_good

def step1_generate_bellhop_environments(num_environments: int = 100) -> bool:
    """步骤1: 生成BELLHOP环境文件"""
    print("\n" + "="*60)
    print("📁 步骤1: 生成BELLHOP环境文件")
    print("="*60)
    
    try:
        # 获取数据目录
        data_dir = get_data_directory()
        print(f"📂 使用数据目录: {data_dir}")
        
        # 初始化生成器
        generator = BellhopEnvGenerator(output_dir=str(data_dir))
        
        # 检查是否已有环境文件
        existing_env_files = list(data_dir.glob('*.env'))
        if existing_env_files:
            print(f"⚠️  发现 {len(existing_env_files)} 个已存在的环境文件")
            
            try:
                choice = input("是否覆盖已存在的文件? (y/N): ").strip().lower()
                if choice not in ['y', 'yes', '是']:
                    print("ℹ️  保留已存在的环境文件，跳过生成步骤")
                    return True
            except KeyboardInterrupt:
                print("\n❌ 用户取消操作")
                return False
            
            # 清理旧文件
            print("🧹 清理旧环境文件...")
            for old_file in existing_env_files:
                try:
                    old_file.unlink()
                except Exception as e:
                    print(f"⚠️  无法删除 {old_file.name}: {e}")
        
        print(f"🔄 开始生成 {num_environments} 个环境文件...")
        
        # 生成环境文件
        start_time = time.time()
        files_generated = generator.generate_all_environments(num_environments)
        generation_time = time.time() - start_time
        
        if files_generated and len(files_generated) > 0:
            print(f"\n✅ 成功生成 {len(files_generated)} 个环境文件")
            print(f"⏱️  生成耗时: {generation_time:.1f} 秒")
            print(f"📈 平均速度: {len(files_generated)/generation_time:.1f} 文件/秒")
            
            # 验证生成的文件
            valid_files = 0
            for file_path in files_generated:
                if file_path.exists() and file_path.stat().st_size > 0:
                    valid_files += 1
            
            if valid_files == len(files_generated):
                print(f"✅ 所有生成的文件都有效")
                return True
            else:
                print(f"⚠️  {len(files_generated) - valid_files} 个文件可能有问题")
                return valid_files > len(files_generated) * 0.8  # 允许20%的失败率
        else:
            print("❌ 环境文件生成失败：未生成任何文件")
            return False
        
    except KeyboardInterrupt:
        print("\n❌ 用户中断操作")
        return False
    except Exception as e:
        print(f"❌ 环境文件生成失败: {e}")
        logger.exception("详细错误信息:")
        return False

def step2_run_bellhop_simulation(parallel: bool = False, max_workers: Optional[int] = None) -> bool:
    """步骤2: 运行BELLHOP仿真"""
    print("\n" + "="*60)
    print("🌊 步骤2: 运行BELLHOP仿真")
    print("="*60)
    
    try:
        # 获取数据目录
        data_dir = get_data_directory()
        print(f"📂 使用数据目录: {data_dir}")
        
        # 初始化处理器
        processor = BellhopProcessor(bellhop_dir=str(data_dir))
        
        # 检查BELLHOP可执行文件
        if not hasattr(processor, 'bellhop_exe') or processor.bellhop_exe is None:
            print("❌ BELLHOP可执行文件未找到")
            print("请确保以下位置存在BELLHOP可执行文件:")
            for exe_name in ['bellhop.exe', 'bellhopf.exe', 'bellhop3d.exe']:
                print(f"  - {data_dir}/{exe_name}")
            return False
        
        print(f"✅ 找到BELLHOP可执行文件: {Path(processor.bellhop_exe).name}")
        
        # 获取环境文件
        env_files = sorted(list(data_dir.glob('*.env')))
        
        if not env_files:
            print("❌ 未找到环境文件，请先运行步骤1")
            return False
        
        print(f"📄 找到 {len(env_files)} 个环境文件")
        
        # 检查是否已有仿真结果
        existing_results = []
        for ext in ['.prt', '.arr', '.ray', '.shd']:
            existing_results.extend(list(data_dir.glob(f'*{ext}')))
        
        if existing_results:
            print(f"⚠️  发现 {len(existing_results)} 个已存在的仿真结果文件")
            try:
                choice = input("是否重新运行仿真? (y/N): ").strip().lower()
                if choice not in ['y', 'yes', '是']:
                    print("ℹ️  使用已存在的仿真结果")
                    return True
            except KeyboardInterrupt:
                print("\n❌ 用户取消操作")
                return False
        
        # 开始仿真
        print(f"🔄 开始仿真处理{'(并行模式)' if parallel else '(串行模式)'}...")
        start_time = time.time()
        
        if parallel and len(env_files) > 1:
            # 并行处理
            success_count = _run_bellhop_parallel(processor, env_files, max_workers)
        else:
            # 串行处理
            success_count = _run_bellhop_sequential(processor, env_files)
        
        simulation_time = time.time() - start_time
        total_count = len(env_files)
        
        print(f"\n✅ BELLHOP仿真完成: {success_count}/{total_count} 成功")
        print(f"⏱️  仿真耗时: {simulation_time:.1f} 秒")
        print(f"📈 成功率: {success_count/total_count*100:.1f}%")
        
        if success_count == 0:
            print("❌ 没有成功的仿真结果")
            return False
        elif success_count < total_count * 0.5:
            print("⚠️  成功率较低，请检查BELLHOP配置和环境文件")
            
        return success_count > 0
        
    except KeyboardInterrupt:
        print("\n❌ 用户中断仿真")
        return False
    except Exception as e:
        print(f"❌ BELLHOP仿真失败: {e}")
        logger.exception("详细错误信息:")
        return False

def _run_bellhop_sequential(processor, env_files: List[Path]) -> int:
    """串行运行BELLHOP仿真"""
    success_count = 0
    
    if HAS_TQDM:
        # 使用tqdm进度条
        progress_bar = tqdm(env_files, desc="🌊 BELLHOP仿真", 
                           bar_format='{l_bar}{bar}| {n_fmt}/{total_fmt} [{elapsed}<{remaining}, {rate_fmt}] {postfix}')
        
        for env_file in progress_bar:
            try:
                success = processor.run_bellhop(str(env_file))
                if success:
                    success_count += 1
                
                # 更新进度条信息
                progress_bar.set_postfix({
                    '成功': f"{success_count}/{len(env_files)}",
                    '当前': env_file.stem[:15]
                })
            except Exception as e:
                logger.warning(f"处理 {env_file.name} 失败: {e}")
                continue
    else:
        # 使用简单进度显示
        total = len(env_files)
        for i, env_file in enumerate(env_files):
            try:
                success = processor.run_bellhop(str(env_file))
                if success:
                    success_count += 1
                
                # 每10%显示一次进度
                if (i + 1) % max(1, total // 10) == 0 or i == total - 1:
                    percentage = (i + 1) / total * 100
                    print(f"\r🌊 仿真进度: {percentage:.1f}% ({i+1}/{total}) - 成功: {success_count}", end="", flush=True)
                    
            except Exception as e:
                logger.warning(f"处理 {env_file.name} 失败: {e}")
                continue
        print()  # 换行
    
    return success_count

def _run_bellhop_parallel(processor, env_files: List[Path], max_workers: Optional[int]) -> int:
    """并行运行BELLHOP仿真"""
    if max_workers is None:
        max_workers = min(4, multiprocessing.cpu_count(), len(env_files))
    
    print(f"🚀 使用 {max_workers} 个并行线程")
    
    success_count = 0
    completed_count = 0
    
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # 提交任务
        future_to_file = {executor.submit(processor.run_bellhop, str(env_file)): env_file 
                         for env_file in env_files}
        
        # 处理结果
        if HAS_TQDM:
            # 使用tqdm进度条
            progress_bar = tqdm(as_completed(future_to_file), total=len(env_files), 
                              desc="🚀 并行仿真",
                              bar_format='{l_bar}{bar}| {n_fmt}/{total_fmt} [{elapsed}<{remaining}] {postfix}')
            
            for future in progress_bar:
                env_file = future_to_file[future]
                completed_count += 1
                
                try:
                    success = future.result(timeout=300)  # 5分钟超时
                    if success:
                        success_count += 1
                except Exception as e:
                    logger.warning(f"处理 {env_file.name} 失败: {e}")
                
                # 更新进度条信息
                progress_bar.set_postfix({
                    '成功': f"{success_count}/{len(env_files)}",
                    '成功率': f"{success_count/completed_count*100:.1f}%" if completed_count > 0 else "0%"
                })
        else:
            # 使用简单进度显示
            total = len(env_files)
            for future in as_completed(future_to_file):
                env_file = future_to_file[future]
                completed_count += 1
                
                try:
                    success = future.result(timeout=300)  # 5分钟超时
                    if success:
                        success_count += 1
                except Exception as e:
                    logger.warning(f"处理 {env_file.name} 失败: {e}")
                
                # 每10%显示一次进度
                if completed_count % max(1, total // 10) == 0 or completed_count == total:
                    percentage = completed_count / total * 100
                    success_rate = success_count / completed_count * 100 if completed_count > 0 else 0
                    print(f"\r🚀 并行仿真: {percentage:.1f}% ({completed_count}/{total}) - 成功: {success_count} ({success_rate:.1f}%)", end="", flush=True)
            print()  # 换行
    
    return success_count

def extract_features_with_progress(extractor, data_dir: Path) -> pd.DataFrame:
    """带进度条的特征提取函数"""
    import threading
    import sys
    
    # 获取所有环境ID
    env_ids = set()
    for file_path in data_dir.glob("*.env"):
        env_id = extractor._extract_env_id(file_path.name)
        if env_id:
            env_ids.add(env_id)
    
    env_ids = sorted(env_ids)
    total_envs = len(env_ids)
    
    if total_envs == 0:
        print("   ❌ 没有找到有效的环境ID")
        return pd.DataFrame()
    
    print(f"   📊 准备处理 {total_envs} 个环境的特征提取")
    
    # 共享变量用于进度跟踪
    progress = {'current': 0, 'current_env': '', 'completed': False}
    all_features = []
    
    def progress_monitor():
        """进度监控线程"""
        spinner = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧']
        spinner_idx = 0
        start_time = time.time()
        
        while not progress['completed']:
            current = progress['current']
            current_env = progress['current_env']
            percentage = (current / total_envs * 100) if total_envs > 0 else 0
            
            # 计算预估剩余时间
            elapsed_time = time.time() - start_time
            if current > 0:
                avg_time_per_env = elapsed_time / current
                remaining_envs = total_envs - current
                eta_seconds = avg_time_per_env * remaining_envs
                eta_str = f"{int(eta_seconds//60)}m{int(eta_seconds%60)}s"
            else:
                eta_str = "计算中..."
            
            # 创建进度条
            bar_length = 25
            filled_length = int(bar_length * current / total_envs) if total_envs > 0 else 0
            bar = '█' * filled_length + '░' * (bar_length - filled_length)
            
            # 显示进度信息
            spinner_char = spinner[spinner_idx % len(spinner)]
            elapsed_str = f"{int(elapsed_time//60)}m{int(elapsed_time%60)}s"
            
            # 截断环境名称以适应显示
            display_env = current_env[:12] + '...' if len(current_env) > 15 else current_env
            
            progress_line = (f'\r   {spinner_char} 特征提取: [{bar}] {percentage:.1f}% '
                           f'({current}/{total_envs}) | 当前: {display_env} | '
                           f'已用: {elapsed_str} | 预计剩余: {eta_str}')
            
            sys.stdout.write(progress_line)
            sys.stdout.flush()
            
            spinner_idx += 1
            time.sleep(0.2)  # 稍微慢一点，减少CPU占用
    
    # 启动进度监控线程
    monitor_thread = threading.Thread(target=progress_monitor, daemon=True)
    monitor_thread.start()
    
    try:
        # 执行特征提取
        for i, env_id in enumerate(env_ids):
            progress['current'] = i
            progress['current_env'] = env_id
            
            try:
                features = extractor.extract_all_features(env_id)
                all_features.append(features)
            except Exception as e:
                # 记录错误但继续处理其他环境
                print(f"\n   ⚠️  处理 {env_id} 时出错: {e}")
                # 添加一个错误记录
                error_features = {'env_id': env_id, 'error': str(e)}
                all_features.append(error_features)
        
        progress['current'] = total_envs
        progress['completed'] = True
        
        # 等待进度条更新完成
        time.sleep(0.2)
        print(f'\n   ✅ 特征提取完成！处理了 {len(all_features)} 个环境')
        
    except Exception as e:
        progress['completed'] = True
        print(f'\n   ❌ 特征提取过程中发生错误: {e}')
        raise
    
    # 转换为DataFrame
    if all_features:
        df = pd.DataFrame(all_features)
        print(f"   📊 生成特征矩阵: {len(df)} 行 × {len(df.columns)} 列")
        return df
    else:
        print("   ❌ 没有提取到任何特征")
        return pd.DataFrame()

def step3_extract_features() -> Optional[str]:
    """步骤3: 特征提取"""
    print("\n" + "="*60)
    print("🔍 步骤3: 特征提取")
    print("="*60)
    
    try:
        # 获取数据目录
        data_dir = get_data_directory()
        print(f"📂 使用数据目录: {data_dir}")
        
        # 检查仿真结果文件
        result_files = []
        for ext in ['.prt', '.arr', '.ray', '.shd']:
            result_files.extend(list(data_dir.glob(f'*{ext}')))
        
        if not result_files:
            print("❌ 未找到BELLHOP仿真结果文件，请先运行步骤2")
            return None
        
        print(f"📄 找到 {len(result_files)} 个仿真结果文件")
        
        # 检查是否已有特征文件
        possible_csv_paths = [
            Path.cwd() / "bellhop_features_extracted.csv",
            Path.cwd() / "python-vm" / "bellhop_features_extracted.csv",
            data_dir.parent.parent / "bellhop_features_extracted.csv"
        ]
        
        existing_csv = None
        for csv_path in possible_csv_paths:
            if csv_path.exists():
                existing_csv = csv_path
                break
        
        if existing_csv:
            print(f"⚠️  发现已存在的特征文件: {existing_csv}")
            try:
                choice = input("是否重新提取特征? (y/N): ").strip().lower()
                if choice not in ['y', 'yes', '是']:
                    print("ℹ️  使用已存在的特征文件")
                    return str(existing_csv)
            except KeyboardInterrupt:
                print("\n❌ 用户取消操作")
                return None
        
        # 初始化特征提取器
        extractor = BellhopFeatureExtractor(data_dir=str(data_dir))
        
        print("🔄 开始特征提取...")
        start_time = time.time()
        
        # 添加超时和进度监控的特征提取
        try:
            print("   正在扫描环境文件...")
            env_files = list(data_dir.glob("*.env"))
            print(f"   找到 {len(env_files)} 个环境文件")
            
            if len(env_files) == 0:
                print("❌ 没有找到任何环境文件")
                return None
            
            # 检查是否有对应的结果文件
            result_count = 0
            for ext in ['.prt', '.arr', '.ray', '.shd']:
                result_count += len(list(data_dir.glob(f'*{ext}')))
            
            print(f"   找到 {result_count} 个结果文件")
            
            if result_count == 0:
                print("❌ 没有找到任何BELLHOP结果文件")
                return None
            
            print("   开始特征提取处理...")
            
            # 创建一个带进度监控的特征提取函数
            features_df = extract_features_with_progress(extractor, data_dir)
            
        except KeyboardInterrupt:
            print("\n❌ 用户中断特征提取")
            return None
        except Exception as e:
            print(f"❌ 特征提取过程中出错: {e}")
            print("   可能的原因:")
            print("   - 某个结果文件格式损坏")
            print("   - 内存不足")
            print("   - 文件权限问题")
            logger.exception("特征提取详细错误:")
            return None
            
        extraction_time = time.time() - start_time
        
        if features_df is None or len(features_df) == 0:
            print("❌ 特征提取失败，未获得有效特征")
            return None
        
        # 验证特征数据质量
        print(f"\n📊 特征数据质量分析:")
        numeric_cols = features_df.select_dtypes(include=['number']).columns
        print(f"   - 数值型特征: {len(numeric_cols)} 个")
        
        # 检查缺失值
        missing_stats = features_df.isnull().sum()
        high_missing_cols = missing_stats[missing_stats > len(features_df) * 0.5]
        if len(high_missing_cols) > 0:
            print(f"   - 高缺失率列 (>50%): {len(high_missing_cols)} 个")
        else:
            print(f"   - 数据完整性良好")
        
        # 决定保存路径
        save_dir = Path.cwd()
        if (Path.cwd() / 'python-vm').exists():
            # 从项目根目录运行
            save_dir = Path.cwd()
        elif Path.cwd().name == 'scripts':
            # 从 scripts 目录运行
            save_dir = Path.cwd().parent.parent
        
        csv_file = save_dir / "bellhop_features_extracted.csv"
        
        # 保存特征数据
        print(f"💾 保存特征数据到: {csv_file}")
        features_df.to_csv(csv_file, index=False, encoding='utf-8')
        
        # 验证保存的文件
        if csv_file.exists() and csv_file.stat().st_size > 0:
            print(f"\n✅ 特征提取完成:")
            print(f"   - 环境数: {len(features_df)}")
            print(f"   - 特征数: {len(features_df.columns)}")
            print(f"   - 文件大小: {csv_file.stat().st_size / 1024:.1f} KB")
            print(f"   - 提取耗时: {extraction_time:.1f} 秒")
            
            return str(csv_file)
        else:
            print("❌ 特征文件保存失败")
            return None
        
    except KeyboardInterrupt:
        print("\n❌ 用户中断特征提取")
        return None
    except Exception as e:
        print(f"❌ 特征提取失败: {e}")
        logger.exception("详细错误信息:")
        return None

def step4_database_storage(csv_file: str) -> bool:
    """步骤4: 数据库存储 - 优化版"""
    print("\n" + "="*60)
    print("💾 步骤4: 数据库存储")
    print("="*60)
    
    if not csv_file or not Path(csv_file).exists():
        print(f"❌ CSV文件不存在: {csv_file}")
        return False
    
    try:
        # 验证CSV文件
        csv_path = Path(csv_file)
        print(f"📄 验证CSV文件: {csv_path}")
        print(f"   - 文件大小: {csv_path.stat().st_size / 1024:.1f} KB")
        
        # 快速检查CSV内容
        try:
            sample_df = pd.read_csv(csv_file, nrows=5)
            print(f"   - 列数: {len(sample_df.columns)}")
            print(f"   - 预览: {list(sample_df.columns[:5])}...")
        except Exception as e:
            print(f"❌ CSV文件格式错误: {e}")
            return False
        
        # 初始化数据库管理器
        db = DatabaseManager()
        
        print("\n🔄 连接数据库...")
        if not db.connect():
            print("❌ 数据库连接失败")
            return False
        
        print(f"✅ 数据库连接成功: {db.db_path}")
        
        # 检查是否已有数据
        try:
            existing_info = db.get_table_info()
            if existing_info.get('row_count', 0) > 0:
                print(f"⚠️  数据库中已有 {existing_info['row_count']} 条记录")
                try:
                    choice = input("是否覆盖已存在的数据? (y/N): ").strip().lower()
                    if choice not in ['y', 'yes', '是']:
                        print("ℹ️  保留已存在的数据")
                        db.disconnect()
                        return True
                except KeyboardInterrupt:
                    print("\n❌ 用户取消操作")
                    db.disconnect()
                    return False
        except Exception:
            # 表不存在，正常情况
            pass
        
        print("🔄 创建表并导入数据...")
        start_time = time.time()
        
        success = db.create_and_populate_from_csv(csv_file)
        import_time = time.time() - start_time
        
        if success:
            # 获取导入结果统计
            info = db.get_table_info()
            print(f"\n✅ 数据库存储完成:")
            print(f"   - 记录数: {info.get('row_count', 0)}")
            print(f"   - 列数: {len(info.get('columns', []))}")
            print(f"   - 导入耗时: {import_time:.1f} 秒")
            
            # 显示样本数据
            if info.get('sample_data'):
                print(f"   - 样本数据: env_id={info['sample_data'][0].get('env_id', 'N/A')}")
        else:
            print("❌ 数据库存储失败")
            
        db.disconnect()
        return success
        
    except KeyboardInterrupt:
        print("\n❌ 用户中断数据库操作")
        try:
            db.disconnect()
        except:
            pass
        return False
    except Exception as e:
        print(f"❌ 数据库存储失败: {e}")
        logger.exception("详细错误信息:")
        try:
            db.disconnect()
        except:
            pass
        return False

def step5_machine_learning():
    """步骤5: 机器学习训练"""
    print("\n" + "="*60)
    print("🤖 步骤5: 机器学习训练")
    print("="*60)
    
    try:
        # 使用数据库中的数据进行训练
        db = DatabaseManager()
        db.connect()
        
        # 获取数据
        features_df = db.get_features_for_training()
        db.disconnect()
        
        if features_df is None or len(features_df) == 0:
            print("❌ 无法从数据库获取训练数据")
            return False
        
        print(f"✅ 从数据库获取了 {len(features_df)} 条训练数据")
        
        # 初始化训练器
        trainer = RandomForestTrainer()
        
        # 数据准备 - 使用实际存在的数值型目标变量
        target_cols = [
            'shd_mean_tl',           # 传播损失(传输损失)
            'arr_mean_amplitude',    # 信号强度
            'arr_mean_time',         # 到达时间
            'ray_path_complexity',   # 路径复杂度
            'shd_energy_distribution' # 能量分布
        ]
        available_targets = [col for col in target_cols if col in features_df.columns]
        
        # 检查哪些目标变量有足够的非空数据
        valid_targets = []
        for col in available_targets:
            non_null_count = features_df[col].notna().sum()
            null_ratio = features_df[col].isnull().sum() / len(features_df)
            print(f"   {col}: {non_null_count} 非空值 ({null_ratio*100:.1f}% 空值)")
            
            # 只保留空值比例低于80%的目标变量
            if null_ratio < 0.8:
                valid_targets.append(col)
        
        if not valid_targets:
            print("❌ 未找到足够数据的目标变量，尝试使用合成目标变量")
            # 如果没有足够的真实目标变量，创建合成目标变量用于演示
            print("🔄 创建合成目标变量用于模型训练...")
            trainer = RandomForestTrainer()
            X_temp, _ = trainer.prepare_data(features_df, target_cols=[])
            if len(X_temp) > 0:
                synthetic_targets = trainer.create_synthetic_targets(X_temp)
                # 将合成目标添加到DataFrame
                for col in synthetic_targets.columns:
                    features_df[f'synthetic_{col}'] = synthetic_targets[col]
                valid_targets = [f'synthetic_{col}' for col in synthetic_targets.columns]
                print(f"   创建了合成目标变量: {valid_targets}")
            else:
                print("❌ 无法创建合成目标变量")
                return False
        
        print(f"🎯 使用目标变量: {valid_targets}")
        
        # 数据清理：移除包含过多NaN的行和列
        print("🧹 清理数据中的空值...")
        original_size = len(features_df)
        
        # 选择数值型特征列
        numeric_cols = features_df.select_dtypes(include=['number']).columns.tolist()
        # 移除id列
        if 'id' in numeric_cols:
            numeric_cols.remove('id')
        
        # 1. 首先移除全为NaN的列
        features_df_clean = features_df.dropna(axis=1, how='all')
        
        # 2. 移除超过70%为NaN的列
        threshold = 0.7
        null_ratio = features_df_clean.isnull().sum() / len(features_df_clean)
        cols_to_keep = null_ratio[null_ratio < threshold].index.tolist()
        features_df_clean = features_df_clean[cols_to_keep]
        
        # 3. 为目标变量填充缺失值（使用均值）
        for target_col in valid_targets:
            if target_col in features_df_clean.columns:
                if features_df_clean[target_col].isnull().sum() > 0:
                    mean_val = features_df_clean[target_col].mean()
                    if not pd.isna(mean_val):
                        features_df_clean[target_col].fillna(mean_val, inplace=True)
                    else:
                        # 如果均值也是NaN，使用0填充
                        features_df_clean[target_col].fillna(0, inplace=True)
        
        # 4. 移除目标变量仍然为NaN的行
        clean_df = features_df_clean.dropna(subset=valid_targets)
        
        print(f"   原始数据: {original_size} 行，{len(features_df.columns)} 列")
        print(f"   清理后: {len(clean_df)} 行，{len(clean_df.columns)} 列")
        print(f"   移除了 {original_size - len(clean_df)} 行，{len(features_df.columns) - len(clean_df.columns)} 列")
        
        if len(clean_df) < 5:
            print("❌ 清理后数据太少，无法训练模型")
            return False
        
        # 训练模型
        X, y = trainer.prepare_data(clean_df, target_cols=valid_targets)
        
        print("🔄 开始训练随机森林模型...")
        trainer.train_regressor(X, y)
        
        # 保存模型
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        model_file = f"models/rf_model_{timestamp}.joblib"
        trainer.save_models(f"_{timestamp}")
        
        print(f"✅ 模型训练完成，保存到: {model_file}")
        
        # 增强版模型评估
        evaluator = ModelEvaluator()
        
        print("📊 模型性能评估:")
        
        # 评估回归模型性能
        if trainer.regressor is not None:
            from sklearn.model_selection import train_test_split
            
            # 分割数据用于评估（保持多输出格式）
            # 使用不同的随机种子以获得不同的评估结果
            import time as time_module
            random_seed = int(time_module.time() * 1000) % 10000  # 基于当前时间的随机种子
            X_train, X_test, y_train, y_test = train_test_split(
                X, y, test_size=0.2, random_state=random_seed
            )
            print(f"   🎲 使用随机种子: {random_seed} 进行数据分割")
            
            # 进行预测
            y_pred = trainer.regressor.predict(X_test)
            
            # 使用增强版评估器 - 对每个目标变量分别评估
            model_name = f"RandomForest_水声传播模型_{timestamp}"
            
            # 如果是多输出回归，对每个目标变量分别评估
            if len(y.columns) > 1:
                print(f"   📊 多输出回归评估 ({len(y.columns)} 个目标变量):")
                all_metrics = {}
                
                for i, target_name in enumerate(y.columns):
                    print(f"   🎯 评估目标变量: {target_name}")
                    y_test_single = y_test.iloc[:, i]
                    y_pred_single = y_pred[:, i]
                    
                    target_model_name = f"{model_name}_{target_name}"
                    metrics = evaluator.evaluate_regression_comprehensive(
                        y_test_single, y_pred_single, target_model_name
                    )
                    all_metrics[target_name] = metrics
                    
                    # 显示主要指标
                    print(f"      ✅ R²: {metrics['r2']:.4f}, RMSE: {metrics['rmse']:.4f}, MAE: {metrics['mae']:.4f}")
                    if 'signal_fidelity' in metrics and metrics['signal_fidelity'] > 0:
                        print(f"      🌊 信号保真度: {metrics['signal_fidelity']:.4f}")
                
                # 计算平均性能
                avg_r2 = sum(m['r2'] for m in all_metrics.values()) / len(all_metrics)
                avg_rmse = sum(m['rmse'] for m in all_metrics.values()) / len(all_metrics)
                avg_mae = sum(m['mae'] for m in all_metrics.values()) / len(all_metrics)
                
                print(f"   📈 平均性能: R²={avg_r2:.4f}, RMSE={avg_rmse:.4f}, MAE={avg_mae:.4f}")
                
                # 保存最佳目标变量的详细报告
                best_target = max(all_metrics.keys(), key=lambda k: all_metrics[k]['r2'])
                metrics = all_metrics[best_target]
                model_name = f"{model_name}_{best_target}_最佳"
                
            else:
                # 单输出回归
                metrics = evaluator.evaluate_regression_comprehensive(y_test, y_pred.flatten(), model_name)
            
            # 显示主要指标
            print(f"   ✅ R² (决定系数):        {metrics['r2']:.4f}")
            print(f"   ✅ RMSE (均方根误差):    {metrics['rmse']:.4f}")
            print(f"   ✅ MAE (平均绝对误差):   {metrics['mae']:.4f}")
            
            if 'signal_fidelity' in metrics:
                print(f"   🌊 信号保真度:          {metrics['signal_fidelity']:.4f}")
                print(f"   🌊 环境鲁棒性:          {metrics.get('environmental_robustness', 0):.4f}")
            
            # 生成并保存完整评估报告
            try:
                saved_files = evaluator.save_evaluation_artifacts(metrics, model_name)
                print(f"   📄 评估报告已保存到: {saved_files.get('report', 'N/A')}")
                print(f"   📊 评估指标已保存到: {saved_files.get('metrics', 'N/A')}")
            except Exception as e:
                print(f"   ⚠️  保存评估报告失败: {e}")
            
            # 生成改进建议
            report = evaluator.generate_comprehensive_report(metrics, model_name)
            print("\n📋 模型质量评估:")
            # 提取评级信息
            if metrics['r2'] >= 0.85:
                print("   🌟 模型质量: 优秀 (建议部署)")
            elif metrics['r2'] >= 0.70:
                print("   ⭐ 模型质量: 良好 (可以使用)")
            else:
                print("   ⚠️  模型质量: 需要改进")
        
        else:
            print("   ❌ 模型评估跳过（没有可用的回归模型）")
        
        return True
        
    except Exception as e:
        print(f"❌ 机器学习训练失败: {e}")
        return False

def get_workflow_configuration() -> Dict[str, Any]:
    """获取工作流配置选项"""
    print("\n🔧 工作流配置选项:")
    
    config = {
        'start_step': 1,
        'num_environments': 100,
        'parallel_simulation': False,
        'max_workers': None,
        'skip_existing': True
    }
    
    # 选择起始步骤
    print("\n📋 选择运行模式:")
    print("1. 完整流程 (生成环境 → 仿真 → 特征提取 → 数据库 → 训练)")
    print("2. 从仿真开始 (使用现有环境文件)")
    print("3. 从特征提取开始 (使用现有仿真结果)")
    print("4. 从数据库存储开始 (使用现有特征文件)")
    print("5. 仅机器学习训练")
    
    try:
        choice = input("请选择模式 (1-5): ").strip()
        if choice in ['1', '2', '3', '4', '5']:
            config['start_step'] = int(choice)
        else:
            print("⚠️  无效选择，使用完整流程")
            config['start_step'] = 1
        
        # 如果从步骤1开始，询问环境数量
        if config['start_step'] == 1:
            try:
                num_env = input(f"生成环境数量 (默认 {config['num_environments']}): ").strip()
                if num_env and num_env.isdigit():
                    config['num_environments'] = int(num_env)
                    if config['num_environments'] > 500:
                        print("⚠️  环境数量较大，建议不超过500个")
            except ValueError:
                pass
        
        # 如果包含仿真步骤，询问是否并行处理
        if config['start_step'] <= 2:
            try:
                parallel_choice = input("是否使用并行仿真? (y/N): ").strip().lower()
                config['parallel_simulation'] = parallel_choice in ['y', 'yes', '是']
                
                if config['parallel_simulation']:
                    max_cores = multiprocessing.cpu_count()
                    try:
                        workers = input(f"并行线程数 (1-{max_cores}, 默认4): ").strip()
                        if workers and workers.isdigit():
                            config['max_workers'] = min(int(workers), max_cores)
                        else:
                            config['max_workers'] = min(4, max_cores)
                    except ValueError:
                        config['max_workers'] = min(4, max_cores)
            except KeyboardInterrupt:
                raise
        
        # 询问是否跳过已存在的文件
        try:
            skip_choice = input("跳过已存在的中间文件? (Y/n): ").strip().lower()
            config['skip_existing'] = skip_choice not in ['n', 'no', '否']
        except KeyboardInterrupt:
            raise
            
    except KeyboardInterrupt:
        print("\n❌ 用户取消配置")
        return None
    
    return config

def main() -> bool:
    """主工作流程"""
    print("="*70)
    print("🚀 FedUWAComm 完整标准工作流")
    print("="*70)
    print("📋 流程: 环境生成 → BELLHOP仿真 → 特征提取 → 数据库存储 → 机器学习")
    print("="*70)
    
    # 环境检查
    print("\n🔍 步骤0: 环境验证")
    if not check_environment():
        print("\n💡 建议:")
        print("   1. 运行 'pip install -r requirements.txt' 安装依赖")
        print("   2. 确保BELLHOP可执行文件在data/bellhop目录下")
        print("   3. 检查系统PATH设置")
        return False
    
    # 获取工作流配置
    config = get_workflow_configuration()
    if config is None:
        return False
    
    # 显示配置摘要
    print(f"\n📋 工作流配置摘要:")
    print(f"   - 起始步骤: {config['start_step']}")
    if config['start_step'] == 1:
        print(f"   - 环境数量: {config['num_environments']}")
    if config['start_step'] <= 2 and config['parallel_simulation']:
        print(f"   - 并行仿真: 是 ({config['max_workers']} 线程)")
    print(f"   - 跳过已存在文件: {'是' if config['skip_existing'] else '否'}")
    
    try:
        input("\n按 Enter 键开始执行工作流...")
    except KeyboardInterrupt:
        print("\n❌ 用户取消操作")
        return False
    
    # 执行工作流
    start_time = time.time()
    workflow_success = True
    
    try:
        # 步骤1: 生成环境文件
        if config['start_step'] <= 1:
            print(f"\n{'='*20} 执行步骤 1/5 {'='*20}")
            if not step1_generate_bellhop_environments(config['num_environments']):
                print("❌ 步骤1失败，工作流中止")
                return False
        
        # 步骤2: BELLHOP仿真
        if config['start_step'] <= 2:
            print(f"\n{'='*20} 执行步骤 2/5 {'='*20}")
            if not step2_run_bellhop_simulation(
                parallel=config['parallel_simulation'],
                max_workers=config['max_workers']
            ):
                print("❌ 步骤2失败，工作流中止")
                return False
        
        # 步骤3: 特征提取
        csv_file = None
        if config['start_step'] <= 3:
            print(f"\n{'='*20} 执行步骤 3/5 {'='*20}")
            csv_file = step3_extract_features()
            if not csv_file:
                print("❌ 步骤3失败，工作流中止")
                return False
        
        # 步骤4: 数据库存储
        if config['start_step'] <= 4:
            print(f"\n{'='*20} 执行步骤 4/5 {'='*20}")
            if csv_file:
                if not step4_database_storage(csv_file):
                    print("❌ 步骤4失败，工作流中止")
                    return False
            else:
                # 尝试查找现有的CSV文件
                possible_csvs = [
                    "bellhop_features_extracted.csv",
                    "python-vm/bellhop_features_extracted.csv"
                ]
                found_csv = None
                for csv_path in possible_csvs:
                    if Path(csv_path).exists():
                        found_csv = csv_path
                        break
                
                if found_csv:
                    if not step4_database_storage(found_csv):
                        print("❌ 步骤4失败，工作流中止")
                        return False
                else:
                    print("❌ 未找到特征CSV文件，请先运行步骤3")
                    return False
        
        # 步骤5: 机器学习训练
        if config['start_step'] <= 5:
            print(f"\n{'='*20} 执行步骤 5/5 {'='*20}")
            if not step5_machine_learning():
                print("❌ 步骤5失败，但前面步骤已完成")
                workflow_success = False
        
        # 工作流完成
        elapsed_time = time.time() - start_time
        
        print("\n" + "="*70)
        if workflow_success:
            print("🎉 工作流执行完成!")
        else:
            print("⚠️  工作流部分完成 (某些步骤失败)")
        
        print(f"⏱️  总耗时: {elapsed_time//60:.0f}分{elapsed_time%60:.0f}秒")
        print(f"📊 执行的步骤: {config['start_step']} - 5")
        
        # 生成执行报告
        print(f"\n📋 执行摘要:")
        if config['start_step'] <= 1:
            print(f"   ✅ 环境文件生成: {config['num_environments']} 个")
        if config['start_step'] <= 2:
            print(f"   ✅ BELLHOP仿真: {'并行' if config['parallel_simulation'] else '串行'}模式")
        if config['start_step'] <= 3:
            print(f"   ✅ 特征提取: 已保存CSV文件")
        if config['start_step'] <= 4:
            print(f"   ✅ 数据库存储: 数据已导入")
        if config['start_step'] <= 5:
            print(f"   {'✅' if workflow_success else '⚠️ '} 机器学习: 模型训练{'完成' if workflow_success else '失败'}")
        
        print("="*70)
        
        return workflow_success
        
    except KeyboardInterrupt:
        elapsed_time = time.time() - start_time
        print(f"\n❌ 用户中断工作流 (已运行 {elapsed_time:.1f} 秒)")
        print("💡 中间结果已保存，可以从中断的步骤继续执行")
        return False
    except Exception as e:
        elapsed_time = time.time() - start_time
        print(f"\n❌ 工作流执行失败: {e}")
        print(f"⏱️  运行时间: {elapsed_time:.1f} 秒")
        logger.exception("详细错误信息:")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1) 
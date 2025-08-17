#!/usr/bin/env python3
"""
BELLHOP批量仿真执行器
===================
功能说明：
1. 调用BELLHOP可执行程序处理.env环境文件
2. 执行水声传播仿真计算
3. 生成仿真结果文件：.prt(打印文件)、.arr(到达时间)、.ray(射线路径)、.shd(传输损失)
4. 支持批量处理多个环境文件

输入：.env环境配置文件（由generate_environments.py生成）
输出：
- .prt文件：仿真运行日志和参数摘要
- .arr文件：声线到达时间和角度数据（ARR模式）
- .ray文件：声线传播路径数据（RAY模式）
- .shd文件：传输损失场数据（SHD模式）

工作流程：
generate_environments.py → run_bellhop_batch.py → final_database_ml.py
    (生成.env文件)         (运行BELLHOP仿真)        (特征提取和ML)
"""

import os
import subprocess
from pathlib import Path
import logging

# 设置日志记录
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def run_bellhop_simulation(env_file_path):
    """
    运行单个BELLHOP仿真
    
    这是核心函数，负责调用BELLHOP可执行程序处理单个环境文件
    
    参数:
        env_file_path: .env环境文件的路径
        
    返回:
        bool: 仿真是否成功
        
    BELLHOP工作原理：
    1. 读取.env文件中的环境参数（声速剖面、频率、几何配置等）
    2. 根据模式执行不同的计算：
       - ARR模式：计算声线到达时间和角度
       - RAY模式：计算声线传播路径
       - SHD模式：计算传输损失场
    3. 输出对应的结果文件
    """
    # 获取脚本的路径
    script_dir = Path(__file__).resolve().parent
    # 计算python-vm的路径
    python_vm_dir = script_dir.parent
    # 设置BELLHOP工作目录为python-vm/data/bellhop
    bellhop_dir = python_vm_dir / "data" / "bellhop"
    
    logger.info(f"使用BELLHOP目录: {bellhop_dir}")
    env_file = Path(env_file_path)
    
    # 切换到BELLHOP工作目录
    # 这是必需的，因为BELLHOP在当前目录寻找输入文件并输出结果文件
    original_dir = os.getcwd()
    os.chdir(bellhop_dir)
    
    try:
        # 准备BELLHOP命令
        # BELLHOP使用文件名（不包含.env扩展名）作为参数
        env_name = env_file.stem  # 例如：B001_arr.env -> B001_arr
        cmd = ["./bellhop.exe", env_name]
        
        logger.info(f"运行BELLHOP命令: {' '.join(cmd)}")
        
        # 执行BELLHOP仿真
        # capture_output=True: 捕获标准输出和错误
        # text=True: 以文本模式处理输出
        # timeout=30: 设置30秒超时，防止程序卡死
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
        
        # 检查仿真结果
        if result.returncode == 0:
            logger.info(f"✅ 仿真成功: {env_name}")
            
            # 检查生成的输出文件
            # 根据.env文件的模式，应该生成对应的输出文件
            if env_name.endswith('_arr'):
                expected_files = [f"{env_name}.arr", f"{env_name}.prt"]
            elif env_name.endswith('_ray'):
                expected_files = [f"{env_name}.ray", f"{env_name}.prt"]
            elif env_name.endswith('_shd'):
                expected_files = [f"{env_name}.shd", f"{env_name}.prt"]
            else:
                expected_files = [f"{env_name}.prt"]  # 至少应该有prt文件
            
            # 验证输出文件是否存在
            missing_files = []
            for expected_file in expected_files:
                if not Path(expected_file).exists():
                    missing_files.append(expected_file)
            
            if missing_files:
                logger.warning(f"警告: 预期的输出文件缺失: {missing_files}")
            else:
                logger.info(f"所有预期输出文件已生成: {expected_files}")
            
            return True
        else:
            logger.error(f"❌ 仿真失败: {env_name}")
            logger.error(f"错误信息: {result.stderr}")
            logger.error(f"标准输出: {result.stdout}")
            return False
            
    except subprocess.TimeoutExpired:
        logger.error(f"❌ 仿真超时: {env_name}")
        logger.error("可能原因：环境参数配置错误或计算复杂度过高")
        return False
    except Exception as e:
        logger.error(f"❌ 仿真异常: {env_name} - {e}")
        return False
    finally:
        # 无论成功失败，都要切换回原目录
        os.chdir(original_dir)

def main():
    """
    主函数：批量执行BELLHOP仿真
    
    功能：
    1. 扫描data/bellhop目录下的所有B*.env文件
    2. 依次调用BELLHOP处理每个环境文件
    3. 统计成功和失败的仿真数量
    4. 列出生成的输出文件
    
    注意：现在处理所有找到的环境文件
    """
    bellhop_dir = Path("data/bellhop")
    
    # 查找所有以B开头的.env文件
    # 这些文件由generate_environments.py生成
    env_files = list(bellhop_dir.glob("B*.env"))
    
    logger.info(f"发现 {len(env_files)} 个环境文件")
    
    if len(env_files) == 0:
        logger.error("错误：没有找到环境文件")
        logger.error("请先运行 python tools/generate_environments.py 生成环境文件")
        return
    
    # 统计变量
    successes = 0  # 成功计数
    failures = 0   # 失败计数
    
    # # 测试模式：只处理前6个文件
    # # 这样可以快速验证BELLHOP是否正常工作
    # # 6个文件包含：B001_arr, B001_ray, B001_shd, B002_arr, B002_ray, B002_shd
    # test_files = env_files[:6]
    
    # logger.info(f"测试模式：处理前 {len(test_files)} 个文件")
    # logger.info("如需处理所有文件，请修改代码中的test_files设置")

    # 处理所有环境文件
    logger.info(f"开始批量处理所有 {len(env_files)} 个环境文件")
    
    # 逐个处理环境文件
    # for i, env_file in enumerate(test_files, 1):
    #     logger.info(f"\n=== 处理第 {i}/{len(test_files)} 个文件: {env_file.name} ===")
    for i, env_file in enumerate(env_files, 1):
        logger.info(f"\n=== 处理第 {i}/{len(env_files)} 个文件: {env_file.name} ===")
        
        if run_bellhop_simulation(env_file):
            successes += 1
        else:
            failures += 1
    
    # 输出仿真结果统计
    logger.info(f"\n=== 仿真完成 ===")
    logger.info(f"结果统计: {successes} 成功, {failures} 失败")
    
    # 如果有成功的仿真，列出生成的输出文件
    if successes > 0:
        logger.info("检查生成的输出文件...")
        
        # 查找所有BELLHOP输出文件
        output_files = []
        output_files.extend(list(bellhop_dir.glob("B*.arr")))  # 到达时间文件
        output_files.extend(list(bellhop_dir.glob("B*.ray")))  # 射线路径文件
        output_files.extend(list(bellhop_dir.glob("B*.shd")))  # 传输损失文件
        output_files.extend(list(bellhop_dir.glob("B*.prt")))  # 打印输出文件
        
        logger.info(f"共生成 {len(output_files)} 个输出文件:")
        
        # 按类型分组显示
        file_types = {'.arr': '到达时间文件', '.ray': '射线路径文件', 
                     '.shd': '传输损失文件', '.prt': '运行日志文件'}
        
        for ext, description in file_types.items():
            type_files = [f for f in output_files if f.suffix == ext]
            if type_files:
                logger.info(f"  {description} ({len(type_files)}个):")
                for f in sorted(type_files):
                    logger.info(f"    - {f.name}")
        
        logger.info(f"\n✅ BELLHOP仿真执行完成！")
        logger.info(f"输出文件位置: {bellhop_dir.absolute()}")
        logger.info(f"下一步：运行 python final_database_ml.py 进行特征提取和机器学习")
    else:
        logger.error(f"\n❌ 所有仿真都失败了")
        logger.error("可能的原因：")
        logger.error("1. BELLHOP可执行文件不存在或无权限")
        logger.error("2. 环境文件格式错误")
        logger.error("3. 系统环境问题")
        logger.error("请检查 data/bellhop/ 目录下是否有 bellhop.exe 文件")

if __name__ == "__main__":
    main()
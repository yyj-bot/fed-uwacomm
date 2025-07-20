import os
import subprocess

DATA_DIR = 'AllBellhopData'
EXE_NAME = 'bellhopf.exe'

def run_bellhop_on_env(env_path):
    main_name = os.path.splitext(os.path.basename(env_path))[0]
    exe_path = os.path.join(DATA_DIR, EXE_NAME)
    cwd = DATA_DIR
    # 只用主名，不加.env后缀
    try:
        result = subprocess.run([exe_path, main_name], cwd=cwd, capture_output=True, text=True, timeout=120)
        # 检查是否生成.prt文件
        prt_file = os.path.join(DATA_DIR, f'{main_name}.prt')
        if os.path.exists(prt_file):
            with open(prt_file, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            if 'FATAL ERROR' in content:
                print(f'{main_name}: 仿真失败（FATAL ERROR）')
            else:
                print(f'{main_name}: 仿真成功')
        else:
            print(f'{main_name}: 未生成输出文件，仿真失败')
    except Exception as e:
        print(f'{main_name}: 仿真运行出错: {e}')

if __name__ == '__main__':
    env_files = [f for f in os.listdir(DATA_DIR) if f.endswith('.env')]
    if not env_files:
        print('未找到.env文件！')
        exit(1)
    for env in env_files:
        run_bellhop_on_env(os.path.join(DATA_DIR, env))
    print('批量仿真已完成！') 
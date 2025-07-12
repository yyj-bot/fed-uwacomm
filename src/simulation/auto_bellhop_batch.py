import os
import shutil
import subprocess
import time

# 官方Bellhop样例模板（用户手动粘贴内容）
official_template = [
    "'Munk profile, coherent'\t! TITLE",
    "50.0\t\t\t\t\t! FREQ (Hz)",
    "1\t\t\t\t\t! NMEDIA",
    "'CVW'\t\t\t\t\t! SSPOPT (Analytic or C-linear interpolation)",
    "51  0.0  5000.0\t\t\t! DEPTH of bottom (m)",
    # 声速剖面插入点
    "'A' 0.0",
    " 5000.0  1600.00 0.0 1.8 0.8 /",
    "1\t\t\t\t\t! NSD",
    "4700.0 /\t\t\t\t! SD(1:NSD) (m)",
    "1\t\t\t\t\t! NRD",
    "4700 /\t\t\t! RD(1:NRD) (m)",
    "21\t\t\t\t\t! NR",
    "0.0  15.0 /\t\t\t! R(1:NR ) (km)",
    "'A'\t  \t\t\t\t! 'R/C/I/S'",
    "1000\t\t\t\t\t! NBEAMS",
    "-80 80 / -20.3 20.3 /\t        ! ALPHA1, 2 (degrees)",
    "0.0  5500.0  101.0\t! STEP (m), ZBOX (m), RBOX (km)"
]

def extract_profile(envfile):
    """提取B*_bellhop.env文件中的声速剖面部分"""
    lines = []
    with open(envfile, 'r', encoding='utf-8') as f:
        for line in f:
            if line.strip() and '/' in line and len(line.split()) >= 2:
                parts = line.strip().replace('/', '').split()
                try:
                    d, c = float(parts[0]), float(parts[1])
                    lines.append(f"  {d:.2f}  {c:.2f}  /")
                except Exception:
                    continue
    return lines

# 1. 查找当前目录下所有 *_bellhop.env 文件
env_files = [f for f in os.listdir('.') if f.endswith('_bellhop.env')]
if not env_files:
    print('当前目录下未找到 *_bellhop.env 文件！')
    exit(1)

# 2. 生成严格官方格式env文件
gen_envs = []
for env in env_files:
    profile_lines = extract_profile(env)
    if not profile_lines:
        print(f'未能解析 {env} 的剖面数据，跳过。')
        continue
    outname = env.replace('_bellhop.env', '_strict.env')
    with open(outname, 'w', encoding='utf-8') as fout:
        for line in official_template[:5]:
            fout.write(line + '\n')
        for pl in profile_lines:
            fout.write(pl + '\n')
        for line in official_template[5:]:
            fout.write(line + '\n')
    gen_envs.append(outname)
    print(f'已生成严格官方格式: {outname}')

# 3. 在本地目录下批量运行bellhopf.exe
def run_bellhopf_on_envs(envs):
    exe_name = 'bellhopf.exe'
    if not os.path.exists(exe_name):
        print('当前目录下未找到 bellhopf.exe！')
        return
    output_types = ['.shd', '.arr', '.txt', '.prt']
    results = {}
    for env in envs:
        main_name = os.path.splitext(env)[0]
        print(f'处理: {env} (主名: {main_name})')
        # 清理旧输出
        for ext in output_types:
            try:
                os.remove(f'{main_name}{ext}')
            except Exception:
                pass
        try:
            result = subprocess.run([f'./{exe_name}', main_name], capture_output=True, text=True, timeout=120)
            out_files = [f'{main_name}{ext}' for ext in output_types if os.path.exists(f'{main_name}{ext}')]
            if out_files:
                results[env] = {'status': '成功', 'outputs': out_files}
                print(f'成功生成: {out_files}')
            else:
                results[env] = {'status': '失败', 'outputs': []}
                print(f'{env} 处理失败！')
        except Exception as e:
            results[env] = {'status': '失败', 'outputs': []}
            print(f'{env} 运行出错: {e}')
    print('\n=== 处理结果汇总 ===')
    for env, info in results.items():
        print(f'{env}: {info["status"]} 输出文件: {info["outputs"]}')
    if all(info['status'] == '成功' for info in results.values()):
        print('\n所有env文件均已成功处理！')
    else:
        print('\n部分env文件处理失败，请检查上方日志。')

run_bellhopf_on_envs(gen_envs) 
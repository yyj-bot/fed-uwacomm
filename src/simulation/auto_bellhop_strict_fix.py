import os
import shutil

def fix_env_depths(env_path, out_path):
    """修正env文件中剖面深度递增问题，输出到新文件"""
    with open(env_path, 'r', encoding='utf-8') as fin:
        lines = fin.readlines()
    # 找到剖面起止行
    start, end = None, None
    for i, line in enumerate(lines):
        if any(x in line for x in ['DEPTH of bottom', 'DEPTH  of bottom']):
            start = i + 1
        if start is not None and ("'A'" in line or 'A ' in line):
            end = i
            break
    if start is None or end is None or end <= start:
        print(f"{env_path} 未找到剖面区间，跳过")
        shutil.copy(env_path, out_path)
        return
    # 解析剖面并去重、排序
    profile = []
    for line in lines[start:end]:
        parts = line.strip().replace('/', '').split()
        if len(parts) >= 2:
            try:
                d, c = float(parts[0]), float(parts[1])
                profile.append((d, c))
            except Exception:
                continue
    # 去重并严格递增
    profile = sorted(set(profile), key=lambda x: x[0])
    profile = [p for i, p in enumerate(profile) if i == 0 or p[0] > profile[i-1][0]]
    # 重写文件
    with open(out_path, 'w', encoding='utf-8') as fout:
        fout.writelines(lines[:start])
        for d, c in profile:
            fout.write(f"  {d:.2f}  {c:.2f}  /\n")
        fout.writelines(lines[end:])
    print(f"已修正: {out_path}")

# 1. 创建统一目录
outdir = 'AllBellhopData'
os.makedirs(outdir, exist_ok=True)

# 2. 修正所有 *_strict.env 文件并移动
for fname in os.listdir('.'):
    if fname.endswith('_strict.env'):
        fix_env_depths(fname, os.path.join(outdir, fname))

# 3. 移动所有 _strict.prt 文件
for fname in os.listdir('.'):
    if fname.endswith('_strict.prt'):
        shutil.move(fname, os.path.join(outdir, fname))

# 4. 移动 bellhopf.exe
if os.path.exists('bellhopf.exe'):
    shutil.copy('bellhopf.exe', os.path.join(outdir, 'bellhopf.exe'))

print('所有env、prt、exe文件已统一放入 AllBellhopData 目录。') 
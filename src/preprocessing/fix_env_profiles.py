import os
import shutil

def fix_env_file(env_path):
    with open(env_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    new_lines = []
    in_profile = False
    profile_lines = []
    for i, line in enumerate(lines):
        # 剖面区间起点：出现DEPTH of bottom后，下一行为剖面起点
        if any(x in line for x in ['DEPTH of bottom', 'DEPTH  of bottom']):
            new_lines.append(line)
            in_profile = True
            continue
        # 剖面区间终止：遇到'A' 0.0或其它参数行
        if in_profile and ("'A'" in line or 'A ' in line):
            # 处理剖面区间
            # 只保留严格递增的深度-声速对
            last_depth = None
            for pl in profile_lines:
                parts = pl.strip().replace('/', '').split()
                if len(parts) >= 2:
                    try:
                        d, c = float(parts[0]), float(parts[1])
                        if last_depth is None or d > last_depth:
                            new_lines.append(f"  {d:.2f}  {c:.2f}  /\n")
                            last_depth = d
                    except Exception:
                        continue
            profile_lines = []
            in_profile = False
            new_lines.append("'A' 0.0\n")
            # 剩余行继续正常处理
        if in_profile:
            # 只收集可能的剖面点
            parts = line.strip().replace('/', '').split()
            if len(parts) >= 2:
                try:
                    float(parts[0]); float(parts[1])
                    profile_lines.append(line)
                except Exception:
                    pass
            continue
        # 非剖面区间直接写入
        new_lines.append(line)
    # 若文件结尾仍在剖面区间，补写一次
    if in_profile and profile_lines:
        last_depth = None
        for pl in profile_lines:
            parts = pl.strip().replace('/', '').split()
            if len(parts) >= 2:
                try:
                    d, c = float(parts[0]), float(parts[1])
                    if last_depth is None or d > last_depth:
                        new_lines.append(f"  {d:.2f}  {c:.2f}  /\n")
                        last_depth = d
                except Exception:
                    continue
        new_lines.append("'A' 0.0\n")
    # 覆盖写回原文件
    with open(env_path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
    print(f"已修正: {env_path}")

# 批量修正AllBellhopData目录下所有.env文件
if __name__ == '__main__':
    env_dir = 'AllBellhopData'
    for fname in os.listdir(env_dir):
        if fname.endswith('.env'):
            fix_env_file(os.path.join(env_dir, fname))
    print('所有env文件剖面区间和格式已自动修正！') 
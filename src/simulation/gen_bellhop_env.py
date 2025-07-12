import os

# B1~B15声速剖面数据文件路径
ssp_file = '../Underwater-Target-Recognition/TypicalSSP/3RangeDependentSSP/B1~B15声速剖面.ssp'
output_dir = '.'  # 当前目录即dachuang

# 仿真参数
frequency = 1000  # Hz
src_depth = 50    # m
rcv_depth = 50    # m
range_start = 0   # m
range_end = 10000 # m
range_num = 101   # 点数
bottom = '2000 1500 1.0 0.0'  # 底质参数（厚度、声速、密度、衰减）
sim_type = 'R'  # Ray tracing

# 读取ssp文件
with open(ssp_file, 'r', encoding='utf-8') as f:
    lines = [line.strip() for line in f if line.strip()]

num_profiles = int(lines[0].split()[0])
depths = list(map(float, lines[1].split()))
# 每一列为一个剖面
profiles = []
for i in range(2, 2 + len(depths)):
    row = list(map(float, lines[i].split()))
    profiles.append(row)
# 转置，得到每个剖面
import numpy as np
profiles = np.array(profiles).T  # shape: (15, Ndepth)

for idx in range(num_profiles):
    env_name = f'B{idx+1}.env'
    with open(os.path.join(output_dir, env_name), 'w', encoding='utf-8') as fout:
        fout.write(f"'BELLHOP仿真输入文件 B{idx+1}\n")
        fout.write(f"{frequency}       {src_depth}           {rcv_depth}              {range_start} {range_end} {range_num}\n")
        fout.write("'深度(m) 声速(m/s)'\n")
        for d, c in zip(depths, profiles[idx]):
            fout.write(f"{d:.4f} {c:.6f}\n")
        fout.write("'底质参数'\n")
        fout.write(bottom + '\n')
        fout.write("'仿真类型'\n")
        fout.write(sim_type + '\n')
print('已生成15个BELLHOP .env文件于dachuang目录下') 
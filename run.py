#!/usr/bin/env python3
"""
FedUWAComm项目主入口脚本
提供命令行界面，方便用户运行项目的各个组件
"""

import os
import argparse
import sys

# 添加src目录到Python路径
sys.path.append(os.path.join(os.path.dirname(__file__), 'src'))

def setup_parser():
    """设置命令行参数解析器"""
    parser = argparse.ArgumentParser(
        description='FedUWAComm - 基于联邦学习的水声通信优化与隐私保护系统软件',
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    
    # 创建子命令解析器
    subparsers = parser.add_subparsers(dest='component', help='要运行的组件')
    
    # 预处理组件
    preproc_parser = subparsers.add_parser('preprocess', help='数据预处理组件')
    preproc_parser.add_argument(
        '--action',
        choices=['generate', 'fix', 'extract', 'workflow'],
        default='workflow',
        help='预处理操作类型'
    )
    preproc_parser.add_argument('--dir', default='data/bellhop', help='输入/输出目录')
    preproc_parser.add_argument('--num', type=int, default=15, help='生成文件数量')
    
    # 训练组件（预留）
    train_parser = subparsers.add_parser('train', help='模型训练组件')
    train_parser.add_argument('--data', default='data/bellhop/bellhop_features.csv', help='训练数据路径')
    train_parser.add_argument('--output', default='models', help='模型输出目录')
    
    # 优化组件（预留）
    optim_parser = subparsers.add_parser('optimize', help='通信优化组件')
    optim_parser.add_argument('--model', default='models/rf_model.pkl', help='预训练模型路径')
    optim_parser.add_argument('--output', default='results', help='优化结果输出目录')
    
    return parser

def run_preprocessing(args):
    """运行预处理组件"""
    from preprocessing.bellhop_tools import main as bellhop_main
    
    # 设置sys.argv以匹配预处理脚本的参数格式
    sys.argv = [
        'unified_bellhop_tools.py',
        f'--action={args.action}',
        f'--dir={args.dir}',
        f'--num={args.num}'
    ]
    
    # 运行预处理脚本
    bellhop_main()

def run_training(args):
    """运行训练组件（预留）"""
    print("模型训练组件尚未实现")
    print(f"将使用数据: {args.data}")
    print(f"模型将保存至: {args.output}")
    # TODO: 实现随机森林模型训练

def run_optimization(args):
    """运行通信优化组件（预留）"""
    print("通信优化组件尚未实现")
    print(f"将使用模型: {args.model}")
    print(f"优化结果将保存至: {args.output}")
    # TODO: 实现通信参数优化

def main():
    """主函数"""
    parser = setup_parser()
    args = parser.parse_args()
    
    if args.component == 'preprocess':
        run_preprocessing(args)
    elif args.component == 'train':
        run_training(args)
    elif args.component == 'optimize':
        run_optimization(args)
    else:
        parser.print_help()

if __name__ == '__main__':
    main() 
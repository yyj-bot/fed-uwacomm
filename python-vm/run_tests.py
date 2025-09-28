#!/usr/bin/env python3
"""
测试运行脚本
用于执行python-vm的各种测试
"""

import sys
import os
import subprocess
import argparse
from pathlib import Path

# 添加src到Python路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root / 'src'))


def run_command(cmd, description):
    """运行命令并显示结果"""
    print(f"\n{'='*60}")
    print(f"执行: {description}")
    print(f"命令: {' '.join(cmd)}")
    print('='*60)
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, cwd=project_root)
        
        if result.stdout:
            print("标准输出:")
            print(result.stdout)
        
        if result.stderr:
            print("错误输出:")
            print(result.stderr)
        
        if result.returncode == 0:
            print(f"✅ {description} - 成功")
        else:
            print(f"❌ {description} - 失败 (退出码: {result.returncode})")
        
        return result.returncode == 0
        
    except Exception as e:
        print(f"❌ {description} - 执行失败: {e}")
        return False


def check_dependencies():
    """检查测试依赖"""
    print("检查测试依赖...")
    
    required_packages = [
        'pytest', 'numpy', 'pandas', 'sklearn', 
        'websockets', 'psutil', 'joblib'
    ]
    
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package)
            print(f"✅ {package}")
        except ImportError:
            print(f"❌ {package} - 未安装")
            missing_packages.append(package)
    
    if missing_packages:
        print(f"\n缺少依赖包: {', '.join(missing_packages)}")
        print("请运行: pip install " + ' '.join(missing_packages))
        return False
    
    return True


def run_unit_tests():
    """运行单元测试"""
    cmd = [sys.executable, '-m', 'pytest', 'tests/unit/', '-v', '--tb=short']
    return run_command(cmd, "单元测试")


def run_integration_tests():
    """运行集成测试"""
    cmd = [sys.executable, '-m', 'pytest', 'tests/integration/', '-v', '--tb=short']
    return run_command(cmd, "集成测试")


def run_e2e_tests():
    """运行端到端测试"""
    cmd = [sys.executable, '-m', 'pytest', 'tests/e2e/', '-v', '--tb=short']
    return run_command(cmd, "端到端测试")


def run_performance_tests():
    """运行性能测试"""
    cmd = [sys.executable, '-m', 'pytest', 'tests/performance/', '-v', '--tb=short', '-m', 'slow']
    return run_command(cmd, "性能测试")


def run_all_tests():
    """运行所有测试"""
    cmd = [sys.executable, '-m', 'pytest', 'tests/', '-v', '--tb=short']
    return run_command(cmd, "所有测试")


def run_coverage_tests():
    """运行覆盖率测试"""
    cmd = [
        sys.executable, '-m', 'pytest', 'tests/', 
        '--cov=src/feduwacomm', 
        '--cov-report=html', 
        '--cov-report=term-missing'
    ]
    return run_command(cmd, "覆盖率测试")


def lint_code():
    """代码检查"""
    print("\n执行代码检查...")
    
    # 检查Python语法
    cmd = [sys.executable, '-m', 'py_compile'] + list(Path('src').rglob('*.py'))
    success = run_command(cmd[:3] + [str(cmd[3])], "Python语法检查")
    
    return success


def validate_test_structure():
    """验证测试结构"""
    print("\n验证测试结构...")
    
    required_dirs = [
        'tests/unit',
        'tests/integration', 
        'tests/e2e',
        'tests/performance'
    ]
    
    required_files = [
        'tests/__init__.py',
        'tests/conftest.py',
        'pytest.ini'
    ]
    
    all_exist = True
    
    for dir_path in required_dirs:
        if Path(dir_path).exists():
            print(f"✅ {dir_path}")
        else:
            print(f"❌ {dir_path} - 不存在")
            all_exist = False
    
    for file_path in required_files:
        if Path(file_path).exists():
            print(f"✅ {file_path}")
        else:
            print(f"❌ {file_path} - 不存在")
            all_exist = False
    
    return all_exist


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='Python VM 测试运行器')
    parser.add_argument('--unit', action='store_true', help='运行单元测试')
    parser.add_argument('--integration', action='store_true', help='运行集成测试')
    parser.add_argument('--e2e', action='store_true', help='运行端到端测试')
    parser.add_argument('--performance', action='store_true', help='运行性能测试')
    parser.add_argument('--coverage', action='store_true', help='运行覆盖率测试')
    parser.add_argument('--lint', action='store_true', help='运行代码检查')
    parser.add_argument('--all', action='store_true', help='运行所有测试')
    parser.add_argument('--check', action='store_true', help='检查测试环境')
    
    args = parser.parse_args()
    
    print("Python VM 测试运行器")
    print("="*60)
    
    # 检查测试环境
    if args.check or not any(vars(args).values()):
        print("\n🔍 检查测试环境...")
        
        if not validate_test_structure():
            print("❌ 测试结构验证失败")
            return 1
        
        if not check_dependencies():
            print("❌ 依赖检查失败")
            return 1
        
        print("✅ 测试环境检查通过")
        
        if not any(vars(args).values()):
            print("\n使用 --help 查看可用选项")
            return 0
    
    success_count = 0
    total_count = 0
    
    # 运行指定的测试
    if args.lint:
        total_count += 1
        if lint_code():
            success_count += 1
    
    if args.unit:
        total_count += 1
        if run_unit_tests():
            success_count += 1
    
    if args.integration:
        total_count += 1
        if run_integration_tests():
            success_count += 1
    
    if args.e2e:
        total_count += 1
        if run_e2e_tests():
            success_count += 1
    
    if args.performance:
        total_count += 1
        if run_performance_tests():
            success_count += 1
    
    if args.coverage:
        total_count += 1
        if run_coverage_tests():
            success_count += 1
    
    if args.all:
        total_count += 1
        if run_all_tests():
            success_count += 1
    
    # 显示总结
    if total_count > 0:
        print(f"\n{'='*60}")
        print("测试总结")
        print('='*60)
        print(f"总计: {total_count}")
        print(f"成功: {success_count}")
        print(f"失败: {total_count - success_count}")
        
        if success_count == total_count:
            print("🎉 所有测试都通过了!")
            return 0
        else:
            print("⚠️  有测试失败")
            return 1
    
    return 0


if __name__ == '__main__':
    sys.exit(main())

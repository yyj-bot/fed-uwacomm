"""
FedUWAComm 项目安装配置
"""

from setuptools import setup, find_packages
from pathlib import Path

# 读取README文件
this_directory = Path(__file__).parent
long_description = (this_directory / "README.md").read_text(encoding='utf-8')

# 读取requirements文件
requirements = []
if (this_directory / "requirements.txt").exists():
    requirements = (this_directory / "requirements.txt").read_text().strip().split('\n')

setup(
    name="feduwacomm",
    version="1.0.0",
    author="FedUWAComm Team",
    author_email="contact@feduwacomm.org",
    description="联邦学习水声通信优化系统",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/yourusername/FedUWAComm",
    package_dir={"": "src"},
    packages=find_packages(where="src"),
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Science/Research",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Topic :: Scientific/Engineering :: Artificial Intelligence",
    ],
    python_requires=">=3.8",
    install_requires=requirements,
    extras_require={
        "dev": [
            "pytest>=6.0",
            "pytest-cov>=2.0",
            "black>=21.0",
            "flake8>=3.8",
            "mypy>=0.812",
        ]
    },
    entry_points={
        "console_scripts": [
            "feduwacomm-ml=scripts.final_database_ml:main",
            "feduwacomm-verify=scripts.verify_database:main",
        ]
    },
    include_package_data=True,
    zip_safe=False,
) 
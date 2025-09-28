# FedUWA Python VM 部署指南

本文档提供了FedUWA Python VM的完整部署指南，包括安装、配置、监控和维护。

## 目录

- [系统要求](#系统要求)
- [快速安装](#快速安装)
- [手动安装](#手动安装)
- [配置管理](#配置管理)
- [服务管理](#服务管理)
- [监控和日志](#监控和日志)
- [故障排除](#故障排除)
- [卸载](#卸载)

## 系统要求

### 最低要求
- **操作系统**: Linux (Ubuntu 18.04+, CentOS 7+) 或 macOS 10.15+
- **Python**: 3.8或更高版本
- **内存**: 2GB RAM
- **磁盘空间**: 5GB可用空间
- **网络**: 稳定的互联网连接

### 推荐配置
- **操作系统**: Ubuntu 20.04 LTS
- **Python**: 3.9+
- **内存**: 4GB RAM
- **磁盘空间**: 20GB可用空间
- **CPU**: 2核心或更多

### 依赖软件
- `python3` 和 `pip3`
- `git` (可选，用于从源码安装)
- `systemd` (Linux系统服务管理)

## 快速安装

### 1. 下载安装脚本

```bash
# 下载项目
git clone https://github.com/your-org/fed-uwacomm.git
cd fed-uwacomm/python-vm

# 或者直接下载安装脚本
curl -O https://raw.githubusercontent.com/your-org/fed-uwacomm/main/python-vm/scripts/install.sh
```

### 2. 运行安装脚本

```bash
# 标准安装
chmod +x scripts/install.sh
./scripts/install.sh

# 跳过测试的快速安装
./scripts/install.sh --skip-tests

# 仅安装不启动服务
./scripts/install.sh --skip-start
```

### 3. 验证安装

```bash
# 检查服务状态
sudo systemctl status feduwa-vm.service

# 运行健康检查
/opt/feduwa/python-vm/scripts/health_check.py
```

## 手动安装

如果自动安装脚本不适用，可以按照以下步骤手动安装。

### 1. 创建用户和目录

```bash
# 创建系统用户
sudo useradd -r -s /bin/bash -d /opt/feduwa -m feduwa

# 创建目录结构
sudo mkdir -p /opt/feduwa/python-vm
sudo mkdir -p /etc/feduwa
sudo mkdir -p /var/log/feduwa
sudo mkdir -p /var/run

# 设置权限
sudo chown -R feduwa:feduwa /opt/feduwa
sudo chown -R feduwa:feduwa /var/log/feduwa
```

### 2. 安装Python依赖

```bash
# 创建虚拟环境
sudo -u feduwa python3 -m venv /opt/feduwa/python-vm/venv

# 激活虚拟环境并安装依赖
sudo -u feduwa bash -c "
    source /opt/feduwa/python-vm/venv/bin/activate
    pip install --upgrade pip
    pip install websockets numpy scikit-learn psutil pyyaml pytest
"
```

### 3. 复制项目文件

```bash
# 复制源代码
sudo cp -r src/ /opt/feduwa/python-vm/
sudo cp -r scripts/ /opt/feduwa/python-vm/
sudo cp -r tests/ /opt/feduwa/python-vm/

# 设置权限
sudo chown -R feduwa:feduwa /opt/feduwa/python-vm
sudo chmod +x /opt/feduwa/python-vm/scripts/*.py
```

### 4. 安装配置文件

```bash
# 复制配置模板
sudo cp config/base.yaml /etc/feduwa/
sudo cp config/prod.yaml /etc/feduwa/

# 设置权限
sudo chown -R feduwa:feduwa /etc/feduwa
sudo chmod 640 /etc/feduwa/*.yaml
```

### 5. 安装systemd服务

```bash
# 复制服务文件
sudo cp scripts/feduwa-vm.service /etc/systemd/system/

# 重新加载systemd并启用服务
sudo systemctl daemon-reload
sudo systemctl enable feduwa-vm.service
```

## 配置管理

### 配置文件结构

```
/etc/feduwa/
├── base.yaml          # 基础配置
├── prod.yaml          # 生产环境配置
├── dev.yaml           # 开发环境配置（可选）
└── modules/           # 模块特定配置（可选）
    ├── websocket.yaml
    └── federated.yaml
```

### 主要配置项

#### WebSocket配置
```yaml
websocket:
  server_url: "ws://your-server:8080/websocket"
  connection_timeout: 30
  heartbeat_interval: 30
  max_reconnect_attempts: 5
  reconnect_delay: 5
```

#### 联邦学习配置
```yaml
federated:
  supported_algorithms:
    - "FEDERATED_AVERAGING"
    - "FEDERATED_PROXIMAL"
    - "FEDERATED_NOVA"
    - "SCAFFOLD"
  max_concurrent_tasks: 3
  default_local_epochs: 5
  default_batch_size: 32
```

#### 日志配置
```yaml
logging:
  level: "INFO"
  file:
    enabled: true
    path: "/var/log/feduwa/feduwa-vm.log"
    max_size: "10MB"
    backup_count: 5
  console:
    enabled: true
    level: "INFO"
```

### 环境变量

可以通过环境变量覆盖配置：

```bash
# 设置环境
export FEDUWA_ENV=prod

# 覆盖配置
export FEDUWA_CONFIG_WEBSOCKET_SERVER_URL="ws://new-server:8080/websocket"
export FEDUWA_CONFIG_LOGGING_LEVEL="DEBUG"

# VM标识
export FEDUWA_VM_ID="vm-production-001"
```

## 服务管理

### systemd命令

```bash
# 启动服务
sudo systemctl start feduwa-vm.service

# 停止服务
sudo systemctl stop feduwa-vm.service

# 重启服务
sudo systemctl restart feduwa-vm.service

# 查看状态
sudo systemctl status feduwa-vm.service

# 查看日志
sudo journalctl -u feduwa-vm.service -f

# 启用开机自启
sudo systemctl enable feduwa-vm.service

# 禁用开机自启
sudo systemctl disable feduwa-vm.service
```

### 手动启动

```bash
# 直接启动（前台运行）
sudo -u feduwa /opt/feduwa/python-vm/scripts/start_vm.py start

# 后台启动
sudo -u feduwa /opt/feduwa/python-vm/scripts/start_vm.py start --daemon

# 停止服务
sudo -u feduwa /opt/feduwa/python-vm/scripts/start_vm.py stop

# 查看状态
sudo -u feduwa /opt/feduwa/python-vm/scripts/start_vm.py status
```

## 监控和日志

### 健康检查

```bash
# 完整健康检查
/opt/feduwa/python-vm/scripts/health_check.py

# 指定检查项
/opt/feduwa/python-vm/scripts/health_check.py --checks process resources

# JSON输出
/opt/feduwa/python-vm/scripts/health_check.py --output json

# 详细输出
/opt/feduwa/python-vm/scripts/health_check.py --verbose
```

### 日志文件

```bash
# 应用日志
tail -f /var/log/feduwa/feduwa-vm.log

# 系统日志
sudo journalctl -u feduwa-vm.service -f

# 健康检查日志
tail -f /var/log/feduwa/health-check.log
```

### 监控指标

服务提供以下监控指标：

- **系统资源**: CPU、内存、磁盘使用率
- **网络状态**: 连接状态、延迟
- **任务状态**: 活跃任务数、完成率
- **错误统计**: 错误类型和频率

### 性能调优

#### 系统级优化

```bash
# 增加文件描述符限制
echo "feduwa soft nofile 65536" | sudo tee -a /etc/security/limits.conf
echo "feduwa hard nofile 65536" | sudo tee -a /etc/security/limits.conf

# 优化网络参数
echo "net.core.rmem_max = 16777216" | sudo tee -a /etc/sysctl.conf
echo "net.core.wmem_max = 16777216" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

#### 应用级优化

在配置文件中调整性能参数：

```yaml
performance:
  optimization:
    thread_pool_size: 4
    async_processing: true
    cache_size: 100
  monitoring:
    enabled: true
    interval: 60
```

## 故障排除

### 常见问题

#### 1. 服务启动失败

```bash
# 查看详细错误信息
sudo journalctl -u feduwa-vm.service --no-pager

# 检查配置文件
/opt/feduwa/python-vm/scripts/health_check.py --checks config

# 检查权限
ls -la /opt/feduwa/python-vm/
ls -la /etc/feduwa/
```

#### 2. 连接问题

```bash
# 测试网络连通性
ping your-server-hostname

# 检查WebSocket连接
/opt/feduwa/python-vm/scripts/health_check.py --checks websocket

# 查看网络配置
cat /etc/feduwa/base.yaml | grep -A 10 websocket
```

#### 3. 资源不足

```bash
# 检查系统资源
/opt/feduwa/python-vm/scripts/health_check.py --checks resources

# 查看进程资源使用
sudo -u feduwa ps aux | grep python

# 检查磁盘空间
df -h
```

#### 4. 权限问题

```bash
# 修复文件权限
sudo chown -R feduwa:feduwa /opt/feduwa/
sudo chown -R feduwa:feduwa /var/log/feduwa/
sudo chmod +x /opt/feduwa/python-vm/scripts/*.py
```

### 日志分析

#### 错误级别

- `ERROR`: 严重错误，需要立即处理
- `WARNING`: 警告信息，可能影响功能
- `INFO`: 一般信息
- `DEBUG`: 调试信息

#### 常见错误模式

```bash
# 连接错误
grep "Connection" /var/log/feduwa/feduwa-vm.log

# 任务错误
grep "Task.*failed" /var/log/feduwa/feduwa-vm.log

# 资源错误
grep -E "(Memory|CPU|Disk)" /var/log/feduwa/feduwa-vm.log
```

### 调试模式

启用调试模式获取更多信息：

```bash
# 临时启用调试
sudo -u feduwa FEDUWA_CONFIG_LOGGING_LEVEL=DEBUG /opt/feduwa/python-vm/scripts/start_vm.py start

# 或修改配置文件
sudo sed -i 's/level: "INFO"/level: "DEBUG"/' /etc/feduwa/base.yaml
sudo systemctl restart feduwa-vm.service
```

## 备份和恢复

### 备份

```bash
#!/bin/bash
# 备份脚本示例

BACKUP_DIR="/backup/feduwa-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

# 备份配置文件
sudo cp -r /etc/feduwa "$BACKUP_DIR/config"

# 备份日志文件
sudo cp -r /var/log/feduwa "$BACKUP_DIR/logs"

# 备份应用文件（可选）
sudo cp -r /opt/feduwa/python-vm "$BACKUP_DIR/app"

echo "备份完成: $BACKUP_DIR"
```

### 恢复

```bash
#!/bin/bash
# 恢复脚本示例

BACKUP_DIR="$1"

if [ -z "$BACKUP_DIR" ]; then
    echo "用法: $0 <备份目录>"
    exit 1
fi

# 停止服务
sudo systemctl stop feduwa-vm.service

# 恢复配置文件
sudo cp -r "$BACKUP_DIR/config" /etc/feduwa

# 恢复应用文件（如果需要）
sudo cp -r "$BACKUP_DIR/app" /opt/feduwa/python-vm

# 设置权限
sudo chown -R feduwa:feduwa /etc/feduwa
sudo chown -R feduwa:feduwa /opt/feduwa

# 启动服务
sudo systemctl start feduwa-vm.service

echo "恢复完成"
```

## 卸载

### 使用卸载脚本

```bash
# 标准卸载
./scripts/uninstall.sh

# 备份配置和日志
./scripts/uninstall.sh --backup-config --backup-logs

# 完全卸载（包括用户）
./scripts/uninstall.sh --remove-user --cleanup-python

# 强制卸载
./scripts/uninstall.sh --force
```

### 手动卸载

```bash
# 停止并禁用服务
sudo systemctl stop feduwa-vm.service
sudo systemctl disable feduwa-vm.service

# 删除服务文件
sudo rm -f /etc/systemd/system/feduwa-vm.service
sudo systemctl daemon-reload

# 删除应用文件
sudo rm -rf /opt/feduwa

# 删除配置文件
sudo rm -rf /etc/feduwa

# 删除日志文件
sudo rm -rf /var/log/feduwa

# 删除用户（可选）
sudo userdel -r feduwa

# 删除cron任务
sudo rm -f /etc/cron.d/feduwa-vm-health

# 删除日志轮转配置
sudo rm -f /etc/logrotate.d/feduwa-vm
```

## 安全考虑

### 网络安全

- 使用TLS/SSL加密WebSocket连接
- 配置防火墙规则限制访问
- 定期更新证书

### 系统安全

- 使用专用用户运行服务
- 限制文件权限
- 定期更新系统和依赖包
- 启用SELinux/AppArmor（如果可用）

### 数据安全

- 加密敏感配置信息
- 定期备份重要数据
- 实施访问控制
- 监控异常活动

## 更新升级

### 应用更新

```bash
# 停止服务
sudo systemctl stop feduwa-vm.service

# 备份当前版本
sudo cp -r /opt/feduwa/python-vm /opt/feduwa/python-vm.backup

# 更新代码
cd /path/to/new/version
sudo cp -r src/ /opt/feduwa/python-vm/
sudo cp -r scripts/ /opt/feduwa/python-vm/

# 更新依赖
sudo -u feduwa bash -c "
    source /opt/feduwa/python-vm/venv/bin/activate
    pip install --upgrade -r requirements.txt
"

# 设置权限
sudo chown -R feduwa:feduwa /opt/feduwa/python-vm
sudo chmod +x /opt/feduwa/python-vm/scripts/*.py

# 启动服务
sudo systemctl start feduwa-vm.service

# 验证更新
/opt/feduwa/python-vm/scripts/health_check.py
```

### 配置更新

```bash
# 备份当前配置
sudo cp /etc/feduwa/base.yaml /etc/feduwa/base.yaml.backup

# 更新配置
sudo nano /etc/feduwa/base.yaml

# 验证配置
/opt/feduwa/python-vm/scripts/health_check.py --checks config

# 重启服务应用新配置
sudo systemctl restart feduwa-vm.service
```

## 支持和帮助

### 文档资源

- [项目主页](https://github.com/your-org/fed-uwacomm)
- [API文档](docs/api/)
- [开发指南](docs/development/)

### 获取帮助

- 提交Issue: [GitHub Issues](https://github.com/your-org/fed-uwacomm/issues)
- 邮件支持: support@your-org.com
- 社区论坛: [论坛链接]

### 贡献代码

欢迎贡献代码和改进建议！请参阅[贡献指南](CONTRIBUTING.md)了解详细信息。

---

**注意**: 本文档会随着项目更新而更新，请定期查看最新版本。

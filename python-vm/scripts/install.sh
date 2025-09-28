#!/bin/bash
# FedUWA Python VM 安装脚本

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否为root用户
check_root() {
    if [[ $EUID -eq 0 ]]; then
        log_error "请不要使用root用户运行此脚本"
        exit 1
    fi
}

# 检查系统要求
check_system_requirements() {
    log_info "检查系统要求..."
    
    # 检查操作系统
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        log_success "操作系统: Linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        log_success "操作系统: macOS"
    else
        log_error "不支持的操作系统: $OSTYPE"
        exit 1
    fi
    
    # 检查Python版本
    if command -v python3 &> /dev/null; then
        PYTHON_VERSION=$(python3 -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')")
        if python3 -c "import sys; exit(0 if sys.version_info >= (3, 8) else 1)"; then
            log_success "Python版本: $PYTHON_VERSION"
        else
            log_error "Python版本过低: $PYTHON_VERSION，需要3.8或更高版本"
            exit 1
        fi
    else
        log_error "未找到Python3"
        exit 1
    fi
    
    # 检查pip
    if command -v pip3 &> /dev/null; then
        log_success "pip3已安装"
    else
        log_error "未找到pip3"
        exit 1
    fi
    
    # 检查git
    if command -v git &> /dev/null; then
        log_success "git已安装"
    else
        log_warning "git未安装，某些功能可能不可用"
    fi
}

# 创建用户和目录
setup_user_and_directories() {
    log_info "设置用户和目录..."
    
    # 创建feduwa用户（如果不存在）
    if ! id "feduwa" &>/dev/null; then
        log_info "创建feduwa用户..."
        sudo useradd -r -s /bin/bash -d /opt/feduwa -m feduwa
        log_success "feduwa用户创建完成"
    else
        log_success "feduwa用户已存在"
    fi
    
    # 创建目录结构
    sudo mkdir -p /opt/feduwa/python-vm
    sudo mkdir -p /etc/feduwa
    sudo mkdir -p /var/log/feduwa
    sudo mkdir -p /var/run
    
    # 设置目录权限
    sudo chown -R feduwa:feduwa /opt/feduwa
    sudo chown -R feduwa:feduwa /var/log/feduwa
    sudo chmod 755 /opt/feduwa
    sudo chmod 755 /var/log/feduwa
    
    log_success "目录结构创建完成"
}

# 安装Python依赖
install_python_dependencies() {
    log_info "安装Python依赖..."
    
    # 创建虚拟环境
    VENV_PATH="/opt/feduwa/python-vm/venv"
    
    if [ ! -d "$VENV_PATH" ]; then
        log_info "创建Python虚拟环境..."
        sudo -u feduwa python3 -m venv "$VENV_PATH"
        log_success "虚拟环境创建完成"
    fi
    
    # 激活虚拟环境并安装依赖
    log_info "安装Python包..."
    sudo -u feduwa bash -c "
        source $VENV_PATH/bin/activate
        pip install --upgrade pip
        pip install -r requirements.txt
    "
    
    log_success "Python依赖安装完成"
}

# 复制文件
copy_files() {
    log_info "复制项目文件..."
    
    # 复制源代码
    sudo cp -r src/ /opt/feduwa/python-vm/
    sudo cp -r scripts/ /opt/feduwa/python-vm/
    sudo cp -r tests/ /opt/feduwa/python-vm/
    sudo cp requirements.txt /opt/feduwa/python-vm/
    sudo cp pytest.ini /opt/feduwa/python-vm/
    
    # 设置文件权限
    sudo chown -R feduwa:feduwa /opt/feduwa/python-vm
    sudo chmod +x /opt/feduwa/python-vm/scripts/*.py
    
    log_success "项目文件复制完成"
}

# 安装配置文件
install_config_files() {
    log_info "安装配置文件..."
    
    # 创建默认配置
    sudo tee /etc/feduwa/base.yaml > /dev/null << 'EOF'
websocket:
  server_url: "ws://localhost:8080/websocket"
  connection_timeout: 30
  heartbeat_interval: 30
  max_reconnect_attempts: 5
  reconnect_delay: 5

federated:
  supported_algorithms:
    - "FEDERATED_AVERAGING"
    - "FEDERATED_PROXIMAL"
    - "FEDERATED_NOVA"
    - "SCAFFOLD"
  max_concurrent_tasks: 3
  default_local_epochs: 5
  default_batch_size: 32

logging:
  level: "INFO"
  format: "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
  file:
    enabled: true
    path: "/var/log/feduwa/feduwa-vm.log"
    max_size: "10MB"
    backup_count: 5
  console:
    enabled: true
    level: "INFO"

performance:
  monitoring:
    enabled: true
    interval: 60
    metrics: ["cpu", "memory", "disk", "network"]
EOF

    # 生产环境配置
    sudo tee /etc/feduwa/prod.yaml > /dev/null << 'EOF'
websocket:
  server_url: "ws://production-server:8080/websocket"
  heartbeat_interval: 60

logging:
  level: "WARNING"
  console:
    enabled: false

security:
  encryption:
    enabled: true
  authentication:
    enabled: true
EOF

    # 设置配置文件权限
    sudo chown -R feduwa:feduwa /etc/feduwa
    sudo chmod 640 /etc/feduwa/*.yaml
    
    log_success "配置文件安装完成"
}

# 安装systemd服务
install_systemd_service() {
    log_info "安装systemd服务..."
    
    # 复制服务文件
    sudo cp scripts/feduwa-vm.service /etc/systemd/system/
    
    # 重新加载systemd
    sudo systemctl daemon-reload
    
    # 启用服务
    sudo systemctl enable feduwa-vm.service
    
    log_success "systemd服务安装完成"
}

# 创建日志轮转配置
setup_log_rotation() {
    log_info "设置日志轮转..."
    
    sudo tee /etc/logrotate.d/feduwa-vm > /dev/null << 'EOF'
/var/log/feduwa/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 feduwa feduwa
    postrotate
        systemctl reload feduwa-vm.service > /dev/null 2>&1 || true
    endscript
}
EOF
    
    log_success "日志轮转配置完成"
}

# 创建监控脚本
setup_monitoring() {
    log_info "设置监控..."
    
    # 创建cron任务进行健康检查
    sudo tee /etc/cron.d/feduwa-vm-health > /dev/null << 'EOF'
# FedUWA VM 健康检查
*/5 * * * * feduwa /opt/feduwa/python-vm/scripts/health_check.py --output json > /var/log/feduwa/health-check.log 2>&1
EOF
    
    log_success "监控设置完成"
}

# 运行测试
run_tests() {
    log_info "运行测试..."
    
    sudo -u feduwa bash -c "
        cd /opt/feduwa/python-vm
        source venv/bin/activate
        python -m pytest tests/unit/ -v --tb=short
    "
    
    if [ $? -eq 0 ]; then
        log_success "测试通过"
    else
        log_warning "部分测试失败，但安装继续进行"
    fi
}

# 启动服务
start_service() {
    log_info "启动服务..."
    
    # 启动服务
    sudo systemctl start feduwa-vm.service
    
    # 检查服务状态
    sleep 3
    if sudo systemctl is-active --quiet feduwa-vm.service; then
        log_success "服务启动成功"
    else
        log_error "服务启动失败"
        log_info "查看服务状态: sudo systemctl status feduwa-vm.service"
        log_info "查看日志: sudo journalctl -u feduwa-vm.service -f"
        exit 1
    fi
}

# 显示安装后信息
show_post_install_info() {
    log_success "安装完成！"
    echo
    echo "服务管理命令:"
    echo "  启动服务: sudo systemctl start feduwa-vm.service"
    echo "  停止服务: sudo systemctl stop feduwa-vm.service"
    echo "  重启服务: sudo systemctl restart feduwa-vm.service"
    echo "  查看状态: sudo systemctl status feduwa-vm.service"
    echo "  查看日志: sudo journalctl -u feduwa-vm.service -f"
    echo
    echo "健康检查:"
    echo "  手动检查: /opt/feduwa/python-vm/scripts/health_check.py"
    echo "  查看健康日志: tail -f /var/log/feduwa/health-check.log"
    echo
    echo "配置文件:"
    echo "  主配置: /etc/feduwa/base.yaml"
    echo "  环境配置: /etc/feduwa/prod.yaml"
    echo
    echo "日志文件:"
    echo "  应用日志: /var/log/feduwa/feduwa-vm.log"
    echo "  健康检查: /var/log/feduwa/health-check.log"
    echo
}

# 主函数
main() {
    log_info "开始安装FedUWA Python VM..."
    
    check_root
    check_system_requirements
    setup_user_and_directories
    install_python_dependencies
    copy_files
    install_config_files
    install_systemd_service
    setup_log_rotation
    setup_monitoring
    run_tests
    start_service
    show_post_install_info
}

# 解析命令行参数
SKIP_TESTS=false
SKIP_START=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-start)
            SKIP_START=true
            shift
            ;;
        -h|--help)
            echo "用法: $0 [选项]"
            echo "选项:"
            echo "  --skip-tests    跳过测试"
            echo "  --skip-start    跳过服务启动"
            echo "  -h, --help      显示帮助信息"
            exit 0
            ;;
        *)
            log_error "未知选项: $1"
            exit 1
            ;;
    esac
done

# 修改主函数以支持选项
main_with_options() {
    log_info "开始安装FedUWA Python VM..."
    
    check_root
    check_system_requirements
    setup_user_and_directories
    install_python_dependencies
    copy_files
    install_config_files
    install_systemd_service
    setup_log_rotation
    setup_monitoring
    
    if [ "$SKIP_TESTS" = false ]; then
        run_tests
    else
        log_info "跳过测试"
    fi
    
    if [ "$SKIP_START" = false ]; then
        start_service
    else
        log_info "跳过服务启动"
    fi
    
    show_post_install_info
}

# 运行安装
main_with_options

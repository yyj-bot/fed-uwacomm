#!/bin/bash
# FedUWA Python VM 卸载脚本

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

# 确认卸载
confirm_uninstall() {
    echo -e "${YELLOW}警告: 这将完全卸载FedUWA Python VM服务${NC}"
    echo "以下内容将被删除:"
    echo "  - 服务文件和配置"
    echo "  - 日志文件"
    echo "  - 用户数据"
    echo "  - systemd服务"
    echo
    
    read -p "确定要继续吗? (y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "卸载已取消"
        exit 0
    fi
}

# 停止服务
stop_service() {
    log_info "停止服务..."
    
    if systemctl is-active --quiet feduwa-vm.service 2>/dev/null; then
        sudo systemctl stop feduwa-vm.service
        log_success "服务已停止"
    else
        log_info "服务未运行"
    fi
    
    if systemctl is-enabled --quiet feduwa-vm.service 2>/dev/null; then
        sudo systemctl disable feduwa-vm.service
        log_success "服务已禁用"
    fi
}

# 删除systemd服务文件
remove_systemd_service() {
    log_info "删除systemd服务..."
    
    if [ -f "/etc/systemd/system/feduwa-vm.service" ]; then
        sudo rm -f /etc/systemd/system/feduwa-vm.service
        sudo systemctl daemon-reload
        log_success "systemd服务文件已删除"
    else
        log_info "systemd服务文件不存在"
    fi
}

# 删除cron任务
remove_cron_jobs() {
    log_info "删除cron任务..."
    
    if [ -f "/etc/cron.d/feduwa-vm-health" ]; then
        sudo rm -f /etc/cron.d/feduwa-vm-health
        log_success "健康检查cron任务已删除"
    fi
}

# 删除日志轮转配置
remove_log_rotation() {
    log_info "删除日志轮转配置..."
    
    if [ -f "/etc/logrotate.d/feduwa-vm" ]; then
        sudo rm -f /etc/logrotate.d/feduwa-vm
        log_success "日志轮转配置已删除"
    fi
}

# 删除配置文件
remove_config_files() {
    log_info "删除配置文件..."
    
    if [ -d "/etc/feduwa" ]; then
        # 备份配置文件（如果用户需要）
        if [ "$BACKUP_CONFIG" = true ]; then
            BACKUP_DIR="/tmp/feduwa-config-backup-$(date +%Y%m%d-%H%M%S)"
            sudo cp -r /etc/feduwa "$BACKUP_DIR"
            sudo chown -R $USER:$USER "$BACKUP_DIR"
            log_info "配置文件已备份到: $BACKUP_DIR"
        fi
        
        sudo rm -rf /etc/feduwa
        log_success "配置文件已删除"
    else
        log_info "配置文件不存在"
    fi
}

# 删除日志文件
remove_log_files() {
    log_info "删除日志文件..."
    
    if [ -d "/var/log/feduwa" ]; then
        # 备份日志文件（如果用户需要）
        if [ "$BACKUP_LOGS" = true ]; then
            BACKUP_DIR="/tmp/feduwa-logs-backup-$(date +%Y%m%d-%H%M%S)"
            sudo cp -r /var/log/feduwa "$BACKUP_DIR"
            sudo chown -R $USER:$USER "$BACKUP_DIR"
            log_info "日志文件已备份到: $BACKUP_DIR"
        fi
        
        sudo rm -rf /var/log/feduwa
        log_success "日志文件已删除"
    else
        log_info "日志文件不存在"
    fi
}

# 删除运行时文件
remove_runtime_files() {
    log_info "删除运行时文件..."
    
    # 删除PID文件
    if [ -f "/var/run/feduwa-vm.pid" ]; then
        sudo rm -f /var/run/feduwa-vm.pid
    fi
    
    # 删除状态文件
    if [ -f "/var/run/feduwa-vm-status.json" ]; then
        sudo rm -f /var/run/feduwa-vm-status.json
    fi
    
    log_success "运行时文件已删除"
}

# 删除应用文件
remove_application_files() {
    log_info "删除应用文件..."
    
    if [ -d "/opt/feduwa/python-vm" ]; then
        sudo rm -rf /opt/feduwa/python-vm
        log_success "应用文件已删除"
    else
        log_info "应用文件不存在"
    fi
    
    # 如果/opt/feduwa目录为空，也删除它
    if [ -d "/opt/feduwa" ] && [ -z "$(ls -A /opt/feduwa)" ]; then
        sudo rmdir /opt/feduwa
        log_success "空的/opt/feduwa目录已删除"
    fi
}

# 删除用户（可选）
remove_user() {
    if [ "$REMOVE_USER" = true ]; then
        log_info "删除feduwa用户..."
        
        if id "feduwa" &>/dev/null; then
            # 停止所有feduwa用户的进程
            sudo pkill -u feduwa || true
            
            # 删除用户和家目录
            sudo userdel -r feduwa 2>/dev/null || true
            log_success "feduwa用户已删除"
        else
            log_info "feduwa用户不存在"
        fi
    else
        log_info "保留feduwa用户（使用--remove-user删除）"
    fi
}

# 清理Python包（可选）
cleanup_python_packages() {
    if [ "$CLEANUP_PYTHON" = true ]; then
        log_info "清理Python包..."
        
        # 这里可以添加清理特定Python包的逻辑
        # 但要小心不要删除系统需要的包
        log_warning "Python包清理需要手动执行"
        echo "如需清理，请手动执行:"
        echo "  pip3 uninstall websockets numpy scikit-learn psutil pyyaml"
    fi
}

# 验证卸载
verify_uninstall() {
    log_info "验证卸载..."
    
    local issues=()
    
    # 检查服务是否还在运行
    if systemctl is-active --quiet feduwa-vm.service 2>/dev/null; then
        issues+=("服务仍在运行")
    fi
    
    # 检查文件是否还存在
    if [ -f "/etc/systemd/system/feduwa-vm.service" ]; then
        issues+=("systemd服务文件仍存在")
    fi
    
    if [ -d "/opt/feduwa/python-vm" ]; then
        issues+=("应用文件仍存在")
    fi
    
    if [ -d "/etc/feduwa" ]; then
        issues+=("配置文件仍存在")
    fi
    
    if [ ${#issues[@]} -eq 0 ]; then
        log_success "卸载验证通过"
        return 0
    else
        log_warning "发现以下问题:"
        for issue in "${issues[@]}"; do
            echo "  - $issue"
        done
        return 1
    fi
}

# 显示卸载后信息
show_post_uninstall_info() {
    log_success "卸载完成！"
    echo
    
    if [ "$BACKUP_CONFIG" = true ] || [ "$BACKUP_LOGS" = true ]; then
        echo "备份文件位置:"
        if [ "$BACKUP_CONFIG" = true ]; then
            echo "  配置文件: /tmp/feduwa-config-backup-*"
        fi
        if [ "$BACKUP_LOGS" = true ]; then
            echo "  日志文件: /tmp/feduwa-logs-backup-*"
        fi
        echo
    fi
    
    echo "如需重新安装，请运行安装脚本"
    echo
}

# 主函数
main() {
    log_info "开始卸载FedUWA Python VM..."
    
    confirm_uninstall
    stop_service
    remove_systemd_service
    remove_cron_jobs
    remove_log_rotation
    remove_runtime_files
    remove_config_files
    remove_log_files
    remove_application_files
    remove_user
    cleanup_python_packages
    
    if verify_uninstall; then
        show_post_uninstall_info
    else
        log_warning "卸载可能不完整，请手动检查剩余文件"
    fi
}

# 解析命令行参数
BACKUP_CONFIG=false
BACKUP_LOGS=false
REMOVE_USER=false
CLEANUP_PYTHON=false
FORCE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --backup-config)
            BACKUP_CONFIG=true
            shift
            ;;
        --backup-logs)
            BACKUP_LOGS=true
            shift
            ;;
        --remove-user)
            REMOVE_USER=true
            shift
            ;;
        --cleanup-python)
            CLEANUP_PYTHON=true
            shift
            ;;
        --force)
            FORCE=true
            shift
            ;;
        -h|--help)
            echo "用法: $0 [选项]"
            echo "选项:"
            echo "  --backup-config    备份配置文件"
            echo "  --backup-logs      备份日志文件"
            echo "  --remove-user      删除feduwa用户"
            echo "  --cleanup-python   清理Python包"
            echo "  --force            强制卸载，不询问确认"
            echo "  -h, --help         显示帮助信息"
            exit 0
            ;;
        *)
            log_error "未知选项: $1"
            exit 1
            ;;
    esac
done

# 修改主函数以支持强制模式
main_with_options() {
    log_info "开始卸载FedUWA Python VM..."
    
    if [ "$FORCE" = false ]; then
        confirm_uninstall
    fi
    
    stop_service
    remove_systemd_service
    remove_cron_jobs
    remove_log_rotation
    remove_runtime_files
    remove_config_files
    remove_log_files
    remove_application_files
    remove_user
    cleanup_python_packages
    
    if verify_uninstall; then
        show_post_uninstall_info
    else
        log_warning "卸载可能不完整，请手动检查剩余文件"
    fi
}

# 检查权限
if [[ $EUID -eq 0 ]]; then
    log_error "请不要使用root用户运行此脚本"
    exit 1
fi

# 运行卸载
main_with_options

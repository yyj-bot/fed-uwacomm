# 部署和维护指导

本文档提供 Python VM WebSocket 协议 v1.4 重构后的完整部署指导和长期维护策略，确保生产环境的稳定运行。

## 1. 部署准备

### 1.1 环境要求

#### 系统要求
```yaml
操作系统:
  - Ubuntu 20.04 LTS 或更高版本
  - CentOS 8 或更高版本
  - macOS 10.15 或更高版本 (开发环境)
  - Windows 10 (开发环境，生产不推荐)

硬件要求:
  最低配置:
    - CPU: 2核心
    - RAM: 4GB
    - 磁盘: 20GB
    - 网络: 100Mbps

  推荐配置:
    - CPU: 4核心或更多
    - RAM: 8GB或更多
    - 磁盘: 50GB或更多 (SSD推荐)
    - 网络: 1Gbps

  生产环境:
    - CPU: 8核心或更多
    - RAM: 16GB或更多
    - 磁盘: 100GB或更多 (NVMe SSD)
    - 网络: 10Gbps
```

#### 软件依赖
```yaml
Python环境:
  - Python 3.8 或更高版本
  - pip 20.0 或更高版本
  - virtualenv 或 conda (推荐)

系统依赖:
  Ubuntu/Debian:
    - build-essential
    - python3-dev
    - libmysqlclient-dev
    - libssl-dev
    - pkg-config

  CentOS/RHEL:
    - gcc
    - gcc-c++
    - python3-devel
    - mysql-devel
    - openssl-devel

  macOS:
    - Xcode命令行工具
    - Homebrew (推荐)
```

### 1.2 依赖安装

#### Python虚拟环境设置
```bash
# 创建虚拟环境
python3 -m venv federated_vm_env

# 激活虚拟环境
source federated_vm_env/bin/activate  # Linux/macOS
# 或
federated_vm_env\Scripts\activate     # Windows

# 升级pip
pip install --upgrade pip setuptools wheel
```

#### 安装项目依赖
```bash
# 安装核心依赖
pip install -r requirements.txt

# 安装开发依赖 (可选)
pip install -r requirements-dev.txt

# 安装项目包
pip install -e .
```

#### requirements.txt 示例
```txt
# WebSocket和网络通信
websocket-client>=1.6.0
requests>=2.28.0

# 机器学习和数据处理
numpy>=1.21.0
pandas>=1.5.0
scikit-learn>=1.1.0
scipy>=1.9.0

# 数据库连接
PyMySQL>=1.0.0
SQLAlchemy>=1.4.0

# 配置和日志
PyYAML>=6.0
python-dotenv>=0.19.0

# 系统监控
psutil>=5.9.0

# 序列化和压缩
pickle5>=0.0.12; python_version < "3.8"
lz4>=4.0.0

# 测试框架 (开发环境)
pytest>=7.0.0
pytest-cov>=4.0.0
pytest-asyncio>=0.21.0

# 代码质量
black>=22.0.0
flake8>=5.0.0
mypy>=0.991
```

### 1.3 配置文件准备

#### 主配置文件 (config.yml)
```yaml
# config.yml
server:
  # WebSocket服务器配置
  websocket:
    url: "ws://localhost:8080/websocket"
    reconnect_interval: 5
    heartbeat_interval: 30
    connection_timeout: 10

  # 认证配置
  auth:
    access_token: "${ACCESS_TOKEN}"
    vm_id: "${VM_ID}"
    api_key: "${API_KEY}"

# 联邦学习配置
federated_learning:
  # 性能配置
  performance:
    max_concurrent_tasks: 3
    max_memory_mb: 2048
    thread_pool_size: 5

  # 算法配置
  algorithms:
    default_params:
      learning_rate: 0.01
      local_epochs: 5
      batch_size: 32

  # 数据配置
  data:
    cache_enabled: true
    cache_size_mb: 512
    preprocessing:
      feature_extraction:
        enabled: true
        cache_features: true
      normalization:
        enabled: true
        method: "standard"

  # 压缩配置
  compression:
    gradient_compression:
      enabled: true
      type: "quantization"
      bits: 8
    model_compression:
      enabled: true

# 数据库配置
database:
  host: "${DB_HOST:localhost}"
  port: "${DB_PORT:3306}"
  database: "${DB_NAME:feduwacomm}"
  username: "${DB_USER:feduwacomm}"
  password: "${DB_PASSWORD}"
  pool_size: 10
  max_overflow: 20
  pool_timeout: 30

# 日志配置
logging:
  level: "INFO"
  format: "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
  file: "federated_learning.log"
  max_size_mb: 100
  backup_count: 5
  console_output: true

# 监控配置
monitoring:
  resource_monitoring:
    enabled: true
    interval_seconds: 30
  performance_logging:
    enabled: true
  metrics_export:
    enabled: false
    endpoint: "http://localhost:9090/metrics"

# 安全配置
security:
  ssl_verify: true
  certificate_path: ""
  private_key_path: ""
  encrypt_sensitive_data: true
```

#### 环境变量配置 (.env)
```bash
# .env
# 服务器连接
ACCESS_TOKEN=your_access_token_here
VM_ID=vm_001
API_KEY=your_api_key_here

# 数据库连接
DB_HOST=localhost
DB_PORT=3306
DB_NAME=feduwacomm
DB_USER=feduwacomm
DB_PASSWORD=your_db_password_here

# 可选配置
ENVIRONMENT=production
LOG_LEVEL=INFO
DEBUG=false

# SSL配置 (生产环境)
SSL_CERT_PATH=/path/to/cert.pem
SSL_KEY_PATH=/path/to/key.pem
```

## 2. 部署实施

### 2.1 单机部署

#### 部署脚本 (deploy.sh)
```bash
#!/bin/bash
# deploy.sh - 单机部署脚本

set -e

# 配置变量
PROJECT_DIR="/opt/federated-learning-vm"
SERVICE_USER="fedlearn"
CONFIG_DIR="$PROJECT_DIR/config"
LOG_DIR="/var/log/federated-learning"
PYTHON_ENV="$PROJECT_DIR/venv"

echo "🚀 Starting Federated Learning VM deployment..."

# 创建服务用户
if ! id "$SERVICE_USER" &>/dev/null; then
    echo "Creating service user: $SERVICE_USER"
    sudo useradd -r -s /bin/false -d "$PROJECT_DIR" "$SERVICE_USER"
fi

# 创建目录结构
echo "Creating directory structure..."
sudo mkdir -p "$PROJECT_DIR"
sudo mkdir -p "$CONFIG_DIR"
sudo mkdir -p "$LOG_DIR"
sudo chown -R "$SERVICE_USER:$SERVICE_USER" "$PROJECT_DIR"
sudo chown -R "$SERVICE_USER:$SERVICE_USER" "$LOG_DIR"

# 复制代码
echo "Copying application code..."
sudo cp -r ./src "$PROJECT_DIR/"
sudo cp -r ./config/* "$CONFIG_DIR/"
sudo cp requirements.txt "$PROJECT_DIR/"
sudo chown -R "$SERVICE_USER:$SERVICE_USER" "$PROJECT_DIR"

# 创建Python虚拟环境
echo "Setting up Python virtual environment..."
sudo -u "$SERVICE_USER" python3 -m venv "$PYTHON_ENV"
sudo -u "$SERVICE_USER" "$PYTHON_ENV/bin/pip" install --upgrade pip
sudo -u "$SERVICE_USER" "$PYTHON_ENV/bin/pip" install -r "$PROJECT_DIR/requirements.txt"
sudo -u "$SERVICE_USER" "$PYTHON_ENV/bin/pip" install -e "$PROJECT_DIR"

# 复制配置文件
echo "Setting up configuration..."
if [ ! -f "$CONFIG_DIR/.env" ]; then
    sudo cp "$CONFIG_DIR/.env.example" "$CONFIG_DIR/.env"
    echo "⚠️ Please edit $CONFIG_DIR/.env with your configuration"
fi

# 创建systemd服务
echo "Creating systemd service..."
sudo tee /etc/systemd/system/federated-learning-vm.service > /dev/null <<EOF
[Unit]
Description=Federated Learning VM Client
After=network.target mysql.service
Wants=network.target

[Service]
Type=simple
User=$SERVICE_USER
Group=$SERVICE_USER
WorkingDirectory=$PROJECT_DIR
Environment=PYTHONPATH=$PROJECT_DIR/src
EnvironmentFile=$CONFIG_DIR/.env
ExecStart=$PYTHON_ENV/bin/python -m feduwacomm.main --config $CONFIG_DIR/config.yml
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=federated-learning-vm

# 安全设置
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=$LOG_DIR $PROJECT_DIR/data

[Install]
WantedBy=multi-user.target
EOF

# 重新加载systemd
sudo systemctl daemon-reload

# 创建日志轮转配置
echo "Setting up log rotation..."
sudo tee /etc/logrotate.d/federated-learning-vm > /dev/null <<EOF
$LOG_DIR/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 644 $SERVICE_USER $SERVICE_USER
    postrotate
        systemctl reload federated-learning-vm || true
    endrotate
}
EOF

echo "✅ Deployment completed!"
echo ""
echo "Next steps:"
echo "1. Edit configuration: sudo nano $CONFIG_DIR/.env"
echo "2. Edit main config: sudo nano $CONFIG_DIR/config.yml"
echo "3. Start service: sudo systemctl start federated-learning-vm"
echo "4. Enable auto-start: sudo systemctl enable federated-learning-vm"
echo "5. Check status: sudo systemctl status federated-learning-vm"
echo "6. View logs: sudo journalctl -u federated-learning-vm -f"
```

#### 启动和管理
```bash
# 启动服务
sudo systemctl start federated-learning-vm

# 启用开机自启
sudo systemctl enable federated-learning-vm

# 查看服务状态
sudo systemctl status federated-learning-vm

# 查看实时日志
sudo journalctl -u federated-learning-vm -f

# 重启服务
sudo systemctl restart federated-learning-vm

# 停止服务
sudo systemctl stop federated-learning-vm
```

### 2.2 容器化部署

#### Dockerfile
```dockerfile
# Dockerfile
FROM python:3.9-slim

# 设置工作目录
WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    build-essential \
    libmysqlclient-dev \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY src/ ./src/
COPY config/ ./config/

# 安装应用
RUN pip install -e .

# 创建非root用户
RUN useradd -r -s /bin/false fedlearn && \
    chown -R fedlearn:fedlearn /app

# 创建数据和日志目录
RUN mkdir -p /app/data /app/logs && \
    chown -R fedlearn:fedlearn /app/data /app/logs

USER fedlearn

# 暴露端口 (如果需要)
# EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "from feduwacomm.utils.health_check import health_check; exit(0 if health_check() else 1)"

# 启动命令
CMD ["python", "-m", "feduwacomm.main", "--config", "config/config.yml"]
```

#### Docker Compose 配置
```yaml
# docker-compose.yml
version: '3.8'

services:
  federated-vm:
    build: .
    container_name: federated-learning-vm
    restart: unless-stopped

    environment:
      - ACCESS_TOKEN=${ACCESS_TOKEN}
      - VM_ID=${VM_ID}
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=feduwacomm
      - DB_USER=feduwacomm
      - DB_PASSWORD=${DB_PASSWORD}

    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
      - ./config:/app/config

    depends_on:
      - mysql

    networks:
      - federated-network

    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '2.0'
        reservations:
          memory: 1G
          cpus: '1.0'

  mysql:
    image: mysql:8.0
    container_name: federated-mysql
    restart: unless-stopped

    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=feduwacomm
      - MYSQL_USER=feduwacomm
      - MYSQL_PASSWORD=${DB_PASSWORD}

    volumes:
      - mysql_data:/var/lib/mysql
      - ./init:/docker-entrypoint-initdb.d

    ports:
      - "3306:3306"

    networks:
      - federated-network

  # 可选：监控服务
  prometheus:
    image: prom/prometheus:latest
    container_name: federated-prometheus
    restart: unless-stopped

    ports:
      - "9090:9090"

    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus

    networks:
      - federated-network

volumes:
  mysql_data:
  prometheus_data:

networks:
  federated-network:
    driver: bridge
```

#### 容器部署脚本
```bash
#!/bin/bash
# docker-deploy.sh

set -e

echo "🐳 Starting containerized deployment..."

# 检查Docker和Docker Compose
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed"
    exit 1
fi

# 创建必要的目录
mkdir -p data logs config monitoring

# 复制配置文件模板
if [ ! -f .env ]; then
    cp .env.example .env
    echo "⚠️ Please edit .env file with your configuration"
fi

# 构建镜像
echo "Building Docker image..."
docker-compose build

# 启动服务
echo "Starting services..."
docker-compose up -d

# 等待服务启动
echo "Waiting for services to start..."
sleep 10

# 检查服务状态
echo "Checking service status..."
docker-compose ps

echo "✅ Containerized deployment completed!"
echo ""
echo "Useful commands:"
echo "  View logs: docker-compose logs -f federated-vm"
echo "  Stop services: docker-compose down"
echo "  Restart: docker-compose restart federated-vm"
echo "  Update: docker-compose pull && docker-compose up -d"
```

### 2.3 Kubernetes部署

#### Kubernetes配置清单
```yaml
# k8s-deployment.yml
apiVersion: v1
kind: Namespace
metadata:
  name: federated-learning

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: federated-vm-config
  namespace: federated-learning
data:
  config.yml: |
    server:
      websocket:
        url: "ws://federated-server:8080/websocket"
        reconnect_interval: 5
        heartbeat_interval: 30

    federated_learning:
      performance:
        max_concurrent_tasks: 3
        max_memory_mb: 2048

    database:
      host: "mysql-service"
      port: 3306
      database: "feduwacomm"

    logging:
      level: "INFO"
      console_output: true

---
apiVersion: v1
kind: Secret
metadata:
  name: federated-vm-secrets
  namespace: federated-learning
type: Opaque
data:
  access-token: <base64-encoded-access-token>
  db-password: <base64-encoded-db-password>
  vm-id: <base64-encoded-vm-id>

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: federated-vm
  namespace: federated-learning
  labels:
    app: federated-vm
spec:
  replicas: 1
  selector:
    matchLabels:
      app: federated-vm
  template:
    metadata:
      labels:
        app: federated-vm
    spec:
      containers:
      - name: federated-vm
        image: federated-learning-vm:latest
        imagePullPolicy: Always

        env:
        - name: ACCESS_TOKEN
          valueFrom:
            secretKeyRef:
              name: federated-vm-secrets
              key: access-token
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: federated-vm-secrets
              key: db-password
        - name: VM_ID
          valueFrom:
            secretKeyRef:
              name: federated-vm-secrets
              key: vm-id

        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: data-volume
          mountPath: /app/data
        - name: logs-volume
          mountPath: /app/logs

        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"

        livenessProbe:
          exec:
            command:
            - python
            - -c
            - "from feduwacomm.utils.health_check import health_check; exit(0 if health_check() else 1)"
          initialDelaySeconds: 30
          periodSeconds: 30

        readinessProbe:
          exec:
            command:
            - python
            - -c
            - "from feduwacomm.utils.health_check import health_check; exit(0 if health_check() else 1)"
          initialDelaySeconds: 10
          periodSeconds: 10

      volumes:
      - name: config-volume
        configMap:
          name: federated-vm-config
      - name: data-volume
        persistentVolumeClaim:
          claimName: federated-vm-data
      - name: logs-volume
        persistentVolumeClaim:
          claimName: federated-vm-logs

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: federated-vm-data
  namespace: federated-learning
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: federated-vm-logs
  namespace: federated-learning
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi

---
# 可选：水平自动扩缩容
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: federated-vm-hpa
  namespace: federated-learning
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: federated-vm
  minReplicas: 1
  maxReplicas: 3
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## 3. 生产环境优化

### 3.1 性能优化

#### 系统级优化
```bash
# 系统参数优化脚本 (optimize_system.sh)
#!/bin/bash

echo "🔧 Optimizing system parameters for federated learning..."

# 网络优化
echo "Optimizing network parameters..."
cat >> /etc/sysctl.conf <<EOF
# 网络缓冲区优化
net.core.rmem_max = 134217728
net.core.wmem_max = 134217728
net.ipv4.tcp_rmem = 4096 87380 134217728
net.ipv4.tcp_wmem = 4096 65536 134217728

# TCP优化
net.ipv4.tcp_congestion_control = bbr
net.ipv4.tcp_no_metrics_save = 1
net.ipv4.tcp_moderate_rcvbuf = 1

# 连接优化
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.tcp_fin_timeout = 10
EOF

# 内存优化
echo "Optimizing memory parameters..."
cat >> /etc/sysctl.conf <<EOF
# 内存管理优化
vm.swappiness = 10
vm.dirty_ratio = 15
vm.dirty_background_ratio = 5
vm.overcommit_memory = 1
EOF

# 文件描述符优化
echo "Optimizing file descriptors..."
cat >> /etc/security/limits.conf <<EOF
# 文件描述符限制
* soft nofile 65536
* hard nofile 65536
* soft nproc 32768
* hard nproc 32768
EOF

# 应用系统参数
sysctl -p

echo "✅ System optimization completed!"
```

#### 应用级优化配置
```yaml
# performance_config.yml
federated_learning:
  performance:
    # 并发控制
    max_concurrent_tasks: 5
    thread_pool_size: 8
    max_memory_mb: 4096

    # 数据处理优化
    batch_processing:
      enabled: true
      batch_size: 1000
      max_batch_memory_mb: 512

    # 缓存优化
    caching:
      model_cache_size: 5
      gradient_cache_size: 10
      feature_cache_size_mb: 1024
      cache_cleanup_interval: 300

    # 网络优化
    network:
      connection_pool_size: 10
      keep_alive: true
      timeout: 30
      retry_attempts: 3
      backoff_factor: 2

    # 内存管理
    memory_management:
      gc_threshold: 0.8
      gc_interval: 60
      large_object_threshold_mb: 100

  # 压缩优化
  compression:
    gradient_compression:
      enabled: true
      type: "adaptive"  # adaptive, quantization, sparsification
      compression_ratio: 0.1

    model_compression:
      enabled: true
      algorithm: "gzip"
      level: 6

    data_compression:
      enabled: true
      cache_compressed: true

  # I/O优化
  io_optimization:
    async_io: true
    buffer_size: 65536
    read_ahead: true
    write_through: false
```

### 3.2 安全配置

#### SSL/TLS配置
```yaml
# security_config.yml
security:
  # SSL/TLS配置
  ssl:
    enabled: true
    verify_certificates: true
    certificate_path: "/etc/ssl/certs/federated-vm.crt"
    private_key_path: "/etc/ssl/private/federated-vm.key"
    ca_bundle_path: "/etc/ssl/certs/ca-certificates.crt"

    # 协议版本
    min_version: "TLSv1.2"
    max_version: "TLSv1.3"

    # 密码套件
    ciphers: "ECDHE+AESGCM:ECDHE+CHACHA20:DHE+AESGCM:DHE+CHACHA20:!aNULL:!MD5:!DSS"

  # 认证配置
  authentication:
    token_validation: strict
    token_refresh_interval: 3600
    max_token_age: 86400

    # API密钥管理
    api_key_rotation: true
    api_key_rotation_interval: 2592000  # 30天

  # 数据加密
  encryption:
    # 静态数据加密
    encrypt_at_rest: true
    encryption_algorithm: "AES-256-GCM"
    key_derivation: "PBKDF2"

    # 传输加密
    encrypt_in_transit: true

    # 敏感数据字段
    sensitive_fields:
      - "access_token"
      - "api_key"
      - "db_password"
      - "private_key"

  # 访问控制
  access_control:
    # IP白名单
    allowed_ips:
      - "10.0.0.0/8"
      - "172.16.0.0/12"
      - "192.168.0.0/16"

    # 速率限制
    rate_limiting:
      enabled: true
      requests_per_minute: 1000
      burst_size: 100

    # 审计日志
    audit_logging:
      enabled: true
      log_file: "/var/log/federated-learning/audit.log"
      log_level: "INFO"
```

#### 安全部署脚本
```bash
#!/bin/bash
# security_setup.sh

set -e

echo "🔒 Setting up security configurations..."

# 创建SSL证书目录
sudo mkdir -p /etc/ssl/federated-learning
sudo chmod 755 /etc/ssl/federated-learning

# 生成自签名证书（开发环境）
if [ "$1" = "dev" ]; then
    echo "Generating self-signed certificate for development..."
    sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout /etc/ssl/federated-learning/server.key \
        -out /etc/ssl/federated-learning/server.crt \
        -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost"
fi

# 设置证书权限
sudo chmod 600 /etc/ssl/federated-learning/server.key
sudo chmod 644 /etc/ssl/federated-learning/server.crt

# 创建安全配置目录
sudo mkdir -p /etc/federated-learning/security
sudo chmod 750 /etc/federated-learning/security

# 生成随机密钥
echo "Generating security keys..."
ENCRYPTION_KEY=$(openssl rand -hex 32)
API_SECRET=$(openssl rand -hex 16)

# 创建密钥文件
sudo tee /etc/federated-learning/security/keys.env > /dev/null <<EOF
ENCRYPTION_KEY=$ENCRYPTION_KEY
API_SECRET=$API_SECRET
JWT_SECRET=$(openssl rand -hex 32)
EOF

sudo chmod 600 /etc/federated-learning/security/keys.env

# 配置防火墙
echo "Configuring firewall..."
sudo ufw --force reset
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 允许SSH
sudo ufw allow ssh

# 允许WebSocket连接（如果是服务器）
sudo ufw allow 8080/tcp

# 允许MySQL（如果本地数据库）
sudo ufw allow from 10.0.0.0/8 to any port 3306
sudo ufw allow from 172.16.0.0/12 to any port 3306
sudo ufw allow from 192.168.0.0/16 to any port 3306

# 启用防火墙
sudo ufw --force enable

echo "✅ Security setup completed!"
```

### 3.3 监控和日志

#### 监控配置
```yaml
# monitoring_config.yml
monitoring:
  # 系统指标监控
  system_metrics:
    enabled: true
    collection_interval: 30
    metrics:
      - cpu_usage
      - memory_usage
      - disk_usage
      - network_io
      - process_count
      - file_descriptors

  # 应用指标监控
  application_metrics:
    enabled: true
    collection_interval: 60
    metrics:
      - active_tasks
      - message_throughput
      - training_duration
      - model_accuracy
      - gradient_size
      - error_rate

  # 健康检查
  health_checks:
    enabled: true
    check_interval: 30
    checks:
      - websocket_connection
      - database_connection
      - memory_usage
      - disk_space
      - task_status

  # 告警配置
  alerting:
    enabled: true
    webhook_url: "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"
    email_notifications: true

    rules:
      - name: "High CPU Usage"
        condition: "cpu_usage > 80"
        duration: "5m"
        severity: "warning"

      - name: "High Memory Usage"
        condition: "memory_usage > 90"
        duration: "2m"
        severity: "critical"

      - name: "Connection Lost"
        condition: "websocket_connected == false"
        duration: "1m"
        severity: "critical"

      - name: "Training Failure Rate"
        condition: "task_failure_rate > 10"
        duration: "10m"
        severity: "warning"

  # Prometheus集成
  prometheus:
    enabled: true
    port: 9090
    metrics_path: "/metrics"
    scrape_interval: 15
```

#### 日志管理配置
```yaml
# logging_config.yml
logging:
  # 全局日志配置
  global:
    level: "INFO"
    format: "%(asctime)s - %(name)s - %(levelname)s - %(funcName)s:%(lineno)d - %(message)s"

  # 日志处理器
  handlers:
    # 文件处理器
    file_handler:
      enabled: true
      filename: "/var/log/federated-learning/app.log"
      max_size_mb: 100
      backup_count: 10
      rotation: "time"
      rotation_interval: "midnight"

    # 控制台处理器
    console_handler:
      enabled: true
      level: "INFO"

    # 系统日志处理器
    syslog_handler:
      enabled: true
      facility: "daemon"
      address: "/dev/log"

    # 远程日志处理器
    remote_handler:
      enabled: false
      host: "logserver.example.com"
      port: 514
      protocol: "tcp"

  # 模块特定日志级别
  loggers:
    "feduwacomm.websocket": "DEBUG"
    "feduwacomm.federated": "INFO"
    "feduwacomm.database": "WARNING"
    "urllib3": "WARNING"
    "requests": "WARNING"

  # 日志过滤
  filters:
    # 敏感信息过滤
    sensitive_filter:
      enabled: true
      patterns:
        - "access_token"
        - "password"
        - "api_key"
        - "secret"

    # 频率限制
    rate_limit_filter:
      enabled: true
      max_rate: 100  # 每秒最大日志条数
      burst: 50

  # 结构化日志
  structured_logging:
    enabled: true
    format: "json"
    include_fields:
      - timestamp
      - level
      - logger
      - message
      - module
      - function
      - line_number
      - thread_id
      - task_id  # 如果存在
```

## 4. 运维管理

### 4.1 服务管理

#### 服务管理脚本
```bash
#!/bin/bash
# manage_service.sh

SERVICE_NAME="federated-learning-vm"
CONFIG_DIR="/opt/federated-learning-vm/config"
LOG_DIR="/var/log/federated-learning"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}📊 Service Status${NC}"
    systemctl status $SERVICE_NAME --no-pager
}

print_logs() {
    echo -e "${BLUE}📋 Recent Logs${NC}"
    journalctl -u $SERVICE_NAME -n 20 --no-pager
}

start_service() {
    echo -e "${GREEN}🚀 Starting $SERVICE_NAME...${NC}"
    sudo systemctl start $SERVICE_NAME
    sleep 2
    print_status
}

stop_service() {
    echo -e "${RED}🛑 Stopping $SERVICE_NAME...${NC}"
    sudo systemctl stop $SERVICE_NAME
    sleep 2
    print_status
}

restart_service() {
    echo -e "${YELLOW}🔄 Restarting $SERVICE_NAME...${NC}"
    sudo systemctl restart $SERVICE_NAME
    sleep 2
    print_status
}

reload_config() {
    echo -e "${YELLOW}📝 Reloading configuration...${NC}"
    sudo systemctl reload $SERVICE_NAME || restart_service
    sleep 2
    print_status
}

enable_service() {
    echo -e "${GREEN}✅ Enabling $SERVICE_NAME for auto-start...${NC}"
    sudo systemctl enable $SERVICE_NAME
}

disable_service() {
    echo -e "${RED}❌ Disabling $SERVICE_NAME auto-start...${NC}"
    sudo systemctl disable $SERVICE_NAME
}

health_check() {
    echo -e "${BLUE}🏥 Performing health check...${NC}"

    # 检查服务状态
    if systemctl is-active --quiet $SERVICE_NAME; then
        echo -e "${GREEN}✅ Service is running${NC}"
    else
        echo -e "${RED}❌ Service is not running${NC}"
        return 1
    fi

    # 检查配置文件
    if [ -f "$CONFIG_DIR/config.yml" ]; then
        echo -e "${GREEN}✅ Configuration file exists${NC}"
    else
        echo -e "${RED}❌ Configuration file missing${NC}"
        return 1
    fi

    # 检查日志目录
    if [ -d "$LOG_DIR" ]; then
        echo -e "${GREEN}✅ Log directory exists${NC}"
    else
        echo -e "${RED}❌ Log directory missing${NC}"
        return 1
    fi

    # 检查最近的错误
    error_count=$(journalctl -u $SERVICE_NAME --since "1 hour ago" -p err --no-pager | wc -l)
    if [ $error_count -eq 0 ]; then
        echo -e "${GREEN}✅ No recent errors${NC}"
    else
        echo -e "${YELLOW}⚠️ Found $error_count errors in the last hour${NC}"
    fi

    echo -e "${GREEN}🎉 Health check completed${NC}"
}

view_config() {
    echo -e "${BLUE}📄 Current Configuration${NC}"
    if [ -f "$CONFIG_DIR/config.yml" ]; then
        cat "$CONFIG_DIR/config.yml"
    else
        echo -e "${RED}❌ Configuration file not found${NC}"
    fi
}

edit_config() {
    echo -e "${YELLOW}📝 Editing configuration...${NC}"
    sudo nano "$CONFIG_DIR/config.yml"
    echo -e "${YELLOW}🔄 Configuration updated. Restart service to apply changes.${NC}"
}

show_help() {
    echo -e "${BLUE}🔧 Federated Learning VM Service Management${NC}"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  start       Start the service"
    echo "  stop        Stop the service"
    echo "  restart     Restart the service"
    echo "  reload      Reload configuration"
    echo "  status      Show service status"
    echo "  logs        Show recent logs"
    echo "  enable      Enable auto-start"
    echo "  disable     Disable auto-start"
    echo "  health      Perform health check"
    echo "  config      View current configuration"
    echo "  edit        Edit configuration"
    echo "  help        Show this help"
    echo ""
}

# 主程序
case "$1" in
    start)
        start_service
        ;;
    stop)
        stop_service
        ;;
    restart)
        restart_service
        ;;
    reload)
        reload_config
        ;;
    status)
        print_status
        ;;
    logs)
        print_logs
        ;;
    enable)
        enable_service
        ;;
    disable)
        disable_service
        ;;
    health)
        health_check
        ;;
    config)
        view_config
        ;;
    edit)
        edit_config
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo -e "${RED}❌ Unknown command: $1${NC}"
        show_help
        exit 1
        ;;
esac
```

### 4.2 备份和恢复

#### 备份脚本
```bash
#!/bin/bash
# backup.sh

set -e

# 配置
BACKUP_DIR="/backup/federated-learning"
PROJECT_DIR="/opt/federated-learning-vm"
DB_NAME="feduwacomm"
DB_USER="feduwacomm"
RETENTION_DAYS=30

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 生成时间戳
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_PATH="$BACKUP_DIR/backup_$TIMESTAMP"

echo "🗄️ Starting backup process..."
echo "Backup location: $BACKUP_PATH"

# 创建备份目录
mkdir -p "$BACKUP_PATH"

# 1. 备份配置文件
echo "📁 Backing up configuration files..."
tar -czf "$BACKUP_PATH/config.tar.gz" -C "$PROJECT_DIR" config/
echo "✅ Configuration backup completed"

# 2. 备份应用数据
echo "💾 Backing up application data..."
if [ -d "$PROJECT_DIR/data" ]; then
    tar -czf "$BACKUP_PATH/data.tar.gz" -C "$PROJECT_DIR" data/
    echo "✅ Application data backup completed"
else
    echo "ℹ️ No application data directory found"
fi

# 3. 备份日志文件（最近30天）
echo "📋 Backing up recent logs..."
find /var/log/federated-learning -name "*.log" -mtime -30 -exec tar -czf "$BACKUP_PATH/logs.tar.gz" {} +
echo "✅ Log backup completed"

# 4. 备份数据库
echo "🗃️ Backing up database..."
if command -v mysqldump &> /dev/null; then
    mysqldump -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" | gzip > "$BACKUP_PATH/database.sql.gz"
    echo "✅ Database backup completed"
else
    echo "⚠️ mysqldump not found, skipping database backup"
fi

# 5. 创建备份清单
echo "📝 Creating backup manifest..."
cat > "$BACKUP_PATH/manifest.txt" <<EOF
Backup Created: $(date)
Backup Location: $BACKUP_PATH
Server Hostname: $(hostname)
Backup Components:
- Configuration Files: config.tar.gz
- Application Data: data.tar.gz (if exists)
- Log Files: logs.tar.gz
- Database: database.sql.gz (if mysqldump available)

Backup Size:
$(du -sh "$BACKUP_PATH")
EOF

# 6. 清理旧备份
echo "🧹 Cleaning up old backups..."
find "$BACKUP_DIR" -name "backup_*" -type d -mtime +$RETENTION_DAYS -exec rm -rf {} \;
echo "✅ Old backup cleanup completed"

# 7. 压缩整个备份
echo "📦 Compressing backup..."
cd "$BACKUP_DIR"
tar -czf "backup_$TIMESTAMP.tar.gz" "backup_$TIMESTAMP/"
rm -rf "backup_$TIMESTAMP/"
echo "✅ Backup compression completed"

echo "🎉 Backup process completed successfully!"
echo "Backup file: $BACKUP_DIR/backup_$TIMESTAMP.tar.gz"
```

#### 恢复脚本
```bash
#!/bin/bash
# restore.sh

set -e

if [ -z "$1" ]; then
    echo "❌ Please specify backup file"
    echo "Usage: $0 <backup_file.tar.gz>"
    exit 1
fi

BACKUP_FILE="$1"
PROJECT_DIR="/opt/federated-learning-vm"
SERVICE_NAME="federated-learning-vm"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ Backup file not found: $BACKUP_FILE"
    exit 1
fi

echo "🔄 Starting restore process..."
echo "Backup file: $BACKUP_FILE"

# 停止服务
echo "🛑 Stopping service..."
sudo systemctl stop "$SERVICE_NAME" || true

# 创建临时目录
TEMP_DIR=$(mktemp -d)
echo "📁 Extracting backup to: $TEMP_DIR"

# 解压备份
tar -xzf "$BACKUP_FILE" -C "$TEMP_DIR"

# 找到备份目录
RESTORE_DIR=$(find "$TEMP_DIR" -name "backup_*" -type d | head -1)

if [ -z "$RESTORE_DIR" ]; then
    echo "❌ Invalid backup file format"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo "📋 Backup manifest:"
cat "$RESTORE_DIR/manifest.txt"
echo ""

read -p "Continue with restore? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Restore cancelled"
    rm -rf "$TEMP_DIR"
    exit 1
fi

# 备份当前配置
echo "💾 Backing up current configuration..."
sudo cp -r "$PROJECT_DIR/config" "$PROJECT_DIR/config.backup.$(date +%Y%m%d_%H%M%S)"

# 恢复配置文件
if [ -f "$RESTORE_DIR/config.tar.gz" ]; then
    echo "📁 Restoring configuration files..."
    sudo tar -xzf "$RESTORE_DIR/config.tar.gz" -C "$PROJECT_DIR"
    echo "✅ Configuration restored"
fi

# 恢复应用数据
if [ -f "$RESTORE_DIR/data.tar.gz" ]; then
    echo "💾 Restoring application data..."
    sudo tar -xzf "$RESTORE_DIR/data.tar.gz" -C "$PROJECT_DIR"
    echo "✅ Application data restored"
fi

# 恢复数据库
if [ -f "$RESTORE_DIR/database.sql.gz" ]; then
    echo "🗃️ Restoring database..."
    read -p "Enter database password: " -s DB_PASSWORD
    echo

    if command -v mysql &> /dev/null; then
        zcat "$RESTORE_DIR/database.sql.gz" | mysql -u feduwacomm -p"$DB_PASSWORD" feduwacomm
        echo "✅ Database restored"
    else
        echo "⚠️ mysql client not found, skipping database restore"
    fi
fi

# 设置权限
echo "🔒 Setting permissions..."
sudo chown -R fedlearn:fedlearn "$PROJECT_DIR"

# 清理临时文件
rm -rf "$TEMP_DIR"

# 启动服务
echo "🚀 Starting service..."
sudo systemctl start "$SERVICE_NAME"

# 检查服务状态
sleep 3
if systemctl is-active --quiet "$SERVICE_NAME"; then
    echo "✅ Service started successfully"
else
    echo "❌ Service failed to start"
    echo "Check logs: sudo journalctl -u $SERVICE_NAME -n 20"
    exit 1
fi

echo "🎉 Restore process completed successfully!"
```

### 4.3 更新和升级

#### 自动更新脚本
```bash
#!/bin/bash
# update.sh

set -e

SERVICE_NAME="federated-learning-vm"
PROJECT_DIR="/opt/federated-learning-vm"
BACKUP_DIR="/backup/federated-learning"
UPDATE_LOG="/var/log/federated-learning/update.log"

# 日志函数
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$UPDATE_LOG"
}

log "🔄 Starting update process..."

# 检查是否以root身份运行
if [ "$EUID" -ne 0 ]; then
    log "❌ Please run as root or with sudo"
    exit 1
fi

# 创建更新前备份
log "💾 Creating pre-update backup..."
./backup.sh

# 停止服务
log "🛑 Stopping service..."
systemctl stop "$SERVICE_NAME"

# 切换到项目目录
cd "$PROJECT_DIR"

# 更新代码
if [ -d ".git" ]; then
    log "📥 Pulling latest code from repository..."
    sudo -u fedlearn git fetch origin
    sudo -u fedlearn git checkout main
    sudo -u fedlearn git pull origin main
else
    log "⚠️ Not a git repository, skipping code update"
fi

# 检查依赖更新
log "📦 Checking for dependency updates..."
sudo -u fedlearn ./venv/bin/pip install --upgrade -r requirements.txt

# 运行数据库迁移（如果有）
if [ -f "migrate.py" ]; then
    log "🗃️ Running database migrations..."
    sudo -u fedlearn ./venv/bin/python migrate.py
fi

# 重新安装项目包
log "📦 Reinstalling project package..."
sudo -u fedlearn ./venv/bin/pip install -e .

# 验证配置
log "✅ Validating configuration..."
if ! sudo -u fedlearn ./venv/bin/python -c "from feduwacomm.config import load_config; load_config('config/config.yml')"; then
    log "❌ Configuration validation failed"
    exit 1
fi

# 启动服务
log "🚀 Starting service..."
systemctl start "$SERVICE_NAME"

# 等待服务启动
sleep 5

# 验证服务状态
if systemctl is-active --quiet "$SERVICE_NAME"; then
    log "✅ Service started successfully"
else
    log "❌ Service failed to start after update"
    log "🔄 Attempting to restore from backup..."

    # 这里可以调用恢复脚本
    # ./restore.sh "$BACKUP_DIR/latest_backup.tar.gz"

    exit 1
fi

# 运行健康检查
log "🏥 Running post-update health check..."
if ./manage_service.sh health; then
    log "✅ Health check passed"
else
    log "⚠️ Health check failed, please investigate"
fi

log "🎉 Update process completed successfully!"
```

---

**总结**: 至此，Python VM WebSocket 协议重构的完整指导文档已经拆分完成。现在需要更新 README.md 设置完整的文档目录导航。
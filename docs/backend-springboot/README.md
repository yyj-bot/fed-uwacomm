# 水声联邦学习后端服务 (FedUWAComm Backend)

## 项目简介

水声联邦学习后端服务是基于Spring Boot 3.4.4构建的RESTful API服务，为水声联邦学习系统提供用户管理、权限控制、WebSocket通信等核心功能。

## 技术栈

- **Java**: 17
- **Spring Boot**: 3.4.4
- **Spring Security**: 安全认证
- **MyBatis**: 数据持久化
- **MySQL**: 数据库
- **WebSocket**: 实时通信
- **JWT**: 身份认证
- **Maven**: 项目管理

## 项目结构

```
backend-springboot/
├── feduwacomm-common/          # 公共模块
│   ├── BaseContext.java        # 基础上下文
│   ├── Result.java             # 统一响应结果
│   ├── exception/              # 异常处理
│   └── utils/                  # 工具类
├── feduwacomm-pojo/           # 数据传输对象
│   ├── dto/                   # 数据传输对象
│   ├── entity/                # 实体类
│   └── vo/                    # 视图对象
├── feduwacomm-server/         # 主服务模块
│   ├── config/                # 配置类
│   ├── controller/            # 控制器
│   ├── service/               # 服务层
│   ├── mapper/                # 数据访问层
│   └── interceptor/           # 拦截器
└── pom.xml                    # 父级POM
```

## 核心功能

### 1. 用户管理
- 用户注册、登录、注销
- 用户信息查询、更新、删除
- 用户权限管理
- 用户锁定/解锁

### 2. 权限控制
- JWT令牌认证
- 基于角色的权限控制
- 权限拦截器

### 3. WebSocket通信
- 实时消息推送
- 联邦学习状态同步
- 客户端连接管理

### 4. 系统监控
- 健康检查接口
- 系统日志记录
- 性能监控

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE feduwacomm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置数据库连接（`application.yml`）：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/feduwacomm?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 安装运行

#### 方法一：使用批处理文件（Windows）

```bash
# 在backend-springboot目录下运行
run.bat
```

#### 方法二：使用Maven命令

```bash
# 清理并编译
mvn clean compile

# 启动服务
mvn -pl feduwacomm-server spring-boot:run
```

#### 方法三：打包运行

```bash
# 打包
mvn clean package

# 运行jar包
java -jar feduwacomm-server/target/feduwacomm-server-1.0.0.jar
```

### 服务访问

- **API基础地址**: http://localhost:8080/api
- **健康检查**: http://localhost:8080/api/health
- **用户管理**: http://localhost:8080/api/users

## API接口

### 用户管理接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/users` | 获取所有用户 |
| GET | `/api/users/{id}` | 根据ID获取用户 |
| POST | `/api/users` | 创建用户 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户 |
| GET | `/api/users/search` | 搜索用户 |

### WebSocket接口

- **连接地址**: ws://localhost:8080/api/websocket
- **消息格式**: JSON格式的WebSocket消息

## 配置说明

### 主要配置项

```yaml
server:
  port: 8080                    # 服务端口
  servlet:
    context-path: /api          # 上下文路径

spring:
  datasource:                   # 数据库配置
    url: jdbc:mysql://localhost:3306/feduwacomm
    username: root
    password: your_password
  
  jackson:                      # JSON配置
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis:                        # MyBatis配置
  configuration:
    map-underscore-to-camel-case: true
  mapper-locations: classpath:mapper/*.xml

logging:                        # 日志配置
  level:
    com.feduwacomm: DEBUG
```

## 开发指南

### 添加新的控制器

1. 在`controller`包下创建新的控制器类
2. 使用`@RestController`和`@RequestMapping`注解
3. 实现相应的业务逻辑

### 添加新的服务

1. 在`service`包下创建服务接口
2. 在`service/impl`包下实现服务类
3. 使用`@Service`注解标记

### 添加新的数据访问

1. 在`mapper`包下创建Mapper接口
2. 在`resources/mapper`下创建对应的XML文件
3. 使用`@Mapper`注解标记

## 部署说明

### 生产环境部署

1. 修改`application.yml`中的数据库配置
2. 配置日志输出路径
3. 设置适当的内存参数：
```bash
java -Xms512m -Xmx1024m -jar feduwacomm-server-1.0.0.jar
```

### Docker部署

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/feduwacomm-server-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 故障排除

### 常见问题

1. **端口占用**: 修改`application.yml`中的`server.port`
2. **数据库连接失败**: 检查数据库配置和网络连接
3. **JWT令牌过期**: 检查JWT配置和令牌有效期

### 日志查看

```bash
# 查看实时日志
tail -f logs/feduwacomm.log
```

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 许可证

本项目采用MIT许可证。

## 联系方式

- 项目维护者: FedUWAComm Team
- 邮箱: [联系邮箱]
- 项目地址: [项目地址]

---

**注意**: 首次运行前请确保数据库已正确配置并创建相应的表结构。

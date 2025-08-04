# FedUWAComm Backend

水声联邦学习后端服务

## 项目简介

这是一个基于Spring Boot 3.4.4的水声联邦学习后端服务，采用多模块架构设计，使用MyBatis作为数据访问层，提供基础的项目框架。

## 技术栈

- **Spring Boot 3.4.4** - 主框架
- **Spring Security** - 安全框架
- **MyBatis 3.0.3** - 数据访问层
- **MySQL** - 主数据库
- **Lombok** - 代码简化
- **Maven** - 项目管理

## 项目结构

```
backend-springboot/
├── feduwacomm-parent/          # 父级项目
│   └── pom.xml                 # 父级POM文件
├── feduwacomm-common/          # 通用模块
│   ├── pom.xml
│   └── src/main/java/com/feduwacomm/
│       └── exception/          # 异常处理
├── feduwacomm-pojo/            # 数据对象模块
│   ├── pom.xml
│   └── src/main/java/com/feduwacomm/
│       └── (待添加实体类和DTO)
└── feduwacomm-server/          # 服务端模块
    ├── pom.xml
    ├── src/main/java/com/feduwacomm/
    │   ├── FedUWACommApplication.java  # 主应用程序类
    │   ├── config/             # 配置类
    │   ├── controller/         # 控制器
    │   ├── service/            # 服务层（待添加）
    │   └── mapper/             # MyBatis Mapper接口（待添加）
    └── src/main/resources/
        ├── application.yml     # 配置文件
        └── mapper/             # MyBatis XML映射文件（待添加）
```

## 模块说明

### feduwacomm-common
- 通用工具类
- 异常处理
- 常量定义
- 通用配置

### feduwacomm-pojo
- 实体类（Entity）- 待添加
- 数据传输对象（DTO）- 待添加

### feduwacomm-server
- 主应用程序类
- 控制器（Controller）
- 服务层（Service）- 待添加
- MyBatis Mapper接口 - 待添加
- 配置类（Config）
- 启动类

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd backend-springboot
   ```

2. **配置数据库**
   - 创建MySQL数据库：`feduwacomm`
   - 修改 `feduwacomm-server/src/main/resources/application.yml` 中的数据库连接信息

3. **运行项目**
   ```bash
   # 方式1：使用启动脚本
   run.bat
   
   # 方式2：使用Maven命令
   mvn -pl feduwacomm-server spring-boot:run
   ```

4. **访问服务**
   - 服务地址：http://localhost:8080/api
   - 健康检查：http://localhost:8080/api/health

## API接口

### 健康检查
- `GET /api/health` - 健康检查

## 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/feduwacomm
    username: root
    password: password
```

### MyBatis配置
```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true
  mapper-locations: classpath:mapper/*.xml
```

## 开发指南

### 添加新的实体类
1. 在 `feduwacomm-pojo` 模块的 `entity` 包中创建实体类
2. 在 `feduwacomm-pojo` 模块的 `dto` 包中创建数据传输对象
3. 在 `feduwacomm-server` 模块的 `mapper` 包中创建Mapper接口
4. 在 `feduwacomm-server` 模块的 `service` 包中创建服务接口和实现
5. 在 `feduwacomm-server` 模块的 `controller` 包中创建控制器

### MyBatis使用说明
- 支持XML映射文件
- 支持注解方式编写SQL
- 支持驼峰命名转换
- 支持动态SQL和复杂查询

### 运行测试
```bash
mvn test
```

### 打包部署
```bash
mvn clean package
java -jar feduwacomm-server/target/feduwacomm-server-1.0.0.jar
```

## 模块依赖关系

```
feduwacomm-server
    └── feduwacomm-pojo
            └── feduwacomm-common
```

## 许可证

本项目采用 MIT 许可证。

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 联系方式

- 项目维护者：FedUWAComm Team
- 邮箱：[your-email@example.com]
- 项目地址：[repository-url]

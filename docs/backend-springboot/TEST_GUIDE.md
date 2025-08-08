# SpringBoot后端测试指南

本文档说明如何测试生成的用户管理示例接口。

## 1. 数据库准备

### 1.1 创建数据库和表
```sql
-- 执行以下SQL脚本创建数据库和表
-- 文件位置: feduwacomm-server/src/main/resources/sql/init.sql
```

### 1.2 数据库配置
确保 `application.yml` 中的数据库配置正确：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/feduwacomm_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: NBNB
```

## 2. 启动服务

### 2.1 使用Maven启动
```bash
cd backend-springboot
mvn -pl feduwacomm-server spring-boot:run
```

### 2.2 使用启动脚本
```bash
cd backend-springboot
./run.bat
```

### 2.3 验证服务启动
访问: http://localhost:8080/api/health

## 3. API接口测试

### 3.1 获取所有用户
```bash
GET http://localhost:8080/api/users
```

### 3.2 根据ID获取用户
```bash
GET http://localhost:8080/api/users/1
```

### 3.3 创建用户
```bash
POST http://localhost:8080/api/users
Content-Type: application/json

{
    "username": "new_user",
    "email": "new@example.com",
    "phone": "13800138003"
}
```

### 3.4 更新用户
```bash
PUT http://localhost:8080/api/users/1
Content-Type: application/json

{
    "username": "updated_user",
    "email": "updated@example.com",
    "phone": "13800138004",
    "status": 1
}
```

### 3.5 删除用户
```bash
DELETE http://localhost:8080/api/users/1
```

### 3.6 获取用户数量
```bash
GET http://localhost:8080/api/users/count
```

## 4. 使用Postman测试

### 4.1 导入Postman集合
可以创建一个Postman集合，包含以上所有API接口。

### 4.2 测试流程
1. 先测试获取用户列表，确认数据库连接正常
2. 创建新用户，验证插入功能
3. 查询刚创建的用户，验证查询功能
4. 更新用户信息，验证更新功能
5. 删除用户，验证删除功能

## 5. 响应格式

所有接口都返回统一的JSON格式：
```json
{
    "success": true,
    "data": {...},
    "message": "操作成功"
}
```

## 6. 错误处理

如果出现错误，响应格式为：
```json
{
    "success": false,
    "message": "错误信息"
}
```

## 7. 常见问题

### 7.1 数据库连接失败
- 检查MySQL服务是否启动
- 检查数据库配置是否正确
- 检查数据库和表是否已创建

### 7.2 端口占用
- 检查8080端口是否被占用
- 可以在application.yml中修改端口

### 7.3 跨域问题
- 已配置@CrossOrigin注解，支持跨域访问

## 8. 项目结构

```
backend-springboot/
├── feduwacomm-pojo/           # 数据对象模块
│   └── src/main/java/com/feduwacomm/
│       ├── entity/            # 实体类
│       │   └── User.java
│       └── dto/               # 数据传输对象
│           └── UserDTO.java
├── feduwacomm-server/         # 服务端模块
│   └── src/main/java/com/feduwacomm/
│       ├── controller/        # 控制器
│       │   └── UserController.java
│       ├── service/           # 服务层
│       │   ├── UserService.java
│       │   └── impl/
│       │       └── UserServiceImpl.java
│       ├── mapper/            # 数据访问层
│       │   └── UserMapper.java
│       └── resources/
│           ├── application.yml # 配置文件
│           └── sql/           # SQL脚本
│               └── init.sql
```

## 9. 下一步开发

1. 添加更多业务实体（如项目、任务等）
2. 完善异常处理机制
3. 添加数据验证
4. 实现用户认证和授权
5. 添加日志记录
6. 编写单元测试 
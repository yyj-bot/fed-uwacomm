# SpringBoot后端XML映射模式测试指南

本文档说明如何使用XML映射模式测试用户管理功能。

## 1. 修改内容

### 1.1 新增文件
- `mapper/UserMapper.xml` - MyBatis XML映射文件

### 1.2 修改文件
- `UserMapper.java` - 移除注解，改为XML映射
- `MyBatisConfig.java` - 恢复XML映射文件配置
- `application.yml` - 恢复mapper-locations配置
- `UserService.java` - 添加条件查询方法
- `UserServiceImpl.java` - 实现条件查询方法
- `UserController.java` - 添加条件查询接口

## 2. XML映射的优势

### 2.1 更好的SQL管理
- SQL语句与Java代码分离
- 支持复杂的动态SQL
- 更好的可读性和维护性

### 2.2 动态SQL支持
- 条件查询（if标签）
- 批量操作（foreach标签）
- 复杂查询逻辑

### 2.3 结果映射
- 精确的字段映射
- 支持关联查询
- 类型转换控制

## 3. 数据库准备

### 3.1 创建数据库和表
```sql
-- 执行SQL脚本
-- 文件位置: feduwacomm-server/src/main/resources/sql/init.sql
```

### 3.2 数据库配置
确保 `application.yml` 中的数据库配置正确：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/feduwacomm_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: NBNB
```

## 4. 启动服务

### 4.1 使用Maven启动
```bash
cd backend-springboot
mvn -pl feduwacomm-server spring-boot:run
```

### 4.2 使用启动脚本
```bash
cd backend-springboot
./run.bat
```

## 5. API接口测试

### 5.1 基础CRUD操作
```bash
# 获取所有用户
GET http://localhost:8080/api/users

# 根据ID获取用户
GET http://localhost:8080/api/users/1

# 创建用户
POST http://localhost:8080/api/users
Content-Type: application/json
{
    "username": "new_user",
    "email": "new@example.com",
    "phone": "13800138003"
}

# 更新用户
PUT http://localhost:8080/api/users/1
Content-Type: application/json
{
    "username": "updated_user",
    "email": "updated@example.com",
    "phone": "13800138004",
    "status": 1
}

# 删除用户
DELETE http://localhost:8080/api/users/1

# 获取用户数量
GET http://localhost:8080/api/users/count
```

### 5.2 新增的条件查询接口
```bash
# 根据用户名查询
GET http://localhost:8080/api/users/search?username=admin

# 根据邮箱查询
GET http://localhost:8080/api/users/search?email=test@example.com

# 根据手机号查询
GET http://localhost:8080/api/users/search?phone=13800138000

# 组合条件查询
GET http://localhost:8080/api/users/search?username=admin&email=admin@example.com
```

## 6. XML映射文件说明

### 6.1 结果映射
```xml
<resultMap id="UserResultMap" type="com.feduwacomm.entity.User">
    <id column="id" property="id" jdbcType="BIGINT"/>
    <result column="username" property="username" jdbcType="VARCHAR"/>
    <!-- 其他字段映射 -->
</resultMap>
```

### 6.2 动态SQL
```xml
<select id="findByCondition" parameterType="map" resultMap="UserResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM users 
    WHERE status = 1
    <if test="username != null and username != ''">
        AND username LIKE CONCAT('%', #{username}, '%')
    </if>
    <!-- 其他条件 -->
</select>
```

### 6.3 插入操作
```xml
<insert id="insert" parameterType="com.feduwacomm.entity.User" 
        useGeneratedKeys="true" keyProperty="id">
    INSERT INTO users (username, email, phone, status, create_time, update_time) 
    VALUES (#{username}, #{email}, #{phone}, #{status}, #{createTime}, #{updateTime})
</insert>
```

## 7. 项目结构

```
backend-springboot/
├── feduwacomm-server/
│   └── src/main/
│       ├── java/com/feduwacomm/
│       │   ├── mapper/
│       │   │   └── UserMapper.java          # 接口定义
│       │   ├── service/
│       │   │   ├── UserService.java         # 服务接口
│       │   │   └── impl/
│       │   │       └── UserServiceImpl.java # 服务实现
│       │   └── controller/
│       │       └── UserController.java      # 控制器
│       └── resources/
│           ├── mapper/
│           │   └── UserMapper.xml           # XML映射文件
│           ├── application.yml              # 配置文件
│           └── sql/
│               └── init.sql                 # 数据库脚本
```

## 8. 与注解模式的对比

| 特性 | 注解模式 | XML模式 |
|------|----------|---------|
| SQL管理 | 与Java代码混合 | 独立文件管理 |
| 动态SQL | 有限支持 | 完整支持 |
| 可读性 | 简单SQL较好 | 复杂SQL更好 |
| 维护性 | 适合简单项目 | 适合复杂项目 |
| 学习成本 | 低 | 中等 |

## 9. 下一步开发建议

1. **添加更多动态SQL功能**
   - 分页查询
   - 排序功能
   - 批量操作

2. **完善结果映射**
   - 关联查询
   - 集合映射
   - 延迟加载

3. **优化性能**
   - 缓存配置
   - 批量插入
   - 分页优化

4. **添加更多业务实体**
   - 项目实体
   - 任务实体
   - 权限实体 
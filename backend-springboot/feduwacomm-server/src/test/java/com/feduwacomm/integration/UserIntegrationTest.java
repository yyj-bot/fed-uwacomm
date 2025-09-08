package com.feduwacomm.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.service.UserService;
import com.feduwacomm.utils.JwtUtil;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户模块集成测试类
 * 使用H2内存数据库进行集成测试，避免DDL事务问题
 * 
 * 注意：@Transactional只对DML操作有效，对DDL操作无效
 * H2数据库的DDL操作同样会隐式提交，但由于是内存数据库，每次测试后会重置
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserIntegrationTest {

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private UserService userService;

        @Autowired
        private UserMapper userMapper;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private JwtUtil jwtUtil;

        private MockMvc mockMvc;
        
        // 管理员Token缓存
        private String adminToken;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }

        /**
         * 获取管理员Token用于需要认证的API调用
         */
        private String getAdminToken() throws Exception {
                if (adminToken != null) {
                        return adminToken;
                }

                // 创建管理员账户
                UserRegisterDTO adminRegister = UserRegisterDTO.builder()
                                .username("testadmin")
                                .email("testadmin@test.com")
                                .password("admin123")
                                .confirmPassword("admin123")
                                .build();

                // 注册管理员
                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(adminRegister)))
                                .andExpect(status().isOk());

                // 手动设置为管理员角色（绕过权限控制）
                User user = userMapper.selectByLoginIdentifier("testadmin");
                userMapper.updateRole(user.getId(), "ADMIN", "SYSTEM");

                // 使用管理员账户登录获取Token
                UserLoginDTO adminLogin = UserLoginDTO.builder()
                                .loginIdentifier("testadmin")
                                .password("admin123")
                                .build();

                MvcResult loginResult = mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(adminLogin)))
                                .andExpect(status().isOk())
                                .andReturn();

                String loginResponse = loginResult.getResponse().getContentAsString();
                JsonNode jsonNode = objectMapper.readTree(loginResponse);
                adminToken = jsonNode.get("data").get("token").asText();
                
                return adminToken;
        }

        /**
         * 创建认证请求头
         */
        private String getBearerToken(String token) {
                return "Bearer " + token;
        }

        @Test
        void testUserRegistrationAndLoginFlow() throws Exception {
                // 1. 测试用户注册
                UserRegisterDTO registerDTO = UserRegisterDTO.builder()
                                .username("integrationtest")
                                .email("integration@test.com")
                                .password("password123")
                                .confirmPassword("password123")
                                .build();

                // 执行注册请求
                String registerResponse = mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("注册成功"))
                                .andExpect(jsonPath("$.data.username").value("integrationtest"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                // 2. 测试用户登录
                UserLoginDTO loginDTO = UserLoginDTO.builder()
                                .loginIdentifier("integrationtest")
                                .password("password123")
                                .build();

                String loginResponse = mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("登录成功"))
                                .andExpect(jsonPath("$.data.token").exists())
                                .andExpect(jsonPath("$.data.user.username").value("integrationtest"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                // 3. 验证用户已创建 - 通过数据库验证
                User createdUser = userMapper.selectByLoginIdentifier("integrationtest");
                assertNotNull(createdUser, "用户应该已成功创建");
                assertEquals("integrationtest", createdUser.getUsername());
                assertEquals("integration@test.com", createdUser.getEmail());
                assertEquals("VIEWER", createdUser.getRole());
                assertEquals("ACTIVE", createdUser.getStatus());
        }

        @Test
        void testUserManagementFlow() throws Exception {
                String token = getAdminToken();

                // 1. 创建测试用户
                UserCreateDTO createDTO = UserCreateDTO.builder()
                                .username("adminuser")
                                .email("admin@test.com")
                                .password("password123")
                                .role("ADMIN")
                                .build();

                mockMvc.perform(post("/api/admin/user/create")
                                .header("Authorization", getBearerToken(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("创建成功"))
                                .andExpect(jsonPath("$.data.username").value("adminuser"));

                // 2. 获取用户列表
                mockMvc.perform(get("/api/admin/user/list")
                                .header("Authorization", getBearerToken(token))
                                .param("page", "1")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.total").exists())
                                .andExpect(jsonPath("$.data.list").isArray());

                // 3. 获取用户详情 - 先通过数据库获取用户ID
                User createdUser = userMapper.selectByLoginIdentifier("adminuser");
                assertNotNull(createdUser, "管理员用户应该已创建");

                mockMvc.perform(get("/api/admin/user/{userId}", createdUser.getId())
                                .header("Authorization", getBearerToken(token)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.userId").value(createdUser.getId()))
                                .andExpect(jsonPath("$.data.username").value("adminuser"));

                // 4. 更新用户信息
                UserAdminUpdateDTO updateDTO = UserAdminUpdateDTO.builder()
                                .username("updatedadmin")
                                .email("updated@test.com")
                                .role("RESEARCHER")
                                .status("ACTIVE")
                                .build();

                mockMvc.perform(put("/api/admin/user/{userId}", createdUser.getId())
                                .header("Authorization", getBearerToken(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("更新成功"))
                                .andExpect(jsonPath("$.data.username").value("updatedadmin"));
        }

        @Test
        void testPasswordManagementFlow() throws Exception {
                // 简化测试：只验证密码修改API的基本功能
                // 使用管理员Token来测试密码修改功能
                String adminToken = getAdminToken();
                
                // 创建测试用户（通过管理员接口）
                UserCreateDTO createDTO = UserCreateDTO.builder()
                                .username("pwdtest")
                                .email("pwdtest@test.com")
                                .password("oldpass123")
                                .role("VIEWER")
                                .build();

                mockMvc.perform(post("/api/admin/user/create")
                                .header("Authorization", getBearerToken(adminToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));

                // 验证用户创建成功
                User createdUser = userMapper.selectByLoginIdentifier("pwdtest");
                assertNotNull(createdUser, "密码测试用户应该已创建");

                // 使用新用户登录获取Token
                UserLoginDTO loginDTO = UserLoginDTO.builder()
                                .loginIdentifier("pwdtest")
                                .password("oldpass123")
                                .build();

                MvcResult loginResult = mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("登录成功"))
                                .andReturn();

                String loginResponse = loginResult.getResponse().getContentAsString();
                JsonNode jsonNode = objectMapper.readTree(loginResponse);
                String userToken = jsonNode.get("data").get("token").asText();

                // 测试修改密码
                PasswordChangeDTO passwordDTO = PasswordChangeDTO.builder()
                                .oldPassword("oldpass123")
                                .newPassword("newpass123")
                                .confirmPassword("newpass123")
                                .build();

                mockMvc.perform(put("/api/user/password")
                                .header("Authorization", getBearerToken(userToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(passwordDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("密码修改成功"));
        }

        @Test
        void testUserLockAndUnlockFlow() throws Exception {
                String token = getAdminToken();

                // 1. 创建测试用户
                UserCreateDTO createDTO = UserCreateDTO.builder()
                                .username("locktest")
                                .email("lock@test.com")
                                .password("password123")
                                .role("VIEWER")
                                .build();

                mockMvc.perform(post("/api/admin/user/create")
                                .header("Authorization", getBearerToken(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createDTO)))
                                .andExpect(status().isOk());

                User createdUser = userMapper.selectByLoginIdentifier("locktest");
                assertNotNull(createdUser, "锁定测试用户应该已创建");

                // 2. 锁定用户
                UserLockDTO lockDTO = UserLockDTO.builder()
                                .duration(3600)
                                .build();

                mockMvc.perform(post("/api/admin/user/{userId}/lock", createdUser.getId())
                                .header("Authorization", getBearerToken(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(lockDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("用户已锁定"))
                                .andExpect(jsonPath("$.data.userId").value(createdUser.getId()));

                // 3. 解锁用户
                mockMvc.perform(post("/api/admin/user/{userId}/unlock", createdUser.getId())
                                .header("Authorization", getBearerToken(token)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("用户已解锁"))
                                .andExpect(jsonPath("$.data.userId").value(createdUser.getId()));
        }

        @Test
        void testPermissionManagementFlow() throws Exception {
                String token = getAdminToken();

                // 1. 创建测试用户
                UserCreateDTO createDTO = UserCreateDTO.builder()
                                .username("permissiontest")
                                .email("permission@test.com")
                                .password("password123")
                                .role("VIEWER")
                                .build();

                mockMvc.perform(post("/api/admin/user/create")
                                .header("Authorization", getBearerToken(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createDTO)))
                                .andExpect(status().isOk());

                User createdUser = userMapper.selectByLoginIdentifier("permissiontest");
                assertNotNull(createdUser, "权限测试用户应该已创建");

                // 2. 授予权限
                PermissionGrantDTO grantDTO = PermissionGrantDTO.builder()
                                .resourceType("VM")
                                .resourceId("vm_1234567890")
                                .permission("READ")
                                .expiresAt(java.time.LocalDateTime.now().plusDays(30))
                                .build();

                String grantResponse = mockMvc.perform(post("/api/admin/user/{userId}/permissions", createdUser.getId())
                                .header("Authorization", getBearerToken(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(grantDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("权限授予成功"))
                                .andExpect(jsonPath("$.data.permissionId").exists())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                // 3. 获取用户权限
                mockMvc.perform(get("/api/admin/user/{userId}/permissions", createdUser.getId())
                                .header("Authorization", getBearerToken(token)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("获取成功"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].resourceType").value("SYSTEM"))
                                .andExpect(jsonPath("$.data[0].permission").value("read"));
        }

        @Test
        void testErrorHandling() throws Exception {
                // 1. 测试重复注册
                UserRegisterDTO registerDTO = UserRegisterDTO.builder()
                                .username("errortest")
                                .email("error@test.com")
                                .password("password123")
                                .confirmPassword("password123")
                                .build();

                // 第一次注册
                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andExpect(status().isOk());

                // 第二次注册（应该失败）
                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(409));

                // 2. 测试登录失败
                UserLoginDTO wrongLoginDTO = UserLoginDTO.builder()
                                .loginIdentifier("errortest")
                                .password("wrongpassword")
                                .build();

                mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrongLoginDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(401));

                // 3. 测试获取不存在的用户 - 需要认证Token
                String token = getAdminToken();
                mockMvc.perform(get("/api/admin/user/nonexistent-user")
                                .header("Authorization", getBearerToken(token)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        void testValidationErrors() throws Exception {
                // 1. 测试密码不匹配
                UserRegisterDTO invalidRegisterDTO = UserRegisterDTO.builder()
                                .username("validationtest")
                                .email("validation@test.com")
                                .password("password123")
                                .confirmPassword("differentpassword")
                                .build();

                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRegisterDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(400));

                // 2. 测试邮箱格式错误
                UserRegisterDTO invalidEmailDTO = UserRegisterDTO.builder()
                                .username("emailtest")
                                .email("invalid-email")
                                .password("password123")
                                .confirmPassword("password123")
                                .build();

                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidEmailDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(400));
        }
}
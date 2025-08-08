package com.feduwacomm.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.service.UserService;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户模块集成测试类
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private UserService userService;

        @Autowired
        private UserMapper userMapper;

        @Autowired
        private ObjectMapper objectMapper;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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

                // 3. 验证用户已创建
                User createdUser = userMapper.selectByLoginIdentifier("integrationtest");
                assertNotNull(createdUser);
                assertEquals("integrationtest", createdUser.getUsername());
                assertEquals("integration@test.com", createdUser.getEmail());
                assertEquals("VIEWER", createdUser.getRole());
                assertEquals("ACTIVE", createdUser.getStatus());
        }

        @Test
        void testUserManagementFlow() throws Exception {
                // 1. 创建测试用户
                UserCreateDTO createDTO = UserCreateDTO.builder()
                                .username("adminuser")
                                .email("admin@test.com")
                                .password("password123")
                                .role("ADMIN")
                                .build();

                mockMvc.perform(post("/api/user/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("创建成功"))
                                .andExpect(jsonPath("$.data.username").value("adminuser"));

                // 2. 获取用户列表
                mockMvc.perform(get("/api/user/list")
                                .param("page", "1")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.total").exists())
                                .andExpect(jsonPath("$.data.list").isArray());

                // 3. 获取用户详情
                User createdUser = userMapper.selectByLoginIdentifier("adminuser");
                assertNotNull(createdUser);

                mockMvc.perform(get("/api/user/{userId}", createdUser.getId()))
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

                mockMvc.perform(put("/api/user/{userId}", createdUser.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("更新成功"))
                                .andExpect(jsonPath("$.data.username").value("updatedadmin"));
        }

        @Test
        void testPasswordManagementFlow() throws Exception {
                // 1. 创建测试用户
                UserRegisterDTO registerDTO = UserRegisterDTO.builder()
                                .username("passwordtest")
                                .email("password@test.com")
                                .password("oldpassword")
                                .confirmPassword("oldpassword")
                                .build();

                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andExpect(status().isOk());

                // 2. 测试修改密码
                PasswordChangeDTO passwordDTO = PasswordChangeDTO.builder()
                                .oldPassword("oldpassword")
                                .newPassword("newpassword123")
                                .confirmPassword("newpassword123")
                                .build();

                mockMvc.perform(put("/api/user/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(passwordDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("密码修改成功"));
        }

        @Test
        void testUserLockAndUnlockFlow() throws Exception {
                // 1. 创建测试用户
                UserCreateDTO createDTO = UserCreateDTO.builder()
                                .username("locktest")
                                .email("lock@test.com")
                                .password("password123")
                                .role("VIEWER")
                                .build();

                mockMvc.perform(post("/api/user/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createDTO)))
                                .andExpect(status().isOk());

                User createdUser = userMapper.selectByLoginIdentifier("locktest");
                assertNotNull(createdUser);

                // 2. 锁定用户
                UserLockDTO lockDTO = UserLockDTO.builder()
                                .duration(3600)
                                .build();

                mockMvc.perform(post("/api/user/{userId}/lock", createdUser.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(lockDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("用户已锁定"))
                                .andExpect(jsonPath("$.data.userId").value(createdUser.getId()));

                // 3. 解锁用户
                mockMvc.perform(post("/api/user/{userId}/unlock", createdUser.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("用户已解锁"))
                                .andExpect(jsonPath("$.data.userId").value(createdUser.getId()));
        }

        @Test
        void testPermissionManagementFlow() throws Exception {
                // 1. 创建测试用户
                UserCreateDTO createDTO = UserCreateDTO.builder()
                                .username("permissiontest")
                                .email("permission@test.com")
                                .password("password123")
                                .role("VIEWER")
                                .build();

                mockMvc.perform(post("/api/user/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createDTO)))
                                .andExpect(status().isOk());

                User createdUser = userMapper.selectByLoginIdentifier("permissiontest");
                assertNotNull(createdUser);

                // 2. 授予权限
                PermissionGrantDTO grantDTO = PermissionGrantDTO.builder()
                                .resourceType("VM")
                                .resourceId("vm_1234567890")
                                .permission("READ")
                                .expiresAt(java.time.LocalDateTime.now().plusDays(30))
                                .build();

                String grantResponse = mockMvc.perform(post("/api/user/{userId}/permissions", createdUser.getId())
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
                mockMvc.perform(get("/api/user/{userId}/permissions", createdUser.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("获取成功"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].resourceType").value("VM"))
                                .andExpect(jsonPath("$.data[0].permission").value("READ"));
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
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));

                // 2. 测试登录失败
                UserLoginDTO wrongLoginDTO = UserLoginDTO.builder()
                                .loginIdentifier("errortest")
                                .password("wrongpassword")
                                .build();

                mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrongLoginDTO)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value(401));

                // 3. 测试获取不存在的用户
                mockMvc.perform(get("/api/user/nonexistent-user"))
                                .andExpect(status().isNotFound())
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
                                .andExpect(status().isBadRequest())
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
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }
}
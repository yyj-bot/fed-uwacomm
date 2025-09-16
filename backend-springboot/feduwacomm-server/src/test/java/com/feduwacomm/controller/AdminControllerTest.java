package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.AdminService;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminController单元测试类
 * 根据admin-api-reference.md文档实现
 */
@ExtendWith(MockitoExtension.class)
public class AdminControllerTest {

        @Mock
        private AdminService adminService;

        @InjectMocks
        private AdminController adminController;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                // 创建验证器
                LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
                validator.afterPropertiesSet();

                // 配置MockMvc，包含全局异常处理器和验证支持
                mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                                .setControllerAdvice(new com.feduwacomm.handler.GlobalExceptionHandler())
                                .setValidator(validator)
                                .build();

                // 配置ObjectMapper以支持LocalDateTime
                objectMapper = new ObjectMapper();
                objectMapper.findAndRegisterModules(); // 自动注册JSR310模块
        }

        // 用户管理接口测试

        @Test
        void testGetUserList_Success() throws Exception {
                // 准备测试数据
                List<UserListVO> userList = Arrays.asList(
                                UserListVO.builder()
                                                .userId("a1b2c3d4e5f678901234567890123456")
                                                .username("admin")
                                                .email("admin@example.com")
                                                .role("ADMIN")
                                                .status("ACTIVE")
                                                .lastLoginTime(LocalDateTime.now())
                                                .lastLoginIp("192.168.1.100")
                                                .loginAttempts(0)
                                                .createdAt(LocalDateTime.now())
                                                .updatedAt(LocalDateTime.now())
                                                .build());

                PageResponseDTO<UserListVO> pageResponse = PageResponseDTO.<UserListVO>builder()
                                .total(1L)
                                .page(1)
                                .size(10)
                                .list(userList)
                                .build();

                when(adminService.getUserList(any(UserQueryDTO.class))).thenReturn(pageResponse);

                // 执行测试
                mockMvc.perform(get("/api/admin/user/list")
                                .param("page", "1")
                                .param("size", "10")
                                .param("username", "admin")
                                .param("email", "admin@example.com")
                                .param("role", "ADMIN")
                                .param("status", "ACTIVE"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("获取成功"))
                                .andExpect(jsonPath("$.data.total").value(1))
                                .andExpect(jsonPath("$.data.list[0].userId").value("a1b2c3d4e5f678901234567890123456"))
                                .andExpect(jsonPath("$.data.list[0].username").value("admin"))
                                .andExpect(jsonPath("$.data.list[0].email").value("admin@example.com"))
                                .andExpect(jsonPath("$.data.list[0].role").value("ADMIN"))
                                .andExpect(jsonPath("$.data.list[0].status").value("ACTIVE"));

                verify(adminService, times(1)).getUserList(any(UserQueryDTO.class));
        }

        @Test
        void testGetUserDetail_Success() throws Exception {
                // 准备测试数据
                String userId = "a1b2c3d4e5f678901234567890123456";
                UserDetailVO userDetail = UserDetailVO.builder()
                                .userId(userId)
                                .username("admin")
                                .email("admin@example.com")
                                .role("ADMIN")
                                .status("ACTIVE")
                                .lastLoginTime(LocalDateTime.now())
                                .lastLoginIp("192.168.1.100")
                                .loginAttempts(0)
                                .lockedUntil(null)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .createdBy("system")
                                .updatedBy("system")
                                .build();

                when(adminService.getUserDetail(userId)).thenReturn(userDetail);

                // 执行测试
                mockMvc.perform(get("/api/admin/user/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("获取成功"))
                                .andExpect(jsonPath("$.data.userId").value(userId))
                                .andExpect(jsonPath("$.data.username").value("admin"))
                                .andExpect(jsonPath("$.data.email").value("admin@example.com"))
                                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

                verify(adminService, times(1)).getUserDetail(userId);
        }

        @Test
        void testCreateUser_Success() throws Exception {
                // 准备测试数据
                UserCreateDTO createDTO = UserCreateDTO.builder()
                                .username("newuser")
                                .email("newuser@example.com")
                                .password("password123")
                                .role("RESEARCHER")
                                .build();

                UserCreateResponseVO responseVO = UserCreateResponseVO.builder()
                                .userId("c3d4e5f6789012345678901234567890")
                                .username("newuser")
                                .email("newuser@example.com")
                                .role("RESEARCHER")
                                .status("ACTIVE")
                                .createdAt(LocalDateTime.now())
                                .build();

                when(adminService.createUser(any(UserCreateDTO.class))).thenReturn(responseVO);

                // 执行测试
                mockMvc.perform(post("/api/admin/user/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("创建成功"))
                                .andExpect(jsonPath("$.data.userId").value("c3d4e5f6789012345678901234567890"))
                                .andExpect(jsonPath("$.data.username").value("newuser"))
                                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                                .andExpect(jsonPath("$.data.role").value("RESEARCHER"))
                                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

                verify(adminService, times(1)).createUser(any(UserCreateDTO.class));
        }

        @Test
        void testUpdateUser_Success() throws Exception {
                // 准备测试数据
                String userId = "a1b2c3d4e5f678901234567890123456";
                UserAdminUpdateDTO updateDTO = UserAdminUpdateDTO.builder()
                                .username("updateduser")
                                .email("updated@example.com")
                                .role("RESEARCHER")
                                .status("ACTIVE")
                                .password("newpassword123")
                                .build();

                UserUpdateResponseVO responseVO = UserUpdateResponseVO.builder()
                                .userId(userId)
                                .username("updateduser")
                                .email("updated@example.com")
                                .role("RESEARCHER")
                                .status("ACTIVE")
                                .updatedAt(LocalDateTime.now())
                                .build();

                when(adminService.updateUser(eq(userId), any(UserAdminUpdateDTO.class))).thenReturn(responseVO);

                // 执行测试
                mockMvc.perform(put("/api/admin/user/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("更新成功"))
                                .andExpect(jsonPath("$.data.userId").value(userId))
                                .andExpect(jsonPath("$.data.username").value("updateduser"))
                                .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                                .andExpect(jsonPath("$.data.role").value("RESEARCHER"))
                                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

                verify(adminService, times(1)).updateUser(eq(userId), any(UserAdminUpdateDTO.class));
        }

        @Test
        void testDeleteUser_Success() throws Exception {
                // 准备测试数据
                String userId = "a1b2c3d4e5f678901234567890123456";

                doNothing().when(adminService).deleteUser(userId);

                // 执行测试
                mockMvc.perform(delete("/api/admin/user/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("删除成功"))
                                .andExpect(jsonPath("$.data").isEmpty());

                verify(adminService, times(1)).deleteUser(userId);
        }

        @Test
        void testLockUser_Success() throws Exception {
                // 准备测试数据
                String userId = "a1b2c3d4e5f678901234567890123456";
                UserLockDTO lockDTO = UserLockDTO.builder()
                                .duration(3600)
                                .build();

                UserLockResponseVO responseVO = UserLockResponseVO.builder()
                                .userId(userId)
                                .lockedUntil(LocalDateTime.now().plusHours(1))
                                .build();

                when(adminService.lockUser(eq(userId), any(UserLockDTO.class))).thenReturn(responseVO);

                // 执行测试
                mockMvc.perform(post("/api/admin/user/{userId}/lock", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(lockDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("用户已锁定"))
                                .andExpect(jsonPath("$.data.userId").value(userId))
                                .andExpect(jsonPath("$.data.lockedUntil").exists());

                verify(adminService, times(1)).lockUser(eq(userId), any(UserLockDTO.class));
        }

        @Test
        void testUnlockUser_Success() throws Exception {
                // 准备测试数据
                String userId = "a1b2c3d4e5f678901234567890123456";
                UserUnlockResponseVO responseVO = UserUnlockResponseVO.builder()
                                .userId(userId)
                                .status("ACTIVE")
                                .build();

                when(adminService.unlockUser(userId)).thenReturn(responseVO);

                // 执行测试
                mockMvc.perform(post("/api/admin/user/{userId}/unlock", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("用户已解锁"))
                                .andExpect(jsonPath("$.data.userId").value(userId))
                                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

                verify(adminService, times(1)).unlockUser(userId);
        }

        @Test
        void testResetPassword_Success() throws Exception {
                // 准备测试数据
                String userId = "a1b2c3d4e5f678901234567890123456";
                PasswordResetDTO resetDTO = PasswordResetDTO.builder()
                                .newPassword("newpassword123")
                                .build();

                doNothing().when(adminService).resetPassword(eq(userId), any(PasswordResetDTO.class));

                // 执行测试
                mockMvc.perform(post("/api/admin/user/{userId}/reset-password", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resetDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("密码重置成功"))
                                .andExpect(jsonPath("$.data").isEmpty());

                verify(adminService, times(1)).resetPassword(eq(userId), any(PasswordResetDTO.class));
        }

        // 权限管理已改为基于角色的权限管理，不再需要单独的权限接口测试



        // 错误处理测试

        @Test
        void testGetUserDetail_UserNotFound() throws Exception {
                // 准备测试数据
                String userId = "nonexistent_user";

                when(adminService.getUserDetail(userId)).thenThrow(new RuntimeException("用户不存在"));

                // 执行测试并打印响应
                String response = mockMvc.perform(get("/api/admin/user/{userId}", userId))
                                .andExpect(status().isOk()) // 全局异常处理器返回200状态码
                                .andExpect(jsonPath("$.code").value(500)) // 业务状态码在响应体中
                                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                System.out.println("=== 用户不存在测试响应 ===");
                System.out.println(response);
                System.out.println("=========================");
        }

        @Test
        void testCreateUser_ValidationError() throws Exception {
                // 准备测试数据 - 缺少必填字段
                UserCreateDTO createDTO = UserCreateDTO.builder()
                                .username("")
                                .email("invalid-email")
                                .password("")
                                .build();

                // 执行测试并打印响应
                String response = mockMvc.perform(post("/api/admin/user/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createDTO)))
                                .andExpect(status().isOk()) // 全局异常处理器返回200状态码
                                .andExpect(jsonPath("$.code").value(400)) // 业务状态码在响应体中
                                .andExpect(jsonPath("$.message").value("验证失败"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                System.out.println("=== 验证错误测试响应 ===");
                System.out.println(response);
                System.out.println("=========================");
        }

        @Test
        void testUpdateUser_UserNotFound() throws Exception {
                // 准备测试数据
                String userId = "nonexistent_user";
                UserAdminUpdateDTO updateDTO = UserAdminUpdateDTO.builder()
                                .username("updateduser")
                                .email("updated@example.com")
                                .role("RESEARCHER")
                                .build();

                when(adminService.updateUser(eq(userId), any(UserAdminUpdateDTO.class)))
                                .thenThrow(new RuntimeException("用户不存在"));

                // 执行测试并打印响应
                String response = mockMvc.perform(put("/api/admin/user/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDTO)))
                                .andExpect(status().isOk()) // 全局异常处理器返回200状态码
                                .andExpect(jsonPath("$.code").value(500)) // 业务状态码在响应体中
                                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                System.out.println("=== 更新用户不存在测试响应 ===");
                System.out.println(response);
                System.out.println("=========================");
        }

        @Test
        void testDeleteUser_UserNotFound() throws Exception {
                // 准备测试数据
                String userId = "nonexistent_user";

                doThrow(new RuntimeException("用户不存在")).when(adminService).deleteUser(userId);

                // 执行测试并打印响应
                String response = mockMvc.perform(delete("/api/admin/user/{userId}", userId))
                                .andExpect(status().isOk()) // 全局异常处理器返回200状态码
                                .andExpect(jsonPath("$.code").value(500)) // 业务状态码在响应体中
                                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                System.out.println("=== 删除用户不存在测试响应 ===");
                System.out.println(response);
                System.out.println("=========================");
        }

        @Test
        void testLockUser_UserNotFound() throws Exception {
                // 准备测试数据
                String userId = "nonexistent_user";
                UserLockDTO lockDTO = UserLockDTO.builder()
                                .duration(3600)
                                .build();

                when(adminService.lockUser(eq(userId), any(UserLockDTO.class)))
                                .thenThrow(new RuntimeException("用户不存在"));

                // 执行测试并打印响应
                String response = mockMvc.perform(post("/api/admin/user/{userId}/lock", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(lockDTO)))
                                .andExpect(status().isOk()) // 全局异常处理器返回200状态码
                                .andExpect(jsonPath("$.code").value(500)) // 业务状态码在响应体中
                                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                System.out.println("=== 锁定用户不存在测试响应 ===");
                System.out.println(response);
                System.out.println("=========================");
        }



        // 边界条件测试

        @Test
        void testGetUserList_EmptyResult() throws Exception {
                // 准备测试数据
                PageResponseDTO<UserListVO> emptyResponse = PageResponseDTO.<UserListVO>builder()
                                .total(0L)
                                .page(1)
                                .size(10)
                                .list(Arrays.asList())
                                .build();

                when(adminService.getUserList(any(UserQueryDTO.class))).thenReturn(emptyResponse);

                // 执行测试
                mockMvc.perform(get("/api/admin/user/list")
                                .param("page", "1")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("获取成功"))
                                .andExpect(jsonPath("$.data.total").value(0))
                                .andExpect(jsonPath("$.data.list").isArray())
                                .andExpect(jsonPath("$.data.list.length()").value(0));

                verify(adminService, times(1)).getUserList(any(UserQueryDTO.class));
        }

        @Test
        void testGetUserList_WithPagination() throws Exception {
                // 准备测试数据
                PageResponseDTO<UserListVO> pageResponse = PageResponseDTO.<UserListVO>builder()
                                .total(100L)
                                .page(2)
                                .size(20)
                                .list(Arrays.asList())
                                .build();

                when(adminService.getUserList(any(UserQueryDTO.class))).thenReturn(pageResponse);

                // 执行测试
                mockMvc.perform(get("/api/admin/user/list")
                                .param("page", "2")
                                .param("size", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.total").value(100))
                                .andExpect(jsonPath("$.data.page").value(2))
                                .andExpect(jsonPath("$.data.size").value(20));

                verify(adminService, times(1)).getUserList(any(UserQueryDTO.class));
        }

        @Test
        void testLockUser_WithoutDuration() throws Exception {
                // 准备测试数据 - 不指定锁定时长，使用默认值
                String userId = "a1b2c3d4e5f678901234567890123456";
                UserLockDTO lockDTO = UserLockDTO.builder().build();

                UserLockResponseVO responseVO = UserLockResponseVO.builder()
                                .userId(userId)
                                .lockedUntil(LocalDateTime.now().plusHours(1))
                                .build();

                when(adminService.lockUser(eq(userId), any(UserLockDTO.class))).thenReturn(responseVO);

                // 执行测试
                mockMvc.perform(post("/api/admin/user/{userId}/lock", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(lockDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("用户已锁定"));

                verify(adminService, times(1)).lockUser(eq(userId), any(UserLockDTO.class));
        }
}
package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.BaseContext;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.UserService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController单元测试类
 * 根据最新的user-api-reference.md文档更新
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

        @Mock
        private UserService userService;

        @InjectMocks
        private UserController userController;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
                objectMapper = new ObjectMapper();
        }

        // 认证相关接口测试

        @Test
        void testRegister_Success() throws Exception {
                // 准备测试数据
                UserRegisterDTO registerDTO = UserRegisterDTO.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .password("password123")
                                .confirmPassword("password123")
                                .build();

                UserRegisterResponseVO responseVO = UserRegisterResponseVO.builder()
                                .userId("a1b2c3d4e5f678901234567890123456")
                                .username("testuser")
                                .email("test@example.com")
                                .role("VIEWER")
                                .status("ACTIVE")
                                .createdAt(LocalDateTime.now())
                                .build();

                when(userService.register(any(UserRegisterDTO.class))).thenReturn(responseVO);

                // 执行测试
                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("注册成功"))
                                .andExpect(jsonPath("$.data.userId").value("a1b2c3d4e5f678901234567890123456"))
                                .andExpect(jsonPath("$.data.username").value("testuser"));

                verify(userService, times(1)).register(any(UserRegisterDTO.class));
        }

        @Test
        void testLogin_Success() throws Exception {
                // 准备测试数据
                UserLoginDTO loginDTO = UserLoginDTO.builder()
                                .loginIdentifier("testuser")
                                .password("password123")
                                .captcha("1234")
                                .captchaKey("key123")
                                .rememberMe(false)
                                .build();

                LoginResponseVO responseVO = LoginResponseVO.builder()
                                .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                                .refreshToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                                .expiresIn(86400L)
                                .user(UserInfoVO.builder()
                                                .userId("a1b2c3d4e5f678901234567890123456")
                                                .username("testuser")
                                                .email("test@example.com")
                                                .role("VIEWER")
                                                .status("ACTIVE")
                                                .lastLoginTime(LocalDateTime.now())
                                                .lastLoginIp("192.168.1.100")
                                                .build())
                                .build();

                when(userService.login(any(UserLoginDTO.class))).thenReturn(responseVO);

                // 执行测试
                mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("登录成功"))
                                .andExpect(jsonPath("$.data.token").exists())
                                .andExpect(jsonPath("$.data.user.userId").value("a1b2c3d4e5f678901234567890123456"));

                verify(userService, times(1)).login(any(UserLoginDTO.class));
        }

        @Test
        void testRefreshToken_Success() throws Exception {
                // 准备测试数据
                String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
                TokenRefreshResponseVO responseVO = TokenRefreshResponseVO.builder()
                                .token("new_token_123")
                                .refreshToken("new_refresh_token_123")
                                .expiresIn(86400L)
                                .build();

                when(userService.refreshToken(anyString())).thenReturn(responseVO);

                // 执行测试
                mockMvc.perform(post("/api/user/refresh")
                                .header("Authorization", "Bearer " + refreshToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("Token刷新成功"))
                                .andExpect(jsonPath("$.data.token").value("new_token_123"));

                verify(userService, times(1)).refreshToken(refreshToken);
        }

        @Test
        void testLogout_Success() throws Exception {
                // 准备测试数据
                String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

                doNothing().when(userService).logout(anyString());

                // 执行测试
                mockMvc.perform(post("/api/user/logout")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("登出成功"));

                verify(userService, times(1)).logout(token);
        }

        // 用户信息相关接口测试

        @Test
        void testGetCurrentUserInfo_Success() throws Exception {
                // 准备测试数据
                String userId = "a1b2c3d4e5f678901234567890123456";
                UserInfoVO userInfo = UserInfoVO.builder()
                                .userId(userId)
                                .username("testuser")
                                .email("test@example.com")
                                .role("VIEWER")
                                .status("ACTIVE")
                                .lastLoginTime(LocalDateTime.now())
                                .lastLoginIp("192.168.1.100")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                // 模拟BaseContext.getCurrentUserId()
                try (var baseContextMock = mockStatic(BaseContext.class)) {
                        baseContextMock.when(BaseContext::getCurrentUserId).thenReturn(userId);
                        when(userService.getCurrentUserInfo(userId)).thenReturn(userInfo);

                        // 执行测试
                        mockMvc.perform(get("/api/user/profile"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("获取成功"))
                                        .andExpect(jsonPath("$.data.userId").value(userId));

                        verify(userService, times(1)).getCurrentUserInfo(userId);
                }
        }

        @Test
        void testUpdateUserInfo_Success() throws Exception {
                // 准备测试数据
                String userId = "a1b2c3d4e5f678901234567890123456";
                UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                                .username("newusername")
                                .email("newemail@example.com")
                                .build();

                UserUpdateResponseVO responseVO = UserUpdateResponseVO.builder()
                                .userId(userId)
                                .username("newusername")
                                .email("newemail@example.com")
                                .role("RESEARCHER")
                                .status("ACTIVE")
                                .updatedAt(LocalDateTime.now())
                                .build();

                // 模拟BaseContext.getCurrentUserId()
                try (var baseContextMock = mockStatic(BaseContext.class)) {
                        baseContextMock.when(BaseContext::getCurrentUserId).thenReturn(userId);
                        when(userService.updateUserInfo(eq(userId), any(UserUpdateDTO.class))).thenReturn(responseVO);

                        // 执行测试
                        mockMvc.perform(put("/api/user/profile")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(updateDTO)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("更新成功"))
                                        .andExpect(jsonPath("$.data.username").value("newusername"));

                        verify(userService, times(1)).updateUserInfo(eq(userId), any(UserUpdateDTO.class));
                }
        }

        @Test
        void testChangePassword_Success() throws Exception {
                // 准备测试数据
                String userId = "a1b2c3d4e5f678901234567890123456";
                PasswordChangeDTO passwordDTO = PasswordChangeDTO.builder()
                                .oldPassword("oldpassword")
                                .newPassword("newpassword")
                                .confirmPassword("newpassword")
                                .build();

                // 模拟BaseContext.getCurrentUserId()
                try (var baseContextMock = mockStatic(BaseContext.class)) {
                        baseContextMock.when(BaseContext::getCurrentUserId).thenReturn(userId);
                        doNothing().when(userService).changePassword(eq(userId), any(PasswordChangeDTO.class));

                        // 执行测试
                        mockMvc.perform(put("/api/user/password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(passwordDTO)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("密码修改成功"));

                        verify(userService, times(1)).changePassword(eq(userId), any(PasswordChangeDTO.class));
                }
        }

        // 错误处理测试

        @Test
        void testRegister_ValidationError() throws Exception {
                // 准备测试数据 - 缺少必填字段
                UserRegisterDTO registerDTO = UserRegisterDTO.builder()
                                .username("test")
                                .email("invalid-email")
                                .password("123")
                                .confirmPassword("456")
                                .build();

                // 执行测试
                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testLogin_InvalidCredentials() throws Exception {
                // 准备测试数据
                UserLoginDTO loginDTO = UserLoginDTO.builder()
                                .loginIdentifier("wronguser")
                                .password("wrongpassword")
                                .build();

                when(userService.login(any(UserLoginDTO.class))).thenThrow(new RuntimeException("账号或密码错误"));

                // 执行测试
                mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isInternalServerError());
        }
}
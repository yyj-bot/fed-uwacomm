package com.feduwacomm.controller;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.common.Result;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.UserService;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 * 提供用户自助操作功能
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final Logger accessLog = LoggerFactory.getLogger("ACCESS_LOG");

    @Autowired
    private UserService userService;

    // 认证相关接口
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserRegisterResponseVO> register(@Valid @RequestBody UserRegisterDTO registerDTO, 
                                                 HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        log.info("收到用户注册请求: username={}, email={}, ip={}", 
            registerDTO.getUsername(), registerDTO.getEmail(), clientIp);
        accessLog.info("用户注册请求: username={}, email={}, ip={}, userAgent={}", 
            registerDTO.getUsername(), registerDTO.getEmail(), clientIp, userAgent);
        
        try {
            UserRegisterResponseVO response = userService.register(registerDTO);
            log.info("用户注册请求处理成功: username={}, userId={}, ip={}", 
                registerDTO.getUsername(), response.getUserId(), clientIp);
            return Result.success("注册成功", response);
        } catch (Exception e) {
            log.error("用户注册请求处理失败: username={}, email={}, ip={}, error={}", 
                registerDTO.getUsername(), registerDTO.getEmail(), clientIp, e.getMessage());
            throw e;
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponseVO> login(@Valid @RequestBody UserLoginDTO loginDTO, 
                                       HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        log.info("收到用户登录请求: identifier={}, ip={}", loginDTO.getLoginIdentifier(), clientIp);
        accessLog.info("用户登录请求: identifier={}, ip={}, userAgent={}", 
            loginDTO.getLoginIdentifier(), clientIp, userAgent);
        
        try {
            LoginResponseVO response = userService.login(loginDTO);
            log.info("用户登录请求处理成功: identifier={}, userId={}, ip={}", 
                loginDTO.getLoginIdentifier(), response.getUser().getUserId(), clientIp);
            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("用户登录请求处理失败: identifier={}, ip={}, error={}", 
                loginDTO.getLoginIdentifier(), clientIp, e.getMessage());
            throw e;
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<TokenRefreshResponseVO> refreshToken(@RequestHeader("Authorization") String authorization,
                                                     HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String refreshToken = authorization.replace("Bearer ", "");
        
        log.info("收到Token刷新请求: ip={}", clientIp);
        accessLog.info("Token刷新请求: ip={}, userAgent={}", clientIp, userAgent);
        
        try {
            TokenRefreshResponseVO response = userService.refreshToken(refreshToken);
            log.info("Token刷新请求处理成功: ip={}", clientIp);
            return Result.success("Token刷新成功", response);
        } catch (Exception e) {
            log.error("Token刷新请求处理失败: ip={}, error={}", clientIp, e.getMessage());
            throw e;
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("Authorization") String authorization,
                                HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String token = authorization.replace("Bearer ", "");
        
        log.info("收到用户登出请求: ip={}", clientIp);
        accessLog.info("用户登出请求: ip={}, userAgent={}", clientIp, userAgent);
        
        try {
            userService.logout(token);
            log.info("用户登出请求处理成功: ip={}", clientIp);
            return Result.success("登出成功", null);
        } catch (Exception e) {
            log.error("用户登出请求处理失败: ip={}, error={}", clientIp, e.getMessage());
            throw e;
        }
    }

    // 用户信息相关接口
    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public Result<UserInfoVO> getCurrentUserInfo(HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String userId = BaseContext.getCurrentUserId();
        
        log.debug("收到获取用户信息请求: userId={}, ip={}", userId, clientIp);
        accessLog.info("获取用户信息请求: userId={}, ip={}, userAgent={}", userId, clientIp, userAgent);
        
        try {
            UserInfoVO response = userService.getCurrentUserInfo(userId);
            log.debug("获取用户信息请求处理成功: userId={}, ip={}", userId, clientIp);
            return Result.success("获取成功", response);
        } catch (Exception e) {
            log.error("获取用户信息请求处理失败: userId={}, ip={}, error={}", userId, clientIp, e.getMessage());
            throw e;
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    public Result<UserUpdateResponseVO> updateUserInfo(@RequestBody UserUpdateDTO updateDTO,
                                                      HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String userId = BaseContext.getCurrentUserId();
        
        log.info("收到更新用户信息请求: userId={}, updates={}, ip={}", userId, updateDTO, clientIp);
        accessLog.info("更新用户信息请求: userId={}, updates={}, ip={}, userAgent={}", 
            userId, updateDTO, clientIp, userAgent);
        
        try {
            UserUpdateResponseVO response = userService.updateUserInfo(userId, updateDTO);
            log.info("更新用户信息请求处理成功: userId={}, ip={}", userId, clientIp);
            return Result.success("更新成功", response);
        } catch (Exception e) {
            log.error("更新用户信息请求处理失败: userId={}, ip={}, error={}", userId, clientIp, e.getMessage());
            throw e;
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<String> changePassword(@RequestBody PasswordChangeDTO passwordDTO,
                                       HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String userId = BaseContext.getCurrentUserId();
        
        log.info("收到修改密码请求: userId={}, ip={}", userId, clientIp);
        accessLog.info("修改密码请求: userId={}, ip={}, userAgent={}", userId, clientIp, userAgent);
        
        try {
            userService.changePassword(userId, passwordDTO);
            log.info("修改密码请求处理成功: userId={}, ip={}", userId, clientIp);
            return Result.success("密码修改成功", null);
        } catch (Exception e) {
            log.error("修改密码请求处理失败: userId={}, ip={}, error={}", userId, clientIp, e.getMessage());
            throw e;
        }
    }
}
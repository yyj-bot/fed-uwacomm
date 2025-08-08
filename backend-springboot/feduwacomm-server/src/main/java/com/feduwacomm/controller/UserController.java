package com.feduwacomm.controller;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.common.Result;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.UserService;
import com.feduwacomm.vo.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // 认证相关接口
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserRegisterResponseVO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        UserRegisterResponseVO response = userService.register(registerDTO);
        return Result.success("注册成功", response);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponseVO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        LoginResponseVO response = userService.login(loginDTO);
        return Result.success("登录成功", response);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<TokenRefreshResponseVO> refreshToken(@RequestHeader("Authorization") String authorization) {
        String refreshToken = authorization.replace("Bearer ", "");
        TokenRefreshResponseVO response = userService.refreshToken(refreshToken);
        return Result.success("Token刷新成功", response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        userService.logout(token);
        return Result.success("登出成功", null);
    }

    // 用户信息相关接口
    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public Result<UserInfoVO> getCurrentUserInfo() {
        String userId = BaseContext.getCurrentUserId();
        UserInfoVO response = userService.getCurrentUserInfo(userId);
        return Result.success("获取成功", response);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    public Result<UserUpdateResponseVO> updateUserInfo(@RequestBody UserUpdateDTO updateDTO) {
        String userId = BaseContext.getCurrentUserId();
        UserUpdateResponseVO response = userService.updateUserInfo(userId, updateDTO);
        return Result.success("更新成功", response);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<String> changePassword(@RequestBody PasswordChangeDTO passwordDTO) {
        String userId = BaseContext.getCurrentUserId();
        userService.changePassword(userId, passwordDTO);
        return Result.success("密码修改成功", null);
    }

}
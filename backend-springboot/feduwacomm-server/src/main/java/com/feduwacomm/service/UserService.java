package com.feduwacomm.service;

import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.vo.*;

/**
 * 用户服务接口
 * 提供用户自助操作功能
 */
public interface UserService {

    // 认证相关方法
    /**
     * 用户注册
     */
    UserRegisterResponseVO register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     */
    LoginResponseVO login(UserLoginDTO loginDTO);

    /**
     * 刷新Token
     */
    TokenRefreshResponseVO refreshToken(String refreshToken);

    /**
     * 用户登出
     */
    void logout(String token);

    // 用户信息相关方法
    /**
     * 获取当前用户信息
     */
    UserInfoVO getCurrentUserInfo(String userId);

    /**
     * 更新用户信息
     */
    UserUpdateResponseVO updateUserInfo(String userId, UserUpdateDTO updateDTO);

    /**
     * 修改密码
     */
    void changePassword(String userId, PasswordChangeDTO passwordDTO);

    // 内部方法
    /**
     * 根据ID获取用户
     */
    User getUserById(String userId);

    /**
     * 根据登录标识符（用户名或邮箱）获取用户
     */
    User getUserByLoginIdentifier(String loginIdentifier);

    /**
     * 检查用户是否存在
     */
    boolean userExists(String userId);

    /**
     * 检查登录标识符是否存在
     */
    boolean loginIdentifierExists(String loginIdentifier);

    /**
     * 检查邮箱是否存在
     */
    boolean emailExists(String email);

    /**
     * 检查用户名是否存在
     */
    boolean usernameExists(String username);
}
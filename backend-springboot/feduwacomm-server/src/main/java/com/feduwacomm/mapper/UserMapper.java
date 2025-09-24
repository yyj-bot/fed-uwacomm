package com.feduwacomm.mapper;

import com.feduwacomm.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据访问层接口
 * 提供用户自助操作相关的数据访问功能
 */
@Mapper
public interface UserMapper {

        // 基础查询操作
        /**
         * 根据ID查询用户
         */
        User selectById(@Param("id") String id);

        /**
         * 根据邮箱查询用户
         */
        User selectByEmail(@Param("email") String email);

        /**
         * 根据用户名查询用户
         */
        User selectByUsername(@Param("username") String username);

        /**
         * 根据登录标识符（用户名或邮箱）查询用户
         */
        User selectByLoginIdentifier(@Param("loginIdentifier") String loginIdentifier);

        /**
         * 查询第一个管理员用户（按创建时间排序）
         */
        User selectFirstAdmin();

        // 用户自助操作
        /**
         * 插入用户（用户注册时使用）
         */
        int insert(User user);

        /**
         * 更新用户基本信息
         */
        int update(User user);

        /**
         * 更新用户最后登录信息
         */
        int updateLastLogin(@Param("id") String id, @Param("lastLoginTime") String lastLoginTime,
                        @Param("lastLoginIp") String lastLoginIp);

        /**
         * 更新登录失败次数
         */
        int updateLoginAttempts(@Param("id") String id, @Param("loginAttempts") Integer loginAttempts);

        /**
         * 更新用户密码
         */
        int updatePassword(@Param("id") String id, @Param("passwordHash") String passwordHash);

        // 管理员操作
        /**
         * 分页查询用户列表
         */
        List<User> selectByPage(@Param("offset") int offset, @Param("limit") int limit,
                               @Param("role") String role, @Param("status") String status,
                               @Param("keyword") String keyword);

        /**
         * 条件查询用户总数
         */
        int countByCondition(@Param("role") String role, @Param("status") String status,
                           @Param("keyword") String keyword);

        /**
         * 锁定用户
         */
        int lockUser(@Param("id") String id, @Param("lockedUntil") LocalDateTime lockedUntil,
                    @Param("lockReason") String lockReason, @Param("updatedBy") String updatedBy);

        /**
         * 解锁用户
         */
        int unlockUser(@Param("id") String id, @Param("updatedBy") String updatedBy);

        /**
         * 删除用户
         */
        int deleteById(@Param("id") String id);

        /**
         * 更新用户状态
         */
        int updateStatus(@Param("id") String id, @Param("status") String status,
                        @Param("updatedBy") String updatedBy);

        /**
         * 更新用户角色
         */
        int updateRole(@Param("id") String id, @Param("role") String role,
                      @Param("updatedBy") String updatedBy);

        // 统计操作
        /**
         * 统计用户总数
         */
        int countAll();

        /**
         * 根据角色统计用户数量
         */
        int countByRole(@Param("role") String role);

        /**
         * 根据状态统计用户数量
         */
        int countByStatus(@Param("status") String status);
}
package com.feduwacomm.mapper;

import com.feduwacomm.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理员数据访问层接口
 * 提供管理员专用的用户管理操作
 */
@Mapper
public interface AdminMapper {

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

    // 用户管理操作
    /**
     * 条件查询用户
     */
    List<User> selectByCondition(@Param("role") String role, @Param("status") String status,
            @Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 条件查询用户总数
     */
    int countByCondition(@Param("role") String role, @Param("status") String status,
            @Param("keyword") String keyword);

    /**
     * 插入用户
     */
    int insert(User user);

    /**
     * 更新用户
     */
    int update(User user);

    /**
     * 根据ID删除用户
     */
    int deleteById(@Param("id") String id);

    /**
     * 更新用户密码
     */
    int updatePassword(@Param("id") String id, @Param("passwordHash") String passwordHash);

    // 统计查询
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
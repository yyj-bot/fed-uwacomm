package com.feduwacomm.mapper;

import com.feduwacomm.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问层接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据ID查询用户
     */
    User selectById(@Param("id") String id);

    /**
     * 根据账号查询用户
     */
    User selectByAccount(@Param("account") String account);

    /**
     * 根据邮箱查询用户
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 根据用户名查询用户
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 查询所有用户
     */
    List<User> selectAll();

    /**
     * 根据角色查询用户
     */
    List<User> selectByRole(@Param("role") String role);

    /**
     * 根据状态查询用户
     */
    List<User> selectByStatus(@Param("status") String status);

    /**
     * 分页查询用户
     */
    List<User> selectByPage(@Param("offset") int offset, @Param("limit") int limit);

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
     * 更新用户最后登录信息
     */
    int updateLastLogin(@Param("id") String id, @Param("lastLoginTime") String lastLoginTime,
            @Param("lastLoginIp") String lastLoginIp);

    /**
     * 更新登录失败次数
     */
    int updateLoginAttempts(@Param("id") String id, @Param("loginAttempts") Integer loginAttempts);

    /**
     * 锁定用户
     */
    int lockUser(@Param("id") String id, @Param("lockedUntil") String lockedUntil);

    /**
     * 解锁用户
     */
    int unlockUser(@Param("id") String id);

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
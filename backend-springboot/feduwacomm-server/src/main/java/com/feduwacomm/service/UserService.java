package com.feduwacomm.service;

import com.feduwacomm.dto.UserDTO;
import com.feduwacomm.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 根据ID查询用户
     */
    User findById(Long id);

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    /**
     * 创建用户
     */
    User createUser(UserDTO userDTO);

    /**
     * 更新用户
     */
    User updateUser(Long id, UserDTO userDTO);

    /**
     * 删除用户
     */
    boolean deleteUser(Long id);

    /**
     * 统计用户总数
     */
    int count();

    /**
     * 根据条件查询用户
     */
    List<User> findByCondition(String username, String email, String phone);
}
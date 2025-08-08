package com.feduwacomm.service.impl;

import com.feduwacomm.dto.UserDTO;
import com.feduwacomm.entity.User;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    @Override
    public User updateUser(Long id, UserDTO userDTO) {
        User existingUser = userMapper.findById(id);
        if (existingUser == null) {
            return null;
        }

        existingUser.setUsername(userDTO.getUsername());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setPhone(userDTO.getPhone());
        existingUser.setStatus(userDTO.getStatus());
        existingUser.setUpdateTime(LocalDateTime.now());

        userMapper.update(existingUser);
        return existingUser;
    }

    @Override
    public boolean deleteUser(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public int count() {
        return userMapper.count();
    }

    @Override
    public List<User> findByCondition(String username, String email, String phone) {
        Map<String, Object> params = new HashMap<>();
        if (username != null && !username.trim().isEmpty()) {
            params.put("username", username);
        }
        if (email != null && !email.trim().isEmpty()) {
            params.put("email", email);
        }
        if (phone != null && !phone.trim().isEmpty()) {
            params.put("phone", phone);
        }
        return userMapper.findByCondition(params);
    }
}
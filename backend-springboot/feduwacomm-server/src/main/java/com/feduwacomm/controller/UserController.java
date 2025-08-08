package com.feduwacomm.controller;

import com.feduwacomm.dto.UserDTO;
import com.feduwacomm.entity.User;
import com.feduwacomm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取所有用户
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<User> users = userService.findAll();
            response.put("success", true);
            response.put("data", users);
            response.put("message", "获取用户列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findById(id);
            if (user != null) {
                response.put("success", true);
                response.put("data", user);
                response.put("message", "获取用户成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 创建用户
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody UserDTO userDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            User createdUser = userService.createUser(userDTO);
            response.put("success", true);
            response.put("data", createdUser);
            response.put("message", "创建用户成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "创建用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            User updatedUser = userService.updateUser(id, userDTO);
            if (updatedUser != null) {
                response.put("success", true);
                response.put("data", updatedUser);
                response.put("message", "更新用户成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = userService.deleteUser(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "删除用户成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getUserCount() {
        Map<String, Object> response = new HashMap<>();
        try {
            int count = userService.count();
            response.put("success", true);
            response.put("data", count);
            response.put("message", "获取用户数量成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户数量失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根据条件查询用户
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<User> users = userService.findByCondition(username, email, phone);
            response.put("success", true);
            response.put("data", users);
            response.put("message", "查询用户成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查询用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
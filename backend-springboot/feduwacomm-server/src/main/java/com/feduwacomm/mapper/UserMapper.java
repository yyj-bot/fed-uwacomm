package com.feduwacomm.mapper;

import com.feduwacomm.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper {

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 根据ID查询用户
     */
    User findById(@Param("id") Long id);

    /**
     * 根据用户名查询用户
     */
    User findByUsername(@Param("username") String username);

    /**
     * 插入用户
     */
    int insert(User user);

    /**
     * 更新用户
     */
    int update(User user);

    /**
     * 删除用户（软删除）
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计用户总数
     */
    int count();

    /**
     * 根据条件查询用户
     */
    List<User> findByCondition(Map<String, Object> params);
}
package com.feduwacomm.mapper;

import com.feduwacomm.entity.UserPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户权限数据访问层接口
 */
@Mapper
public interface UserPermissionMapper {

    /**
     * 根据ID查询权限
     */
    UserPermission selectById(@Param("id") String id);

    /**
     * 根据用户ID查询所有权限
     */
    List<UserPermission> selectByUserId(@Param("userId") String userId);

    /**
     * 根据用户ID和资源类型查询权限
     */
    List<UserPermission> selectByUserIdAndResourceType(@Param("userId") String userId,
            @Param("resourceType") String resourceType);

    /**
     * 根据用户ID、资源类型和资源ID查询权限
     */
    UserPermission selectByUserIdAndResource(@Param("userId") String userId, @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId);

    /**
     * 查询所有权限
     */
    List<UserPermission> selectAll();

    /**
     * 插入权限
     */
    int insert(UserPermission permission);

    /**
     * 更新权限
     */
    int update(UserPermission permission);

    /**
     * 根据ID删除权限
     */
    int deleteById(@Param("id") String id);

    /**
     * 根据用户ID删除所有权限
     */
    int deleteByUserId(@Param("userId") String userId);

    /**
     * 根据用户ID和资源类型删除权限
     */
    int deleteByUserIdAndResourceType(@Param("userId") String userId, @Param("resourceType") String resourceType);

    /**
     * 检查用户是否有指定权限
     */
    int checkPermission(@Param("userId") String userId, @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId, @Param("permission") String permission);

    /**
     * 统计用户权限数量
     */
    int countByUserId(@Param("userId") String userId);
}
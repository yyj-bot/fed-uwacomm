package com.feduwacomm.mapper;

import com.feduwacomm.entity.VmTempPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 虚拟机临时权限Mapper接口
 */
@Mapper
public interface VmTempPermissionMapper {

    /**
     * 根据用户ID和虚拟机ID查询临时权限列表
     *
     * @param userId 用户ID
     * @param vmId 虚拟机ID
     * @return 临时权限列表
     */
    @Select("SELECT * FROM vm_temp_permissions WHERE user_id = #{userId} AND vm_id = #{vmId}")
    List<VmTempPermission> selectByUserIdAndVmId(@Param("userId") String userId, @Param("vmId") String vmId);

    /**
     * 根据用户ID查询所有临时权限
     *
     * @param userId 用户ID
     * @return 临时权限列表
     */
    @Select("SELECT * FROM vm_temp_permissions WHERE user_id = #{userId} AND status = 'ACTIVE'")
    List<VmTempPermission> selectByUserId(@Param("userId") String userId);

    /**
     * 根据虚拟机ID查询所有临时权限
     *
     * @param vmId 虚拟机ID
     * @return 临时权限列表
     */
    @Select("SELECT * FROM vm_temp_permissions WHERE vm_id = #{vmId} AND status = 'ACTIVE'")
    List<VmTempPermission> selectByVmId(@Param("vmId") String vmId);

    /**
     * 插入临时权限记录
     *
     * @param permission 临时权限实体
     * @return 影响行数
     */
    @Insert("INSERT INTO vm_temp_permissions (id, user_id, vm_id, status, permission_level, start_time, end_time, reason, granted_by, one_time_use, used, access_conditions, last_used_at, created_at, updated_at, created_by, updated_by) " +
            "VALUES (#{id}, #{userId}, #{vmId}, #{status}, #{permissionLevel}, #{startTime}, #{endTime}, #{reason}, #{grantedBy}, #{oneTimeUse}, #{used}, #{accessConditions}, #{lastUsedAt}, #{createdAt}, #{updatedAt}, #{createdBy}, #{updatedBy})")
    int insert(VmTempPermission permission);

    /**
     * 更新临时权限记录
     *
     * @param permission 临时权限实体
     * @return 影响行数
     */
    @Update("UPDATE vm_temp_permissions SET status = #{status}, permission_level = #{permissionLevel}, start_time = #{startTime}, end_time = #{endTime}, " +
            "reason = #{reason}, one_time_use = #{oneTimeUse}, used = #{used}, access_conditions = #{accessConditions}, " +
            "last_used_at = #{lastUsedAt}, updated_at = #{updatedAt}, updated_by = #{updatedBy} WHERE id = #{id}")
    int update(VmTempPermission permission);

    /**
     * 删除临时权限记录
     *
     * @param id 临时权限ID
     * @return 影响行数
     */
    @Delete("DELETE FROM vm_temp_permissions WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    /**
     * 根据ID查询临时权限记录
     *
     * @param id 临时权限ID
     * @return 临时权限实体
     */
    @Select("SELECT * FROM vm_temp_permissions WHERE id = #{id}")
    VmTempPermission selectById(@Param("id") String id);

    /**
     * 更新临时权限状态
     *
     * @param id 临时权限ID
     * @param status 新状态
     * @return 影响行数
     */
    @Update("UPDATE vm_temp_permissions SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);

    /**
     * 标记临时权限为已使用
     *
     * @param id 临时权限ID
     * @return 影响行数
     */
    @Update("UPDATE vm_temp_permissions SET used = true, last_used_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int markAsUsed(@Param("id") String id);

    /**
     * 查询有效的临时权限（时间范围内且未过期）
     *
     * @param userId 用户ID
     * @param vmId 虚拟机ID
     * @param currentTime 当前时间戳
     * @return 有效的临时权限列表
     */
    @Select("SELECT * FROM vm_temp_permissions WHERE user_id = #{userId} AND vm_id = #{vmId} AND status = 'ACTIVE' " +
            "AND start_time <= #{currentTime} AND end_time >= #{currentTime} " +
            "AND (one_time_use = false OR used = false)")
    List<VmTempPermission> selectValidPermissions(@Param("userId") String userId, @Param("vmId") String vmId, @Param("currentTime") Long currentTime);

    /**
     * 清理过期的临时权限
     *
     * @param currentTime 当前时间戳
     * @return 影响行数
     */
    @Update("UPDATE vm_temp_permissions SET status = 'EXPIRED', updated_at = NOW() " +
            "WHERE status = 'ACTIVE' AND end_time < #{currentTime}")
    int expireOldPermissions(@Param("currentTime") Long currentTime);
}
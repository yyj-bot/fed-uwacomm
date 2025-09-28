package com.feduwacomm.mapper;

import com.feduwacomm.entity.VmShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 虚拟机共享Mapper接口
 */
@Mapper
public interface VmShareMapper {

    /**
     * 根据虚拟机ID查询所有共享记录
     *
     * @param vmId 虚拟机ID
     * @return 共享记录列表
     */
    @Select("SELECT * FROM vm_shares WHERE vm_id = #{vmId} AND status = 'ACTIVE'")
    List<VmShare> selectByVmId(@Param("vmId") String vmId);

    /**
     * 根据用户ID查询所有被共享的虚拟机
     *
     * @param userId 用户ID
     * @return 共享记录列表
     */
    @Select("SELECT * FROM vm_shares WHERE shared_user_id = #{userId} AND status = 'ACTIVE'")
    List<VmShare> selectByUserId(@Param("userId") String userId);

    /**
     * 根据虚拟机ID和用户ID查询共享记录
     *
     * @param vmId 虚拟机ID
     * @param userId 用户ID
     * @return 共享记录实体
     */
    @Select("SELECT * FROM vm_shares WHERE vm_id = #{vmId} AND shared_user_id = #{userId} AND status = 'ACTIVE'")
    VmShare selectByVmIdAndUserId(@Param("vmId") String vmId, @Param("userId") String userId);

    /**
     * 插入共享记录
     *
     * @param share 共享记录实体
     * @return 影响行数
     */
    @Insert("INSERT INTO vm_shares (id, vm_id, shared_user_id, status, permission_level, start_time, end_time, reason, shared_by, allow_reshare, access_limit, access_count, created_at, updated_at, created_by, updated_by) " +
            "VALUES (#{id}, #{vmId}, #{sharedUserId}, #{status}, #{permissionLevel}, #{startTime}, #{endTime}, #{reason}, #{sharedBy}, #{allowReshare}, #{accessLimit}, #{accessCount}, #{createdAt}, #{updatedAt}, #{createdBy}, #{updatedBy})")
    int insert(VmShare share);

    /**
     * 更新共享记录
     *
     * @param share 共享记录实体
     * @return 影响行数
     */
    @Update("UPDATE vm_shares SET status = #{status}, permission_level = #{permissionLevel}, start_time = #{startTime}, end_time = #{endTime}, " +
            "reason = #{reason}, allow_reshare = #{allowReshare}, access_limit = #{accessLimit}, access_count = #{accessCount}, " +
            "updated_at = #{updatedAt}, updated_by = #{updatedBy} WHERE id = #{id}")
    int update(VmShare share);

    /**
     * 删除共享记录
     *
     * @param id 共享记录ID
     * @return 影响行数
     */
    @Delete("DELETE FROM vm_shares WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    /**
     * 根据ID查询共享记录
     *
     * @param id 共享记录ID
     * @return 共享记录实体
     */
    @Select("SELECT * FROM vm_shares WHERE id = #{id}")
    VmShare selectById(@Param("id") String id);

    /**
     * 更新共享状态
     *
     * @param id 共享记录ID
     * @param status 新状态
     * @return 影响行数
     */
    @Update("UPDATE vm_shares SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);

    /**
     * 增加访问计数
     *
     * @param id 共享记录ID
     * @return 影响行数
     */
    @Update("UPDATE vm_shares SET access_count = access_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementAccessCount(@Param("id") String id);

    /**
     * 检查用户是否有虚拟机的共享权限
     *
     * @param vmId 虚拟机ID
     * @param userId 用户ID
     * @return 是否有共享权限
     */
    @Select("SELECT COUNT(*) > 0 FROM vm_shares WHERE vm_id = #{vmId} AND shared_user_id = #{userId} AND status = 'ACTIVE' " +
            "AND (end_time IS NULL OR end_time > NOW()) AND (access_limit IS NULL OR access_count < access_limit)")
    boolean hasActiveShare(@Param("vmId") String vmId, @Param("userId") String userId);
}
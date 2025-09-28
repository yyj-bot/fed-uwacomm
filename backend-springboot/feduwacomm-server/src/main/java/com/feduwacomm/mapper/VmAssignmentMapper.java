package com.feduwacomm.mapper;

import com.feduwacomm.entity.VmAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 虚拟机分配关系Mapper接口
 */
@Mapper
public interface VmAssignmentMapper {

    /**
     * 根据用户ID和虚拟机ID查询分配关系
     *
     * @param userId 用户ID
     * @param vmId 虚拟机ID
     * @return 分配关系实体
     */
    @Select("SELECT * FROM vm_assignments WHERE user_id = #{userId} AND vm_id = #{vmId} AND status = 'ACTIVE'")
    VmAssignment selectByUserIdAndVmId(@Param("userId") String userId, @Param("vmId") String vmId);

    /**
     * 根据用户ID查询所有分配的虚拟机
     *
     * @param userId 用户ID
     * @return 分配关系列表
     */
    @Select("SELECT * FROM vm_assignments WHERE user_id = #{userId} AND status = 'ACTIVE'")
    List<VmAssignment> selectByUserId(@Param("userId") String userId);

    /**
     * 根据虚拟机ID查询所有分配的用户
     *
     * @param vmId 虚拟机ID
     * @return 分配关系列表
     */
    @Select("SELECT * FROM vm_assignments WHERE vm_id = #{vmId} AND status = 'ACTIVE'")
    List<VmAssignment> selectByVmId(@Param("vmId") String vmId);

    /**
     * 插入分配关系
     *
     * @param assignment 分配关系实体
     * @return 影响行数
     */
    @Insert("INSERT INTO vm_assignments (id, user_id, vm_id, status, permission_level, start_time, end_time, reason, assigned_by, created_at, updated_at, created_by, updated_by) " +
            "VALUES (#{id}, #{userId}, #{vmId}, #{status}, #{permissionLevel}, #{startTime}, #{endTime}, #{reason}, #{assignedBy}, #{createdAt}, #{updatedAt}, #{createdBy}, #{updatedBy})")
    int insert(VmAssignment assignment);

    /**
     * 更新分配关系
     *
     * @param assignment 分配关系实体
     * @return 影响行数
     */
    @Update("UPDATE vm_assignments SET status = #{status}, permission_level = #{permissionLevel}, start_time = #{startTime}, end_time = #{endTime}, " +
            "reason = #{reason}, updated_at = #{updatedAt}, updated_by = #{updatedBy} WHERE id = #{id}")
    int update(VmAssignment assignment);

    /**
     * 删除分配关系
     *
     * @param id 分配关系ID
     * @return 影响行数
     */
    @Delete("DELETE FROM vm_assignments WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    /**
     * 根据ID查询分配关系
     *
     * @param id 分配关系ID
     * @return 分配关系实体
     */
    @Select("SELECT * FROM vm_assignments WHERE id = #{id}")
    VmAssignment selectById(@Param("id") String id);

    /**
     * 更新分配状态
     *
     * @param id 分配关系ID
     * @param status 新状态
     * @return 影响行数
     */
    @Update("UPDATE vm_assignments SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);
}
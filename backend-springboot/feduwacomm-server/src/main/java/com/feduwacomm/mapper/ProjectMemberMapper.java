package com.feduwacomm.mapper;

import com.feduwacomm.entity.ProjectMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 项目成员Mapper接口
 */
@Mapper
public interface ProjectMemberMapper {

    /**
     * 根据项目ID和用户ID查询成员关系
     *
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 成员关系实体
     */
    @Select("SELECT * FROM project_members WHERE project_id = #{projectId} AND user_id = #{userId} AND status = 'ACTIVE'")
    ProjectMember selectByProjectIdAndUserId(@Param("projectId") String projectId, @Param("userId") String userId);

    /**
     * 根据项目ID查询所有成员
     *
     * @param projectId 项目ID
     * @return 成员关系列表
     */
    @Select("SELECT * FROM project_members WHERE project_id = #{projectId} AND status = 'ACTIVE'")
    List<ProjectMember> selectByProjectId(@Param("projectId") String projectId);

    /**
     * 根据用户ID查询所有参与的项目
     *
     * @param userId 用户ID
     * @return 成员关系列表
     */
    @Select("SELECT * FROM project_members WHERE user_id = #{userId} AND status = 'ACTIVE'")
    List<ProjectMember> selectByUserId(@Param("userId") String userId);

    /**
     * 插入项目成员关系
     *
     * @param member 成员关系实体
     * @return 影响行数
     */
    @Insert("INSERT INTO project_members (id, project_id, user_id, status, role, permissions, joined_at, left_at, invited_by, created_at, updated_at, created_by, updated_by) " +
            "VALUES (#{id}, #{projectId}, #{userId}, #{status}, #{role}, #{permissions}, #{joinedAt}, #{leftAt}, #{invitedBy}, #{createdAt}, #{updatedAt}, #{createdBy}, #{updatedBy})")
    int insert(ProjectMember member);

    /**
     * 更新项目成员关系
     *
     * @param member 成员关系实体
     * @return 影响行数
     */
    @Update("UPDATE project_members SET status = #{status}, role = #{role}, permissions = #{permissions}, left_at = #{leftAt}, " +
            "updated_at = #{updatedAt}, updated_by = #{updatedBy} WHERE id = #{id}")
    int update(ProjectMember member);

    /**
     * 删除项目成员关系
     *
     * @param id 成员关系ID
     * @return 影响行数
     */
    @Delete("DELETE FROM project_members WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    /**
     * 根据ID查询成员关系
     *
     * @param id 成员关系ID
     * @return 成员关系实体
     */
    @Select("SELECT * FROM project_members WHERE id = #{id}")
    ProjectMember selectById(@Param("id") String id);

    /**
     * 更新成员状态
     *
     * @param id 成员关系ID
     * @param status 新状态
     * @return 影响行数
     */
    @Update("UPDATE project_members SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);

    /**
     * 检查用户是否为项目成员
     *
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 是否为成员
     */
    @Select("SELECT COUNT(*) > 0 FROM project_members WHERE project_id = #{projectId} AND user_id = #{userId} AND status = 'ACTIVE'")
    boolean isProjectMember(@Param("projectId") String projectId, @Param("userId") String userId);
}
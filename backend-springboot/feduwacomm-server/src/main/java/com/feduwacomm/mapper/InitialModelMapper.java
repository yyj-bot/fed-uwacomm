package com.feduwacomm.mapper;

import com.feduwacomm.entity.InitialModel;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 初始模型数据访问层
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Mapper
public interface InitialModelMapper {
    
    /**
     * 插入初始模型记录
     */
    @Insert("INSERT INTO initial_models (id, task_id, model_type, generation_method, " +
            "model_size, architecture_params, model_data, status, " +
            "created_at, created_by, updated_at) " +
            "VALUES (#{id}, #{taskId}, #{modelType}, #{generationMethod}, " +
            "#{modelSize}, #{architectureParams}, #{modelData}, #{status}, " +
            "#{createdAt}, #{createdBy}, #{updatedAt})")
    int insertInitialModel(InitialModel initialModel);
    
    /**
     * 根据ID查询初始模型
     */
    @Select("SELECT * FROM initial_models WHERE id = #{id}")
    InitialModel selectById(String id);
    
    /**
     * 根据任务ID查询初始模型列表
     */
    @Select("SELECT * FROM initial_models WHERE task_id = #{taskId} ORDER BY created_at DESC")
    List<InitialModel> selectByTaskId(String taskId);
    
    /**
     * 根据任务ID和生成方式查询初始模型
     */
    @Select("SELECT * FROM initial_models WHERE task_id = #{taskId} " +
            "AND generation_method = #{generationMethod} ORDER BY created_at DESC")
    List<InitialModel> selectByTaskIdAndMethod(String taskId, String generationMethod);
    
    /**
     * 更新模型状态
     */
    @Update("UPDATE initial_models SET status = #{status}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);
    
    /**
     * 更新模型JSON数据
     */
    @Update("UPDATE initial_models SET model_data = #{modelData}, " +
            "status = #{status}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateModelData(@Param("id") String id, @Param("modelData") String modelData,
                       @Param("status") String status);

    /**
     * 更新模型JSON数据和大小
     */
    @Update("UPDATE initial_models SET model_data = #{modelData}, " +
            "model_size = #{modelSize}, status = #{status}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateModelDataAndSize(@Param("id") String id, @Param("modelData") String modelData,
                               @Param("modelSize") Long modelSize, @Param("status") String status);

    /**
     * 根据状态查询初始模型列表
     */
    @Select("SELECT * FROM initial_models WHERE status = #{status} ORDER BY created_at DESC")
    List<InitialModel> selectByStatus(String status);
    
    /**
     * 统计任务的初始模型数量
     */
    @Select("SELECT COUNT(*) FROM initial_models WHERE task_id = #{taskId}")
    int countByTaskId(String taskId);
    
    /**
     * 统计任务特定状态的初始模型数量
     */
    @Select("SELECT COUNT(*) FROM initial_models WHERE task_id = #{taskId} AND status = #{status}")
    int countByTaskIdAndStatus(@Param("taskId") String taskId, @Param("status") String status);
    
    /**
     * 删除初始模型记录
     */
    @Delete("DELETE FROM initial_models WHERE id = #{id}")
    int deleteById(String id);
    
    /**
     * 根据任务ID删除所有初始模型记录
     */
    @Delete("DELETE FROM initial_models WHERE task_id = #{taskId}")
    int deleteByTaskId(String taskId);
    
    /**
     * 查询待清理的模型(状态为FAILED且创建时间超过指定天数)
     */
    @Select("SELECT * FROM initial_models WHERE status = 'FAILED' " +
            "AND created_at < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    List<InitialModel> selectFailedModelsOlderThan(@Param("days") int days);
}
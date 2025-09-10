package com.feduwacomm.mapper;

import com.feduwacomm.entity.ModelDistribution;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模型分发记录数据访问层
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Mapper
public interface ModelDistributionMapper {
    
    /**
     * 插入模型分发记录
     */
    @Insert("INSERT INTO model_distributions (id, model_id, vm_id, distribution_status, " +
            "distributed_at, verified_at, error_message, checksum_verified, created_at) " +
            "VALUES (#{id}, #{modelId}, #{vmId}, #{distributionStatus}, " +
            "#{distributedAt}, #{verifiedAt}, #{errorMessage}, #{checksumVerified}, #{createdAt})")
    int insertModelDistribution(ModelDistribution distribution);
    
    /**
     * 批量插入模型分发记录
     */
    @Insert("<script>" +
            "INSERT INTO model_distributions (id, model_id, vm_id, distribution_status, created_at) VALUES " +
            "<foreach collection='distributions' item='item' separator=','>" +
            "(#{item.id}, #{item.modelId}, #{item.vmId}, #{item.distributionStatus}, #{item.createdAt})" +
            "</foreach>" +
            "</script>")
    int batchInsertModelDistributions(@Param("distributions") List<ModelDistribution> distributions);
    
    /**
     * 根据ID查询分发记录
     */
    @Select("SELECT * FROM model_distributions WHERE id = #{id}")
    ModelDistribution selectById(String id);
    
    /**
     * 根据模型ID查询所有分发记录
     */
    @Select("SELECT md.*, vm.name as vm_name, vm.ip_address as vm_ip " +
            "FROM model_distributions md " +
            "LEFT JOIN vm_instances vm ON md.vm_id = vm.id " +
            "WHERE md.model_id = #{modelId} ORDER BY md.created_at DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "modelId", column = "model_id"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "distributionStatus", column = "distribution_status"),
        @Result(property = "distributedAt", column = "distributed_at"),
        @Result(property = "verifiedAt", column = "verified_at"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "checksumVerified", column = "checksum_verified"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<Map<String, Object>> selectByModelIdWithVmInfo(String modelId);
    
    /**
     * 根据虚拟机ID查询分发记录
     */
    @Select("SELECT * FROM model_distributions WHERE vm_id = #{vmId} ORDER BY created_at DESC")
    List<ModelDistribution> selectByVmId(String vmId);
    
    /**
     * 根据模型ID和虚拟机ID查询分发记录
     */
    @Select("SELECT * FROM model_distributions WHERE model_id = #{modelId} AND vm_id = #{vmId}")
    ModelDistribution selectByModelIdAndVmId(@Param("modelId") String modelId, @Param("vmId") String vmId);
    
    /**
     * 更新分发状态
     */
    @Update("UPDATE model_distributions SET distribution_status = #{status}, " +
            "distributed_at = #{distributedAt}, error_message = #{errorMessage} " +
            "WHERE id = #{id}")
    int updateDistributionStatus(@Param("id") String id, @Param("status") String status,
                               @Param("distributedAt") LocalDateTime distributedAt,
                               @Param("errorMessage") String errorMessage);
    
    /**
     * 更新验证状态
     */
    @Update("UPDATE model_distributions SET checksum_verified = #{verified}, " +
            "verified_at = #{verifiedAt} WHERE id = #{id}")
    int updateVerificationStatus(@Param("id") String id, @Param("verified") Boolean verified,
                               @Param("verifiedAt") LocalDateTime verifiedAt);
    
    /**
     * 统计模型的分发状态
     */
    @Select("SELECT distribution_status as status, COUNT(*) as count " +
            "FROM model_distributions WHERE model_id = #{modelId} " +
            "GROUP BY distribution_status")
    List<Map<String, Object>> countDistributionStatusByModelId(String modelId);
    
    /**
     * 统计模型分发进度
     */
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN distribution_status = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
            "SUM(CASE WHEN distribution_status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgress, " +
            "SUM(CASE WHEN distribution_status = 'FAILED' THEN 1 ELSE 0 END) as failed, " +
            "SUM(CASE WHEN distribution_status = 'PENDING' THEN 1 ELSE 0 END) as pending " +
            "FROM model_distributions WHERE model_id = #{modelId}")
    Map<String, Object> getDistributionProgress(String modelId);
    
    /**
     * 查询待分发的记录
     */
    @Select("SELECT * FROM model_distributions WHERE distribution_status = 'PENDING' " +
            "ORDER BY created_at ASC LIMIT #{limit}")
    List<ModelDistribution> selectPendingDistributions(@Param("limit") int limit);
    
    /**
     * 查询超时的分发记录
     */
    @Select("SELECT * FROM model_distributions WHERE distribution_status = 'IN_PROGRESS' " +
            "AND created_at < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    List<ModelDistribution> selectTimeoutDistributions(@Param("timeoutMinutes") int timeoutMinutes);
    
    /**
     * 删除分发记录
     */
    @Delete("DELETE FROM model_distributions WHERE id = #{id}")
    int deleteById(String id);
    
    /**
     * 根据模型ID删除所有分发记录
     */
    @Delete("DELETE FROM model_distributions WHERE model_id = #{modelId}")
    int deleteByModelId(String modelId);
    
    /**
     * 重置分发状态(用于重新分发)
     */
    @Update("UPDATE model_distributions SET distribution_status = 'PENDING', " +
            "distributed_at = NULL, verified_at = NULL, error_message = NULL, " +
            "checksum_verified = FALSE WHERE model_id = #{modelId}")
    int resetDistributionStatus(String modelId);
}
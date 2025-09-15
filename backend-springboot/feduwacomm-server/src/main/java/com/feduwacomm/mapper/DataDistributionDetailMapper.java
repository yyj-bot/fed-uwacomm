package com.feduwacomm.mapper;

import com.feduwacomm.entity.DataDistributionDetail;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据分发详情数据访问层
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Mapper
public interface DataDistributionDetailMapper {

    /**
     * 插入数据分发详情
     */
    @Insert("INSERT INTO data_distribution_details (id, distribution_id, dataset_id, vm_id, " +
            "status, data_size, transferred_size, checksum, distributed_at, verified_at, " +
            "error_message, created_at) " +
            "VALUES (#{id}, #{distributionId}, #{datasetId}, #{vmId}, " +
            "#{status}, #{dataSize}, #{transferredSize}, #{checksum}, #{distributedAt}, #{verifiedAt}, " +
            "#{errorMessage}, #{createdAt})")
    int insertDetail(DataDistributionDetail detail);

    /**
     * 批量插入数据分发详情
     */
    @Insert("<script>" +
            "INSERT INTO data_distribution_details (id, distribution_id, dataset_id, vm_id, " +
            "status, data_size, transferred_size, checksum, created_at) VALUES " +
            "<foreach collection='details' item='item' separator=','>" +
            "(#{item.id}, #{item.distributionId}, #{item.datasetId}, #{item.vmId}, " +
            "#{item.status}, #{item.dataSize}, #{item.transferredSize}, #{item.checksum}, #{item.createdAt})" +
            "</foreach>" +
            "</script>")
    int batchInsertDetails(@Param("details") List<DataDistributionDetail> details);

    /**
     * 根据ID查询分发详情
     */
    @Select("SELECT * FROM data_distribution_details WHERE id = #{id}")
    DataDistributionDetail selectById(String id);

    /**
     * 根据分发ID查询所有详情
     */
    @Select("SELECT ddd.*, vm.name as vm_name, vm.ip_address as vm_ip, " +
            "td.name as dataset_name, td.data_type as dataset_type " +
            "FROM data_distribution_details ddd " +
            "LEFT JOIN vm_instances vm ON ddd.vm_id = vm.id " +
            "LEFT JOIN training_dataset td ON ddd.dataset_id = td.id " +
            "WHERE ddd.distribution_id = #{distributionId} ORDER BY ddd.created_at DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "distributionId", column = "distribution_id"),
        @Result(property = "datasetId", column = "dataset_id"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "status", column = "status"),
        @Result(property = "dataSize", column = "data_size"),
        @Result(property = "transferredSize", column = "transferred_size"),
        @Result(property = "checksum", column = "checksum"),
        @Result(property = "distributedAt", column = "distributed_at"),
        @Result(property = "verifiedAt", column = "verified_at"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<java.util.Map<String, Object>> selectByDistributionIdWithInfo(String distributionId);

    /**
     * 根据虚拟机ID查询分发详情
     */
    @Select("SELECT * FROM data_distribution_details WHERE vm_id = #{vmId} " +
            "ORDER BY created_at DESC")
    List<DataDistributionDetail> selectByVmId(String vmId);

    /**
     * 根据数据集ID查询分发详情
     */
    @Select("SELECT * FROM data_distribution_details WHERE dataset_id = #{datasetId} " +
            "ORDER BY created_at DESC")
    List<DataDistributionDetail> selectByDatasetId(String datasetId);

    /**
     * 根据状态查询分发详情
     */
    @Select("SELECT * FROM data_distribution_details WHERE status = #{status} " +
            "ORDER BY created_at DESC")
    List<DataDistributionDetail> selectByStatus(String status);

    /**
     * 根据分发ID和状态查询详情
     */
    @Select("SELECT * FROM data_distribution_details WHERE distribution_id = #{distributionId} " +
            "AND status = #{status} ORDER BY created_at DESC")
    List<DataDistributionDetail> selectByDistributionIdAndStatus(@Param("distributionId") String distributionId,
                                                                @Param("status") String status);

    /**
     * 根据分发ID和虚拟机ID查询详情
     */
    @Select("SELECT * FROM data_distribution_details WHERE distribution_id = #{distributionId} " +
            "AND vm_id = #{vmId}")
    List<DataDistributionDetail> selectByDistributionIdAndVmId(@Param("distributionId") String distributionId,
                                                              @Param("vmId") String vmId);

    /**
     * 更新分发状态
     */
    @Update("UPDATE data_distribution_details SET status = #{status}, " +
            "distributed_at = #{distributedAt}, error_message = #{errorMessage} " +
            "WHERE id = #{id}")
    int updateStatus(@Param("id") String id, 
                    @Param("status") String status,
                    @Param("distributedAt") LocalDateTime distributedAt,
                    @Param("errorMessage") String errorMessage);

    /**
     * 更新传输进度
     */
    @Update("UPDATE data_distribution_details SET transferred_size = #{transferredSize} " +
            "WHERE id = #{id}")
    int updateTransferredSize(@Param("id") String id, @Param("transferredSize") Long transferredSize);

    /**
     * 更新验证状态
     */
    @Update("UPDATE data_distribution_details SET verified_at = #{verifiedAt}, " +
            "checksum = #{checksum} WHERE id = #{id}")
    int updateVerificationStatus(@Param("id") String id, 
                                @Param("verifiedAt") LocalDateTime verifiedAt,
                                @Param("checksum") String checksum);

    /**
     * 删除分发详情
     */
    @Delete("DELETE FROM data_distribution_details WHERE id = #{id}")
    int deleteById(String id);

    /**
     * 根据分发ID删除所有详情
     */
    @Delete("DELETE FROM data_distribution_details WHERE distribution_id = #{distributionId}")
    int deleteByDistributionId(String distributionId);

    /**
     * 统计分发详情状态
     */
    @Select("SELECT status, COUNT(*) as count FROM data_distribution_details " +
            "WHERE distribution_id = #{distributionId} GROUP BY status")
    List<java.util.Map<String, Object>> countStatusByDistributionId(String distributionId);

    /**
     * 统计分发进度
     */
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
            "SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgress, " +
            "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed, " +
            "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending, " +
            "SUM(data_size) as totalDataSize, " +
            "SUM(transferred_size) as totalTransferredSize " +
            "FROM data_distribution_details WHERE distribution_id = #{distributionId}")
    java.util.Map<String, Object> getDistributionProgress(String distributionId);

    /**
     * 查询待分发的详情
     */
    @Select("SELECT * FROM data_distribution_details WHERE status = 'PENDING' " +
            "ORDER BY created_at ASC LIMIT #{limit}")
    List<DataDistributionDetail> selectPendingDetails(@Param("limit") int limit);

    /**
     * 查询超时的分发详情
     */
    @Select("SELECT * FROM data_distribution_details WHERE status = 'IN_PROGRESS' " +
            "AND created_at < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    List<DataDistributionDetail> selectTimeoutDetails(@Param("timeoutMinutes") int timeoutMinutes);

    /**
     * 统计虚拟机分发数据量
     */
    @Select("SELECT vm_id, SUM(data_size) as total_size, COUNT(*) as count " +
            "FROM data_distribution_details WHERE distribution_id = #{distributionId} " +
            "AND status = 'COMPLETED' GROUP BY vm_id")
    List<java.util.Map<String, Object>> getVmDataStatistics(String distributionId);

    /**
     * 重置分发状态(用于重新分发)
     */
    @Update("UPDATE data_distribution_details SET status = 'PENDING', " +
            "distributed_at = NULL, verified_at = NULL, error_message = NULL, " +
            "transferred_size = 0 WHERE distribution_id = #{distributionId}")
    int resetDistributionStatus(String distributionId);

    /**
     * 获取数据分发平衡性统计
     */
    @Select("SELECT " +
            "vm_id, " +
            "COUNT(*) as dataset_count, " +
            "SUM(data_size) as total_data_size, " +
            "AVG(data_size) as avg_data_size, " +
            "MIN(data_size) as min_data_size, " +
            "MAX(data_size) as max_data_size " +
            "FROM data_distribution_details " +
            "WHERE distribution_id = #{distributionId} AND status = 'COMPLETED' " +
            "GROUP BY vm_id")
    List<java.util.Map<String, Object>> getDataBalanceStatistics(String distributionId);
}
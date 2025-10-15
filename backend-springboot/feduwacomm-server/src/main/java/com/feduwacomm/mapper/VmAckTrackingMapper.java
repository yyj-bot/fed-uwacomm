package com.feduwacomm.mapper;

import com.feduwacomm.entity.VmAckTracking;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VM确认跟踪Mapper接口
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Mapper
public interface VmAckTrackingMapper {

    /**
     * 插入ACK跟踪记录
     */
    @Insert("""
        INSERT INTO vm_ack_tracking (
            task_id, round_number, vm_id, ack_type, status, message_id,
            ack_data, error_message, acknowledged_at, timeout_at
        ) VALUES (
            #{taskId}, #{roundNumber}, #{vmId}, #{ackType}, #{status}, #{messageId},
            #{ackData}, #{errorMessage}, #{acknowledgedAt}, #{timeoutAt}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAckTracking(VmAckTracking ackTracking);

    /**
     * 通用插入方法（别名）
     */
    default int insert(VmAckTracking ackTracking) {
        return insertAckTracking(ackTracking);
    }

    /**
     * 更新ACK状态
     */
    @Update("""
        UPDATE vm_ack_tracking
        SET status = #{status},
            ack_data = #{ackData},
            error_message = #{errorMessage},
            acknowledged_at = #{acknowledgedAt},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    int updateAckStatus(VmAckTracking ackTracking);

    /**
     * 根据任务ID和VM ID查询指定类型的ACK记录
     */
    @Select("""
        SELECT * FROM vm_ack_tracking
        WHERE task_id = #{taskId}
        AND vm_id = #{vmId}
        AND ack_type = #{ackType}
        AND (#{roundNumber} IS NULL OR round_number = #{roundNumber})
        ORDER BY created_at DESC
        LIMIT 1
    """)
    VmAckTracking selectLatestAck(@Param("taskId") String taskId,
                                  @Param("vmId") String vmId,
                                  @Param("ackType") VmAckTracking.AckType ackType,
                                  @Param("roundNumber") Integer roundNumber);

    /**
     * 查询任务的所有VM的指定类型ACK状态
     */
    @Select("""
        SELECT va.* FROM vm_ack_tracking va
        INNER JOIN (
            SELECT vm_id, MAX(created_at) as max_created
            FROM vm_ack_tracking
            WHERE task_id = #{taskId}
            AND ack_type = #{ackType}
            AND (#{roundNumber} IS NULL OR round_number = #{roundNumber})
            GROUP BY vm_id
        ) latest ON va.vm_id = latest.vm_id AND va.created_at = latest.max_created
        WHERE va.task_id = #{taskId}
        AND va.ack_type = #{ackType}
        AND (#{roundNumber} IS NULL OR va.round_number = #{roundNumber})
    """)
    List<VmAckTracking> selectAllVmAcksByType(@Param("taskId") String taskId,
                                              @Param("ackType") VmAckTracking.AckType ackType,
                                              @Param("roundNumber") Integer roundNumber);

    /**
     * 统计任务中指定状态的ACK数量
     */
    @Select("""
        SELECT COUNT(DISTINCT vm_id) FROM vm_ack_tracking
        WHERE task_id = #{taskId}
        AND ack_type = #{ackType}
        AND status = #{status}
        AND (#{roundNumber} IS NULL OR round_number = #{roundNumber})
        AND id IN (
            SELECT MAX(id) FROM vm_ack_tracking
            WHERE task_id = #{taskId}
            AND ack_type = #{ackType}
            AND (#{roundNumber} IS NULL OR round_number = #{roundNumber})
            GROUP BY vm_id
        )
    """)
    int countVmsByAckStatus(@Param("taskId") String taskId,
                           @Param("ackType") VmAckTracking.AckType ackType,
                           @Param("status") VmAckTracking.AckStatus status,
                           @Param("roundNumber") Integer roundNumber);

    /**
     * 统计任务中参与指定ACK类型的VM总数
     */
    @Select("""
        SELECT COUNT(DISTINCT vm_id) FROM vm_ack_tracking
        WHERE task_id = #{taskId}
        AND ack_type = #{ackType}
        AND (#{roundNumber} IS NULL OR round_number = #{roundNumber})
    """)
    int countTotalVmsByAckType(@Param("taskId") String taskId,
                              @Param("ackType") VmAckTracking.AckType ackType,
                              @Param("roundNumber") Integer roundNumber);

    /**
     * 查询超时的ACK记录
     */
    @Select("""
        SELECT * FROM vm_ack_tracking
        WHERE status = 'PENDING'
        AND timeout_at < #{currentTime}
    """)
    List<VmAckTracking> selectTimeoutAcks(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 批量更新超时的ACK状态
     */
    @Update("""
        UPDATE vm_ack_tracking
        SET status = 'TIMEOUT',
            updated_at = CURRENT_TIMESTAMP
        WHERE status = 'PENDING'
        AND timeout_at < #{currentTime}
    """)
    int updateTimeoutAcks(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 删除过期的ACK记录（清理历史数据）
     */
    @Delete("""
        DELETE FROM vm_ack_tracking
        WHERE created_at < #{expireTime}
        AND status IN ('SUCCESS', 'FAILED', 'TIMEOUT')
    """)
    int deleteExpiredAcks(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 根据任务ID删除所有相关ACK记录
     */
    @Delete("DELETE FROM vm_ack_tracking WHERE task_id = #{taskId}")
    int deleteByTaskId(@Param("taskId") String taskId);

    /**
     * 根据VM ID删除所有相关ACK记录
     */
    @Delete("DELETE FROM vm_ack_tracking WHERE vm_id = #{vmId}")
    int deleteByVmId(@Param("vmId") String vmId);

    /**
     * 根据任务ID和确认类型查询ACK记录
     */
    @Select("""
        SELECT * FROM vm_ack_tracking
        WHERE task_id = #{taskId}
        AND ack_type = #{ackType}
        ORDER BY created_at DESC
    """)
    List<VmAckTracking> findByTaskIdAndAckType(@Param("taskId") String taskId,
                                              @Param("ackType") String ackType);

    /**
     * 根据任务ID查询所有ACK记录
     */
    @Select("""
        SELECT * FROM vm_ack_tracking
        WHERE task_id = #{taskId}
        ORDER BY created_at DESC
    """)
    List<VmAckTracking> findByTaskId(@Param("taskId") String taskId);

    /**
     * 查询所有ACK跟踪记录（用于缓存预热）
     */
    @Select("""
        SELECT * FROM vm_ack_tracking
        ORDER BY task_id, ack_type, created_at DESC
    """)
    List<VmAckTracking> selectAllAckTrackings();

    /**
     * 查询任务指定ACK类型的最新轮次号
     */
    @Select("""
        SELECT MAX(round_number)
        FROM vm_ack_tracking
        WHERE task_id = #{taskId}
          AND ack_type = #{ackType}
    """)
    Integer findLatestRoundNumber(@Param("taskId") String taskId,
                                  @Param("ackType") VmAckTracking.AckType ackType);
}

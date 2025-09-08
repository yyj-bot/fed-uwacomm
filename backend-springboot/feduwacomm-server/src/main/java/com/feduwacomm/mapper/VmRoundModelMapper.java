package com.feduwacomm.mapper;

import com.feduwacomm.entity.VmRoundModel;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 虚拟机轮次模型结果Mapper接口
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Mapper
public interface VmRoundModelMapper {

    /**
     * 分页查询虚拟机轮次模型结果
     *
     * @param taskId 任务ID
     * @param roundNumber 轮数
     * @param vmId 虚拟机ID
     * @param offset 偏移量
     * @param size 页大小
     * @return 虚拟机轮次模型结果列表
     */
    @Select({
        "<script>",
        "SELECT id, task_id, vm_id, round_number, accuracy, loss, created_at, parameters",
        "FROM vm_round_models",
        "WHERE 1=1",
        "<if test='taskId != null'>",
        "  AND task_id = #{taskId}",
        "</if>",
        "<if test='roundNumber != null'>",
        "  AND round_number = #{roundNumber}",
        "</if>",
        "<if test='vmId != null'>",
        "  AND vm_id = #{vmId}",
        "</if>",
        "ORDER BY created_at DESC",
        "LIMIT #{offset}, #{size}",
        "</script>"
    })
    List<VmRoundModel> selectByPage(@Param("taskId") String taskId,
                                   @Param("roundNumber") Integer roundNumber,
                                   @Param("vmId") String vmId,
                                   @Param("offset") Integer offset,
                                   @Param("size") Integer size);

    /**
     * 统计符合条件的记录总数
     *
     * @param taskId 任务ID
     * @param roundNumber 轮数
     * @param vmId 虚拟机ID
     * @return 总记录数
     */
    @Select({
        "<script>",
        "SELECT COUNT(*)",
        "FROM vm_round_models",
        "WHERE 1=1",
        "<if test='taskId != null'>",
        "  AND task_id = #{taskId}",
        "</if>",
        "<if test='roundNumber != null'>",
        "  AND round_number = #{roundNumber}",
        "</if>",
        "<if test='vmId != null'>",
        "  AND vm_id = #{vmId}",
        "</if>",
        "</script>"
    })
    Long countByConditions(@Param("taskId") String taskId,
                          @Param("roundNumber") Integer roundNumber,
                          @Param("vmId") String vmId);

    /**
     * 根据ID查询虚拟机轮次模型结果详情
     *
     * @param id 主键ID
     * @return 虚拟机轮次模型结果
     */
    @Select("SELECT id, task_id, vm_id, round_number, accuracy, loss, created_at, parameters " +
           "FROM vm_round_models WHERE id = #{id}")
    VmRoundModel selectById(@Param("id") String id);

    /**
     * 查询虚拟机在指定任务下的训练指标趋势
     *
     * @param taskId 任务ID
     * @param vmId 虚拟机ID
     * @param metric 指标名称
     * @return 指标趋势数据
     */
    @Select({
        "<script>",
        "SELECT round_number, ",
        "<choose>",
        "  <when test='metric == \"accuracy\"'>accuracy as value</when>",
        "  <when test='metric == \"loss\"'>loss as value</when>",
        "  <otherwise>NULL as value</otherwise>",
        "</choose>",
        "FROM vm_round_models",
        "WHERE task_id = #{taskId} AND vm_id = #{vmId}",
        "AND ",
        "<choose>",
        "  <when test='metric == \"accuracy\"'>accuracy IS NOT NULL</when>",
        "  <when test='metric == \"loss\"'>loss IS NOT NULL</when>",
        "  <otherwise>1=0</otherwise>",
        "</choose>",
        "ORDER BY round_number ASC",
        "</script>"
    })
    @Results({
        @Result(property = "roundNumber", column = "round_number"),
        @Result(property = "value", column = "value")
    })
    List<VmRoundModelTrendPoint> selectTrendByTaskAndVm(@Param("taskId") String taskId,
                                                       @Param("vmId") String vmId,
                                                       @Param("metric") String metric);

    /**
     * 查询最佳指标的虚拟机轮次模型
     *
     * @param taskId 任务ID
     * @param metric 指标名称
     * @return 最佳模型结果
     */
    @Select({
        "<script>",
        "SELECT id, round_number, vm_id, ",
        "<choose>",
        "  <when test='metric == \"accuracy\"'>accuracy as value</when>",
        "  <when test='metric == \"loss\"'>loss as value</when>",
        "  <otherwise>NULL as value</otherwise>",
        "</choose>",
        "FROM vm_round_models",
        "WHERE task_id = #{taskId}",
        "AND ",
        "<choose>",
        "  <when test='metric == \"accuracy\"'>accuracy IS NOT NULL</when>",
        "  <when test='metric == \"loss\"'>loss IS NOT NULL</when>",
        "  <otherwise>1=0</otherwise>",
        "</choose>",
        "ORDER BY ",
        "<choose>",
        "  <when test='metric == \"accuracy\"'>accuracy DESC</when>",
        "  <when test='metric == \"loss\"'>loss ASC</when>",
        "  <otherwise>id</otherwise>",
        "</choose>",
        "LIMIT 1",
        "</script>"
    })
    @Results({
        @Result(property = "vmRoundModelId", column = "id"),
        @Result(property = "roundNumber", column = "round_number"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "value", column = "value")
    })
    VmRoundModelBestResult selectBestByTaskAndMetric(@Param("taskId") String taskId,
                                                    @Param("metric") String metric);

    /**
     * 查询离群指标的虚拟机轮次模型
     *
     * @param taskId 任务ID
     * @param metric 指标名称
     * @return 离群模型结果
     */
    @Select({
        "<script>",
        "SELECT id, round_number, vm_id, ",
        "<choose>",
        "  <when test='metric == \"accuracy\"'>accuracy as value</when>",
        "  <when test='metric == \"loss\"'>loss as value</when>",
        "  <otherwise>NULL as value</otherwise>",
        "</choose>",
        "FROM vm_round_models",
        "WHERE task_id = #{taskId}",
        "AND ",
        "<choose>",
        "  <when test='metric == \"accuracy\"'>accuracy IS NOT NULL</when>",
        "  <when test='metric == \"loss\"'>loss IS NOT NULL</when>",
        "  <otherwise>1=0</otherwise>",
        "</choose>",
        "ORDER BY ",
        "<choose>",
        "  <when test='metric == \"accuracy\"'>accuracy ASC</when>",
        "  <when test='metric == \"loss\"'>loss DESC</when>",
        "  <otherwise>id</otherwise>",
        "</choose>",
        "LIMIT 1",
        "</script>"
    })
    @Results({
        @Result(property = "vmRoundModelId", column = "id"),
        @Result(property = "roundNumber", column = "round_number"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "value", column = "value")
    })
    VmRoundModelBestResult selectOutlierByTaskAndMetric(@Param("taskId") String taskId,
                                                       @Param("metric") String metric);

    /**
     * 趋势查询结果内部类
     */
    class VmRoundModelTrendPoint {
        private Integer roundNumber;
        private java.math.BigDecimal value;

        public Integer getRoundNumber() {
            return roundNumber;
        }

        public void setRoundNumber(Integer roundNumber) {
            this.roundNumber = roundNumber;
        }

        public java.math.BigDecimal getValue() {
            return value;
        }

        public void setValue(java.math.BigDecimal value) {
            this.value = value;
        }
    }

    /**
     * 最佳/离群查询结果内部类
     */
    class VmRoundModelBestResult {
        private String vmRoundModelId;
        private Integer roundNumber;
        private String vmId;
        private java.math.BigDecimal value;

        public String getVmRoundModelId() {
            return vmRoundModelId;
        }

        public void setVmRoundModelId(String vmRoundModelId) {
            this.vmRoundModelId = vmRoundModelId;
        }

        public Integer getRoundNumber() {
            return roundNumber;
        }

        public void setRoundNumber(Integer roundNumber) {
            this.roundNumber = roundNumber;
        }

        public String getVmId() {
            return vmId;
        }

        public void setVmId(String vmId) {
            this.vmId = vmId;
        }

        public java.math.BigDecimal getValue() {
            return value;
        }

        public void setValue(java.math.BigDecimal value) {
            this.value = value;
        }
    }
}
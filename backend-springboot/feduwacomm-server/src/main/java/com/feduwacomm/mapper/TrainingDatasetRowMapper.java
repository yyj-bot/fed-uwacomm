package com.feduwacomm.mapper;

import com.feduwacomm.entity.TrainingDataRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 训练数据明细表Mapper接口
 */
@Mapper
public interface TrainingDatasetRowMapper {

    /**
     * 批量插入数据行记录（JSON字符串格式）
     */
    int insertRows(@Param("datasetId") String datasetId, @Param("rowsJson") List<String> rowsJson);

    /**
     * 插入单条数据行记录
     */
    int insertDataRow(TrainingDataRow dataRow);

    /**
     * 批量插入数据行记录
     */
    int insertDataRows(@Param("rows") List<TrainingDataRow> dataRows);

    /**
     * 根据数据集ID统计数据行数
     */
    int countByDataset(@Param("datasetId") String datasetId);

    /**
     * 根据数据集ID删除数据行
     */
    int deleteByDataset(@Param("datasetId") String datasetId);

    /**
     * 根据数据集ID查询数据行
     */
    List<TrainingDataRow> selectByDatasetId(@Param("datasetId") String datasetId,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);

    /**
     * 根据行ID查询数据行
     */
    TrainingDataRow selectById(@Param("id") String id);
} 
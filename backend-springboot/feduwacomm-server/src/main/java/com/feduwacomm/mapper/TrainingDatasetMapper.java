package com.feduwacomm.mapper;

import com.feduwacomm.entity.TrainingData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TrainingDatasetMapper {

    int upsertDataset(@Param("id") String id,
                      @Param("vmId") String vmId,
                      @Param("name") String name,
                      @Param("description") String description,
                      @Param("dataType") String dataType,
                      @Param("status") String status,
                      @Param("metadata") String metadataJson);

    int insertTrainingData(TrainingData trainingData);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    int updateTrainingData(TrainingData trainingData);

    int updateProgress(@Param("id") String id, @Param("progress") Integer progress);

    int updateValidationResult(@Param("id") String id, 
                             @Param("isValid") Boolean isValid,
                             @Param("validationResult") String validationResultJson);

    int updateProcessResult(@Param("id") String id,
                          @Param("isProcessed") Boolean isProcessed, 
                          @Param("processResult") String processResultJson);

    Map<String, Object> selectById(@Param("id") String id);

    TrainingData selectByIdEntity(@Param("id") String id);

    List<TrainingData> selectByQuery(@Param("vmId") String vmId,
                                   @Param("dataType") String dataType,
                                   @Param("status") String status,
                                   @Param("keyword") String keyword,
                                   @Param("startDate") String startDate,
                                   @Param("endDate") String endDate,
                                   @Param("tags") List<String> tags,
                                   @Param("offset") Integer offset,
                                   @Param("limit") Integer limit);

    Long countByQuery(@Param("vmId") String vmId,
                     @Param("dataType") String dataType,
                     @Param("status") String status,
                     @Param("keyword") String keyword,
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate,
                     @Param("tags") List<String> tags);

    List<TrainingData> selectByIds(@Param("ids") List<String> ids);

    int deleteById(@Param("id") String id);

    int deleteByIds(@Param("ids") List<String> ids);

    Map<String, Object> selectStatistics(@Param("vmId") String vmId,
                                        @Param("dataType") String dataType,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate);

    Long getTotalCount();

    Long getTotalSize();

    Map<String, Integer> getDataTypeDistribution();

    Map<String, Integer> getStatusDistribution();

    List<Map<String, Object>> getVmDistribution();

    List<Integer> getUploadTrendLast7Days();

    List<Integer> getUploadTrendLast30Days();

    List<Map<String, Object>> getTopDataTypes(@Param("limit") Integer limit);
} 
package com.feduwacomm.mapper;

import com.feduwacomm.entity.TrainingData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface TrainingDatasetMapper {

    int upsertDataset(@Param("id") String id,
                      @Param("name") String name,
                      @Param("description") String description,
                      @Param("dataType") String dataType,
                      @Param("status") String status,
                      @Param("metadata") String metadataJson);

    int insertTrainingData(TrainingData trainingData);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    int updateTrainingData(TrainingData trainingData);


    Map<String, Object> selectById(@Param("id") String id);

    TrainingData selectByIdEntity(@Param("id") String id);

    // 实体转Map的便捷方法
    default Map<String, Object> selectByIdAsMap(@Param("id") String id) {
        TrainingData entity = selectByIdEntity(id);
        if (entity == null) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("id", entity.getId());
        result.put("name", entity.getName());
        result.put("description", entity.getDescription());
        result.put("dataType", entity.getDataType());
        result.put("status", entity.getStatus());
        result.put("metadata", entity.getMetadata());
        result.put("uploadTime", entity.getUploadTime());
        return result;
    }

    List<TrainingData> selectByQuery(@Param("uploadedBy") String uploadedBy,
                                   @Param("dataType") String dataType,
                                   @Param("status") String status,
                                   @Param("keyword") String keyword,
                                   @Param("startDate") String startDate,
                                   @Param("endDate") String endDate,
                                   @Param("tags") List<String> tags,
                                   @Param("offset") Integer offset,
                                   @Param("limit") Integer limit);

    Long countByQuery(@Param("uploadedBy") String uploadedBy,
                     @Param("dataType") String dataType,
                     @Param("status") String status,
                     @Param("keyword") String keyword,
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate,
                     @Param("tags") List<String> tags);

    List<TrainingData> selectByIds(@Param("ids") List<String> ids);

    int deleteById(@Param("id") String id);

    int deleteByIds(@Param("ids") List<String> ids);

    Map<String, Object> selectStatistics(@Param("uploadedBy") String uploadedBy,
                                        @Param("dataType") String dataType,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate);

    Long getTotalCount();

    Long getTotalSize();

    Map<String, Integer> getDataTypeDistribution();

    Map<String, Integer> getStatusDistribution();


    List<Integer> getUploadTrendLast7Days();

    List<Integer> getUploadTrendLast30Days();

    List<Map<String, Object>> getTopDataTypes(@Param("limit") Integer limit);
} 
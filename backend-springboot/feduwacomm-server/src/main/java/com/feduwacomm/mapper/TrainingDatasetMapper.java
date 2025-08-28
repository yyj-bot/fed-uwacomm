package com.feduwacomm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    int updateStatus(@Param("id") String id, @Param("status") String status);

    Map<String, Object> selectById(@Param("id") String id);

    int deleteById(@Param("id") String id);
} 
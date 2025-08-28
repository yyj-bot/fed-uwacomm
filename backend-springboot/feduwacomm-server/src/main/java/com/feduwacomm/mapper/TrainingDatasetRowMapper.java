package com.feduwacomm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrainingDatasetRowMapper {

    int insertRows(@Param("datasetId") String datasetId, @Param("rowsJson") List<String> rowsJson);

    int countByDataset(@Param("datasetId") String datasetId);

    int deleteByDataset(@Param("datasetId") String datasetId);
} 
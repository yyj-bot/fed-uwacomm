package com.feduwacomm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface FederatedTasksMapper {

    int upsertTask(@Param("id") String id,
                   @Param("name") String name,
                   @Param("algorithm") String algorithm,
                   @Param("status") String status,
                   @Param("totalRounds") Integer totalRounds,
                   @Param("currentRound") Integer currentRound,
                   @Param("config") String configJson);

    int updateProgress(@Param("id") String id,
                       @Param("currentRound") Integer currentRound,
                       @Param("status") String status);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    Map<String, Object> selectById(@Param("id") String id);
} 
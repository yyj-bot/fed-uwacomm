package com.feduwacomm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VmRoundModelsMapper {

    int upsertRoundModel(@Param("id") String id,
                          @Param("taskId") String taskId,
                          @Param("vmId") String vmId,
                          @Param("roundNumber") Integer roundNumber,
                          @Param("accuracy") Double accuracy,
                          @Param("loss") Double loss,
                          @Param("parameters") String parametersJson);
} 
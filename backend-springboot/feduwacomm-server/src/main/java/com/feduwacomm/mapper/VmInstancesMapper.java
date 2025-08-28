package com.feduwacomm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VmInstancesMapper {

    int updateConnection(@Param("id") String id,
                         @Param("connectionStatus") String connectionStatus,
                         @Param("lastHeartbeat") String lastHeartbeat);
} 
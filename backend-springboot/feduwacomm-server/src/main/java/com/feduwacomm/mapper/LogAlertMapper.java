package com.feduwacomm.mapper;

import com.feduwacomm.entity.LogAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LogAlertMapper {

    int insert(LogAlert logAlert);

    LogAlert selectById(@Param("alertId") String alertId);

    int update(LogAlert logAlert);

    int deleteById(@Param("alertId") String alertId);

    List<LogAlert> selectByStatus(@Param("status") String status);

    List<LogAlert> selectByType(@Param("type") String type);

    int updateTrigger(@Param("alertId") String alertId,
                     @Param("message") String message,
                     @Param("triggerCount") Integer triggerCount,
                     @Param("lastTriggered") LocalDateTime lastTriggered);

    List<LogAlert> selectAll();
}
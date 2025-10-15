package com.feduwacomm.mapper;

import com.feduwacomm.entity.RoundStateRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface RoundStateMapper {

    RoundStateRecord selectByTaskIdAndRound(@Param("taskId") String taskId,
                                            @Param("roundNumber") Integer roundNumber);

    int insertRoundState(RoundStateRecord record);

    int updateState(@Param("taskId") String taskId,
                    @Param("roundNumber") Integer roundNumber,
                    @Param("state") String state,
                    @Param("startedAt") LocalDateTime startedAt,
                    @Param("completedAt") LocalDateTime completedAt,
                    @Param("errorMessage") String errorMessage);

    int updateCounters(@Param("taskId") String taskId,
                       @Param("roundNumber") Integer roundNumber,
                       @Param("participantCount") Integer participantCount,
                       @Param("completedParticipants") Integer completedParticipants,
                       @Param("gradientUploadsReceived") Integer gradientUploadsReceived,
                       @Param("modelBroadcastsAcked") Integer modelBroadcastsAcked);

    int updateDatasetBindings(@Param("taskId") String taskId,
                              @Param("roundNumber") Integer roundNumber,
                              @Param("datasetBindings") String datasetBindings);

    int deleteByTaskId(@Param("taskId") String taskId);
}

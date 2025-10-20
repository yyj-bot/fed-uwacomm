package com.feduwacomm.listener;

import com.feduwacomm.entity.ModelDistribution;
import com.feduwacomm.enums.InitialModelStatus;
import com.feduwacomm.event.InitialModelDistributionCompletedEvent;
import com.feduwacomm.mapper.InitialModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.service.LogService;
import com.feduwacomm.service.NotificationService;
import com.feduwacomm.service.RetryService;
import com.feduwacomm.service.WebSocketService;
import com.feduwacomm.service.WorkflowStageTransitionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class InitialModelEventListenerTest {

    private static final String MODEL_ID = "model-1";
    private static final String TASK_ID = "task-1";
    private static final String RETRY_TYPE = "initial_model_distribution";

    @InjectMocks
    private InitialModelEventListener listener;

    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private WorkflowStageTransitionService workflowStageTransitionService;
    @Mock
    private InitialModelMapper initialModelMapper;
    @Mock
    private ModelDistributionMapper modelDistributionMapper;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private RetryService retryService;

    @BeforeEach
    void init() {
        lenient().when(notificationService.sendModelDistributionNotification(anyString(), anyString(), anyString(), anyMap()))
            .thenReturn(true);
        lenient().when(notificationService.sendNotification(anyString(), anyString(), anyString(), anyString(), anyString(), anyMap()))
            .thenReturn(true);
        lenient().when(notificationService.sendWebSocketNotification(anyString(), anyString(), anyString(), anyString(), anyMap()))
            .thenReturn(true);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void handleCompletedEvent_shouldMarkDistributionsCompletedAndModelDistributed() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 2L);
        stats.put("completed", 2L);
        stats.put("failed", 0L);
        stats.put("pending", 0L);

        InitialModelDistributionCompletedEvent event = InitialModelDistributionCompletedEvent.success(
            this, MODEL_ID, TASK_ID, null, 2, 2, 0,
            List.of("vm-1", "vm-2"),
            List.of(),
            stats,
            120L
        );

        ModelDistribution dist1 = ModelDistribution.builder().id("dist-1").build();
        ModelDistribution dist2 = ModelDistribution.builder().id("dist-2").build();
        when(modelDistributionMapper.selectByModelIdAndVmId(MODEL_ID, "vm-1")).thenReturn(dist1);
        when(modelDistributionMapper.selectByModelIdAndVmId(MODEL_ID, "vm-2")).thenReturn(dist2);
        when(modelDistributionMapper.updateDistributionStatus(anyString(), anyString(), any(LocalDateTime.class), any()))
            .thenReturn(1);
        when(modelDistributionMapper.updateVerificationStatus(anyString(), anyBoolean(), any())).thenReturn(1);
        when(webSocketService.sendToTaskSubscribers(eq(TASK_ID), anyMap())).thenReturn(1);

        listener.handleInitialModelDistributionCompleted(event);

        verify(modelDistributionMapper, times(2))
            .updateDistributionStatus(anyString(), eq("COMPLETED"), any(LocalDateTime.class), isNull());
        verify(modelDistributionMapper, times(2))
            .updateVerificationStatus(anyString(), eq(Boolean.TRUE), any(LocalDateTime.class));
        verify(initialModelMapper).updateStatus(MODEL_ID, InitialModelStatus.DISTRIBUTED.getCode(), "SYSTEM");

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webSocketService).sendToTaskSubscribers(eq(TASK_ID), messageCaptor.capture());

        Map<String, Object> message = messageCaptor.getValue();
        assertThat(message.get("status")).isEqualTo("COMPLETED");
        assertThat(message.get("total")).isEqualTo(2);
        assertThat(message.get("success")).isEqualTo(2);
        assertThat(message.get("failed")).isEqualTo(0);

        verify(retryService).resetRetryCount(RETRY_TYPE, MODEL_ID);
    }

    @Test
    void handleCompletedEvent_partialSuccessShouldMarkModelReady() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 2L);
        stats.put("completed", 1L);
        stats.put("failed", 1L);
        stats.put("pending", 0L);

        InitialModelDistributionCompletedEvent event = InitialModelDistributionCompletedEvent.success(
            this, MODEL_ID, TASK_ID, null, 2, 1, 1,
            List.of("vm-1"),
            List.of("vm-2"),
            stats,
            220L
        );

        when(retryService.getRetryCount(RETRY_TYPE, MODEL_ID)).thenReturn(0);
        when(retryService.shouldRetryDistribution(eq(MODEL_ID), eq(0), anyString()))
            .thenReturn(new RetryService.RetryResult(true, 2000, 1, "network"));

        ModelDistribution successDist = ModelDistribution.builder().id("dist-success").build();
        ModelDistribution failedDist = ModelDistribution.builder().id("dist-failed").build();
        when(modelDistributionMapper.selectByModelIdAndVmId(MODEL_ID, "vm-1")).thenReturn(successDist);
        when(modelDistributionMapper.selectByModelIdAndVmId(MODEL_ID, "vm-2")).thenReturn(failedDist);
        when(modelDistributionMapper.updateDistributionStatus(anyString(), anyString(), any(LocalDateTime.class), any()))
            .thenReturn(1);
        when(modelDistributionMapper.updateVerificationStatus(anyString(), anyBoolean(), any())).thenReturn(1);
        when(initialModelMapper.updateStatus(anyString(), anyString(), anyString())).thenReturn(1);
        when(modelDistributionMapper.getDistributionProgress(MODEL_ID)).thenReturn(stats);
        when(webSocketService.sendToTaskSubscribers(eq(TASK_ID), anyMap())).thenReturn(1);

        listener.handleInitialModelDistributionCompleted(event);

        verify(modelDistributionMapper).updateDistributionStatus(eq("dist-success"), eq("COMPLETED"), any(LocalDateTime.class), isNull());
        verify(modelDistributionMapper).updateDistributionStatus(eq("dist-failed"), eq("FAILED"), any(LocalDateTime.class), isNull());
        verify(modelDistributionMapper).updateVerificationStatus(eq("dist-success"), eq(Boolean.TRUE), any(LocalDateTime.class));
        verify(modelDistributionMapper).updateVerificationStatus(eq("dist-failed"), eq(Boolean.FALSE), isNull());
        verify(initialModelMapper).updateStatus(MODEL_ID, InitialModelStatus.READY.getCode(), "SYSTEM");

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webSocketService).sendToTaskSubscribers(eq(TASK_ID), messageCaptor.capture());

        Map<String, Object> message = messageCaptor.getValue();
        assertThat(message.get("status")).isEqualTo("PARTIAL_SUCCESS");
        assertThat(message.get("failed")).isEqualTo(1);

        verify(retryService).scheduleRetry(eq(RETRY_TYPE), eq(MODEL_ID), anyLong(), eq(1), anyMap());
    }

    @Test
    void handleFailedEvent_shouldMarkAllDistributionsFailedAndResetModelStatus() {
        InitialModelDistributionCompletedEvent event = InitialModelDistributionCompletedEvent.failure(
            this, MODEL_ID, TASK_ID, null, 3, "network error", 300L
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 3L);
        stats.put("completed", 0L);
        stats.put("failed", 3L);
        stats.put("pending", 0L);

        Map<String, Object> distributionRecord = new HashMap<>();
        distributionRecord.put("id", "dist-1");

        when(modelDistributionMapper.selectByModelIdWithVmInfo(MODEL_ID))
            .thenReturn(List.of(distributionRecord));
        when(webSocketService.sendToTaskSubscribers(eq(TASK_ID), anyMap())).thenReturn(1);

        listener.handleInitialModelDistributionCompleted(event);

        verify(modelDistributionMapper).updateDistributionStatus(eq("dist-1"), eq("FAILED"), any(LocalDateTime.class), eq("network error"));
        verify(modelDistributionMapper).updateVerificationStatus(eq("dist-1"), eq(Boolean.FALSE), isNull());
        verify(initialModelMapper).updateStatus(MODEL_ID, InitialModelStatus.READY.getCode(), "SYSTEM");

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webSocketService).sendToTaskSubscribers(eq(TASK_ID), messageCaptor.capture());

        Map<String, Object> message = messageCaptor.getValue();
        assertThat(message.get("status")).isEqualTo("FAILED");
        assertThat(message.get("failed")).isEqualTo(3);

        verify(retryService).resetRetryCount(RETRY_TYPE, MODEL_ID);
    }
}

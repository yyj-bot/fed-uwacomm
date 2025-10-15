package com.feduwacomm.service;

import com.feduwacomm.dto.TaskDeleteDTO;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.model.vo.initial.InitialModelBindingVO;
import com.feduwacomm.service.impl.FederatedTaskServiceImpl;
import com.feduwacomm.service.cache.MetricsCacheService;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.TaskOperationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FederatedTaskServiceImplDeleteTaskTest {

    private static final String TASK_ID = "task-1";
    private static final String MODEL_ID = "model-1";
    private static final String OPERATOR_ID = "operator-1";

    @InjectMocks
    private FederatedTaskServiceImpl federatedTaskService;

    @Mock
    private FederatedTasksMapper tasksMapper;
    @Mock
    private InitialModelGenerationService initialModelGenerationService;
    @Mock
    private LogService logService;
    @Mock
    private com.feduwacomm.mapper.TaskParticipantsMapper taskParticipantsMapper;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private DataDistributionService dataDistributionService;
    @Mock
    private TrainingDataService trainingDataService;
    @Mock
    private WebSocketProtocolService webSocketProtocolService;
    @Mock
    private FederatedOrchestrationService orchestrationService;
    @Mock
    private com.feduwacomm.service.VmInstanceService vmInstanceService;
    @Mock
    private UuidUtil uuidUtil;
    @Mock
    private MetricsCacheService metricsCacheService;
    @Mock
    private com.feduwacomm.service.RoundStateManager roundStateManager;
    @Mock
    private com.feduwacomm.service.VmAckTracker vmAckTracker;
    @Mock
    private com.feduwacomm.service.RoundLockManager roundLockManager;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(tasksMapper.selectTaskById(TASK_ID)).thenReturn(new FederatedTask());
        when(tasksMapper.deleteTask(TASK_ID)).thenReturn(1);
    }

    @Test
    void deleteTask_shouldDeleteModelWhenFlagIsTrue() {
        TaskDeleteDTO request = TaskDeleteDTO.builder()
            .deleteData(false)
            .deleteModel(true)
            .build();

        when(initialModelGenerationService.getTaskBinding(TASK_ID))
            .thenReturn(InitialModelBindingVO.builder().modelId(MODEL_ID).build());
        when(initialModelGenerationService.deleteModel(MODEL_ID, OPERATOR_ID)).thenReturn(true);

        TaskOperationVO result = federatedTaskService.deleteTask(TASK_ID, request, OPERATOR_ID);

        assertThat(result.getTaskId()).isEqualTo(TASK_ID);
        assertThat(result.getModelPreserved()).isFalse();

        verify(initialModelGenerationService).deleteModel(MODEL_ID, OPERATOR_ID);
        verify(initialModelGenerationService, never()).unbindModelFromTask(anyString(), anyString(), anyString());
        verify(tasksMapper, never()).deleteParticipantsByTaskId(anyString());

        ArgumentCaptor<Object> detailsCaptor = ArgumentCaptor.forClass(Object.class);
        verify(logService).logTask(eq(TASK_ID), eq("INFO"), eq("任务删除成功"),
            eq("TASK_MANAGER"), isNull(), detailsCaptor.capture());

        assertThat(detailsCaptor.getValue())
            .isInstanceOf(Map.class)
            .satisfies(details -> {
                Map<?, ?> detailMap = (Map<?, ?>) details;
                assertThat(detailMap.get("boundModelId")).isEqualTo(MODEL_ID);
                assertThat(detailMap.get("modelDeleted")).isEqualTo(Boolean.TRUE);
                assertThat(detailMap.get("modelUnbound")).isEqualTo(Boolean.FALSE);
            });
    }

    @Test
    void deleteTask_shouldUnbindModelWhenFlagIsFalse() {
        TaskDeleteDTO request = TaskDeleteDTO.builder()
            .deleteData(false)
            .deleteModel(false)
            .build();

        when(initialModelGenerationService.getTaskBinding(TASK_ID))
            .thenReturn(InitialModelBindingVO.builder().modelId(MODEL_ID).build());

        TaskOperationVO result = federatedTaskService.deleteTask(TASK_ID, request, OPERATOR_ID);

        assertThat(result.getTaskId()).isEqualTo(TASK_ID);
        assertThat(result.getModelPreserved()).isTrue();

        verify(initialModelGenerationService).unbindModelFromTask(MODEL_ID, TASK_ID, OPERATOR_ID);
        verify(initialModelGenerationService, never()).deleteModel(anyString(), anyString());
        verify(tasksMapper, never()).deleteParticipantsByTaskId(anyString());

        ArgumentCaptor<Object> detailsCaptor = ArgumentCaptor.forClass(Object.class);
        verify(logService).logTask(eq(TASK_ID), eq("INFO"), eq("任务删除成功"),
            eq("TASK_MANAGER"), isNull(), detailsCaptor.capture());

        assertThat(detailsCaptor.getValue())
            .isInstanceOf(Map.class)
            .satisfies(details -> {
                Map<?, ?> detailMap = (Map<?, ?>) details;
                assertThat(detailMap.get("boundModelId")).isEqualTo(MODEL_ID);
                assertThat(detailMap.get("modelDeleted")).isEqualTo(Boolean.FALSE);
                assertThat(detailMap.get("modelUnbound")).isEqualTo(Boolean.TRUE);
            });
    }

    @Test
    void deleteTask_shouldSkipModelHandlingWhenNoBinding() {
        TaskDeleteDTO request = TaskDeleteDTO.builder()
            .deleteData(false)
            .deleteModel(true)
            .build();

        when(initialModelGenerationService.getTaskBinding(TASK_ID)).thenReturn(null);

        TaskOperationVO result = federatedTaskService.deleteTask(TASK_ID, request, OPERATOR_ID);

        assertThat(result.getTaskId()).isEqualTo(TASK_ID);
        assertThat(result.getModelPreserved()).isTrue();

        verify(initialModelGenerationService, never()).deleteModel(anyString(), anyString());
        verify(initialModelGenerationService, never()).unbindModelFromTask(anyString(), anyString(), anyString());

        ArgumentCaptor<Object> detailsCaptor = ArgumentCaptor.forClass(Object.class);
        verify(logService).logTask(eq(TASK_ID), eq("INFO"), eq("任务删除成功"),
            eq("TASK_MANAGER"), isNull(), detailsCaptor.capture());

        assertThat(detailsCaptor.getValue())
            .isInstanceOf(Map.class)
            .satisfies(details -> {
                Map<?, ?> detailMap = (Map<?, ?>) details;
                assertThat(detailMap.get("boundModelId")).isNull();
                assertThat(detailMap.get("modelDeleted")).isEqualTo(Boolean.FALSE);
                assertThat(detailMap.get("modelUnbound")).isEqualTo(Boolean.FALSE);
            });
    }
}

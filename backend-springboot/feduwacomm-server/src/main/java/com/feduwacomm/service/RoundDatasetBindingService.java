package com.feduwacomm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.RoundDatasetBinding;
import com.feduwacomm.entity.RoundStateRecord;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.mapper.RoundStateMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 轮次数据集绑定服务
 *
 * <p>负责采集并持久化 VM 在每一轮使用的 assignedDatasetId，
 * 为ROUND_START上下文与多轮梯度调度提供一致的数据来源。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoundDatasetBindingService {

    private static final TypeReference<Map<String, RoundDatasetBinding>> BINDING_TYPE =
            new TypeReference<>() {};

    private final TaskParticipantsMapper taskParticipantsMapper;
    private final RoundStateMapper roundStateMapper;
    private final ObjectMapper objectMapper;

    /**
     * 从 task_participants 表采集最新的 VM 数据集绑定关系。
     *
     * @param taskId 任务ID
     * @return vmId -> RoundDatasetBinding 映射（按查询顺序保序）
     */
    public Map<String, RoundDatasetBinding> captureCurrentBindings(String taskId) {
        List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
        if (CollectionUtils.isEmpty(participants)) {
            log.warn("采集数据集绑定失败: 未找到参与者, taskId={}", taskId);
            return Collections.emptyMap();
        }

        Map<String, RoundDatasetBinding> bindings = new LinkedHashMap<>();
        for (TaskParticipant participant : participants) {
            if (!StringUtils.hasText(participant.getVmId())) {
                continue;
            }
            if (!StringUtils.hasText(participant.getAssignedDatasetId())) {
                log.debug("参与者缺少assignedDatasetId: participantId={}, vmId={}, taskId={}",
                        participant.getId(), participant.getVmId(), taskId);
                continue;
            }

            bindings.put(participant.getVmId(), RoundDatasetBinding.builder()
                    .vmId(participant.getVmId())
                    .assignedDatasetId(participant.getAssignedDatasetId())
                    .datasetStatus(participant.getDatasetStatus())
                    .localPath(participant.getLocalPath())
                    .build());
        }

        if (bindings.isEmpty()) {
            log.warn("采集数据集绑定结果为空: taskId={}", taskId);
        } else {
            log.info("采集数据集绑定成功: taskId={}, vmCount={}", taskId, bindings.size());
        }
        return bindings;
    }

    /**
     * 将绑定快照持久化到 round_states.dataset_bindings。
     *
     * @param taskId        任务ID
     * @param roundNumber   轮次号
     * @param bindings      VM->绑定信息映射
     */
    public void persistBindings(String taskId, int roundNumber, Map<String, RoundDatasetBinding> bindings) {
        Map<String, RoundDatasetBinding> safeBindings =
                bindings != null ? bindings : Collections.emptyMap();
        String json = serializeBindings(safeBindings);
        if (json == null) {
            return;
        }
        int updated = roundStateMapper.updateDatasetBindings(taskId, roundNumber, json);
        if (updated == 0) {
            log.warn("写入数据集绑定快照失败: round_states 不存在? taskId={}, round={}", taskId, roundNumber);
        } else {
            log.info("写入数据集绑定快照成功: taskId={}, round={}, payloadSize={}",
                    taskId, roundNumber, json.length());
        }
    }

    /**
     * 读取指定轮次的绑定快照，如果不存在则返回空集合。
     *
     * @param taskId      任务ID
     * @param roundNumber 轮次号
     * @return VM->绑定信息映射
     */
    public Map<String, RoundDatasetBinding> loadBindings(String taskId, int roundNumber) {
        RoundStateRecord record = roundStateMapper.selectByTaskIdAndRound(taskId, roundNumber);
        if (record == null || !StringUtils.hasText(record.getDatasetBindings())) {
            return Collections.emptyMap();
        }
        Map<String, RoundDatasetBinding> bindings = deserialize(record.getDatasetBindings());
        return bindings != null ? bindings : Collections.emptyMap();
    }

    /**
     * 以列表形式返回绑定快照，方便前端展示。
     */
    public List<RoundDatasetBinding> loadBindingsAsList(String taskId, int roundNumber) {
        return loadBindings(taskId, roundNumber).values().stream().collect(Collectors.toList());
    }

    private String serializeBindings(Map<String, RoundDatasetBinding> bindings) {
        if (bindings == null) {
            bindings = Collections.emptyMap();
        }
        try {
            return objectMapper.writeValueAsString(bindings);
        } catch (JsonProcessingException e) {
            log.error("序列化数据集绑定失败: error={}, entryCount={}",
                    e.getMessage(), bindings != null ? bindings.size() : 0, e);
            return null;
        }
    }

    private Map<String, RoundDatasetBinding> deserialize(String json) {
        try {
            return objectMapper.readValue(json, BINDING_TYPE);
        } catch (Exception e) {
            log.error("反序列化数据集绑定失败: json={}, error={}", json, e.getMessage(), e);
            return null;
        }
    }
}

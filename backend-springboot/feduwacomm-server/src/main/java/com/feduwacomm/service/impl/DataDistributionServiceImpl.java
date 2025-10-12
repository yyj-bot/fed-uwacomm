package com.feduwacomm.service.impl;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.constants.SystemConstants;
import com.feduwacomm.dto.DataDistributionDTO;
import com.feduwacomm.dto.DatasetSlice;
import com.feduwacomm.dto.DatasetAllocationValidation;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.entity.DataDistribution;
import com.feduwacomm.entity.DataDistributionDetail;
import com.feduwacomm.entity.TrainingDataRow;
import com.feduwacomm.event.DataDistributionStartedEvent;
import com.feduwacomm.event.DataDistributionCompletedEvent;
import com.feduwacomm.mapper.DataDistributionMapper;
import com.feduwacomm.mapper.DataDistributionDetailMapper;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.mapper.TrainingDatasetMapper;
import com.feduwacomm.mapper.TrainingDatasetRowMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.common.exception.BusinessException;
import com.feduwacomm.service.DataDistributionService;
import com.feduwacomm.service.DataSlicingService;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.utils.MessageIdGenerator;
import com.feduwacomm.utils.MessageBuilder;
import com.feduwacomm.dto.DataSliceResult;
import com.feduwacomm.dto.SliceInfo;
import com.feduwacomm.dto.BatchRange;
import com.feduwacomm.enums.AllocationStrategy;
import com.feduwacomm.vo.DataDistributionTaskVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 数据分发服务实现
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataDistributionServiceImpl implements DataDistributionService {

    private final DataDistributionMapper dataDistributionMapper;
    private final DataDistributionDetailMapper dataDistributionDetailMapper;
    private final VmInstancesMapper vmInstancesMapper;
    private final TrainingDatasetMapper trainingDatasetMapper;
    private final TaskParticipantsMapper taskParticipantsMapper;
    private final com.feduwacomm.mapper.FederatedTasksMapper federatedTasksMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final UuidUtil uuidUtil;
    private final DataSlicingService dataSlicingService;
    private final TrainingDatasetRowMapper trainingDatasetRowMapper;  // v1.5.1: 数据行查询
    private final SimpMessagingTemplate messagingTemplate;  // v1.5.1: WebSocket消息发送
    private final MessageIdGenerator messageIdGenerator;  // v1.5.1: 消息ID生成
    private final MessageBuilder messageBuilder;  // v1.5.1: 消息构建器
    private final RatioDataSlicingServiceImpl ratioDataSlicingService;  // v1.5.1.1: 比例分配服务
    private final PlatformTransactionManager transactionManager;  // 用于异步线程中的编程式事务

    private static final String DATA_STORAGE_PATH = "./data/distributed";
    private static final int BATCH_SIZE = 50;  // v1.5.1: 数据传输批次大小（减小避免WebSocket断开）

    @Override
    @Transactional
    public DataDistributionTaskVO createDistributionTask(DataDistributionDTO distributionDTO, String createdBy) {
        log.info("创建数据分发任务: taskId={}, strategy={}, vmCount={}", 
                distributionDTO.getTaskId(), distributionDTO.getDistributionStrategy(), distributionDTO.getTargetVmIds().size());

        // 创建分发任务
        String distributionId = uuidUtil.generateUuid();
        DataDistribution distribution = DataDistribution.builder()
                .id(distributionId)
                .taskId(distributionDTO.getTaskId())
                .distributionName(generateDistributionName(distributionDTO))
                .strategy(distributionDTO.getDistributionStrategy())
                .status("CREATED")
                .config(convertToJson(distributionDTO.getDistributionConfig()))
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build();

        dataDistributionMapper.insertDataDistribution(distribution);

        // 根据分发策略创建分发详情
        List<DataDistributionDetail> details = createDistributionDetails(distributionDTO, distributionId);
        if (!details.isEmpty()) {
            dataDistributionDetailMapper.batchInsertDetails(details);
        }

        log.info("数据分发任务创建成功: distributionId={}, detailCount={}", distributionId, details.size());

        return convertToTaskVO(distribution, details);
    }

    @Override
    @Transactional
    public DataDistributionTaskVO startDistribution(String distributionId, String startedBy) {
        log.info("开始数据分发: distributionId={}, startedBy={}", distributionId, startedBy);

        DataDistribution distribution = dataDistributionMapper.selectById(distributionId);
        if (distribution == null) {
            throw new RuntimeException("分发任务不存在: " + distributionId);
        }

        if (!"CREATED".equals(distribution.getStatus()) && !"PAUSED".equals(distribution.getStatus())) {
            throw new RuntimeException("只能启动已创建或已暂停的分发任务");
        }

        // 更新状态为进行中
        dataDistributionMapper.updateStatus(distributionId, "IN_PROGRESS", LocalDateTime.now(), null);

        // 发布分发开始事件
        DataDistributionStartedEvent event = new DataDistributionStartedEvent(
                distributionId, distribution.getTaskId(), distribution.getStrategy(), startedBy);
        eventPublisher.publishEvent(event);

        // 🔧 v1.5.1.1修复：直接同步执行分发，因为executeDistribution内部已使用TransactionTemplate管理事务
        // 移除异步执行，避免测试环境中执行时序问题
        log.info("✅ 事务已提交，开始执行数据分发: distributionId={}", distributionId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("🔄 开始同步执行数据分发: distributionId={}", distributionId);
                executeDistribution(distributionId);
            }
        });

        distribution.setStatus("IN_PROGRESS");
        distribution.setStartedAt(LocalDateTime.now());

        List<DataDistributionDetail> details = getDistributionDetails(distributionId);
        return convertToTaskVO(distribution, details);
    }

    @Override
    @Transactional
    public DataDistributionTaskVO pauseDistribution(String distributionId, String pausedBy) {
        log.info("暂停数据分发: distributionId={}, pausedBy={}", distributionId, pausedBy);

        DataDistribution distribution = dataDistributionMapper.selectById(distributionId);
        if (distribution == null) {
            throw new RuntimeException("分发任务不存在: " + distributionId);
        }

        if (!"IN_PROGRESS".equals(distribution.getStatus())) {
            throw new RuntimeException("只能暂停正在进行的分发任务");
        }

        dataDistributionMapper.updateStatus(distributionId, "PAUSED", null, null);

        distribution.setStatus("PAUSED");
        List<DataDistributionDetail> details = getDistributionDetails(distributionId);
        return convertToTaskVO(distribution, details);
    }

    @Override
    @Transactional
    public DataDistributionTaskVO resumeDistribution(String distributionId, String resumedBy) {
        log.info("恢复数据分发: distributionId={}, resumedBy={}", distributionId, resumedBy);

        DataDistribution distribution = dataDistributionMapper.selectById(distributionId);
        if (distribution == null) {
            throw new RuntimeException("分发任务不存在: " + distributionId);
        }

        if (!"PAUSED".equals(distribution.getStatus())) {
            throw new RuntimeException("只能恢复已暂停的分发任务");
        }

        dataDistributionMapper.updateStatus(distributionId, "IN_PROGRESS", LocalDateTime.now(), null);

        // 🔧 修复：在事务提交后再异步执行分发
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("✅ 事务已提交，恢复异步执行数据分发: distributionId={}", distributionId);
                CompletableFuture.runAsync(() -> executeDistribution(distributionId));
            }
        });

        distribution.setStatus("IN_PROGRESS");
        distribution.setStartedAt(LocalDateTime.now());

        List<DataDistributionDetail> details = getDistributionDetails(distributionId);
        return convertToTaskVO(distribution, details);
    }

    @Override
    @Transactional
    public DataDistributionTaskVO stopDistribution(String distributionId, String stoppedBy) {
        log.info("停止数据分发: distributionId={}, stoppedBy={}", distributionId, stoppedBy);

        DataDistribution distribution = dataDistributionMapper.selectById(distributionId);
        if (distribution == null) {
            throw new RuntimeException("分发任务不存在: " + distributionId);
        }

        if ("COMPLETED".equals(distribution.getStatus()) || "FAILED".equals(distribution.getStatus())) {
            throw new RuntimeException("已完成或已失败的任务无法停止");
        }

        dataDistributionMapper.updateStatus(distributionId, "CANCELLED", null, LocalDateTime.now());

        distribution.setStatus("CANCELLED");
        distribution.setCompletedAt(LocalDateTime.now());
        
        List<DataDistributionDetail> details = getDistributionDetails(distributionId);
        return convertToTaskVO(distribution, details);
    }

    @Override
    @Transactional
    public DataDistributionTaskVO cancelDistribution(String distributionId, String cancelledBy) {
        return stopDistribution(distributionId, cancelledBy);
    }

    @Override
    public DataDistributionTaskVO getDistributionTask(String distributionId) {
        log.debug("获取分发任务详情: distributionId={}", distributionId);

        DataDistribution distribution = dataDistributionMapper.selectById(distributionId);
        if (distribution == null) {
            throw new RuntimeException("分发任务不存在: " + distributionId);
        }

        List<DataDistributionDetail> details = getDistributionDetails(distributionId);
        return convertToTaskVO(distribution, details);
    }

    @Override
    public List<DataDistributionTaskVO> getTaskDistributions(String taskId) {
        log.debug("获取任务的所有分发记录: taskId={}", taskId);

        List<DataDistribution> distributions = dataDistributionMapper.selectByTaskId(taskId);
        return distributions.stream()
                .map(dist -> {
                    List<DataDistributionDetail> details = getDistributionDetails(dist.getId());
                    return convertToTaskVO(dist, details);
                })
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<DataDistributionTaskVO> getDistributionTasksPaged(String taskId, String status, 
                                                                       String strategy, Integer page, Integer size) {
        log.debug("获取分页分发任务列表: taskId={}, status={}, strategy={}, page={}, size={}", 
                taskId, status, strategy, page, size);

        int offset = (page - 1) * size;
        List<DataDistribution> distributions = dataDistributionMapper.selectPagedList(
                taskId, status, strategy, offset, size);
        
        List<DataDistributionTaskVO> taskVOs = distributions.stream()
                .map(dist -> {
                    List<DataDistributionDetail> details = getDistributionDetails(dist.getId());
                    return convertToTaskVO(dist, details);
                })
                .collect(Collectors.toList());

        long total = dataDistributionMapper.countDataDistributions(taskId, status, strategy);

        return PageResult.of(taskVOs, total, (long) page, (long) size);
    }

    @Override
    @Transactional
    public DataDistributionTaskVO redistributeFailedData(String distributionId, String redistributedBy) {
        log.info("重新分发失败数据: distributionId={}, redistributedBy={}", distributionId, redistributedBy);

        DataDistribution distribution = dataDistributionMapper.selectById(distributionId);
        if (distribution == null) {
            throw new RuntimeException("分发任务不存在: " + distributionId);
        }

        // 重置失败的详情状态
        List<DataDistributionDetail> failedDetails = dataDistributionDetailMapper
                .selectByDistributionIdAndStatus(distributionId, "FAILED");

        for (DataDistributionDetail detail : failedDetails) {
            dataDistributionDetailMapper.updateStatus(detail.getId(), "PENDING", null, null);
        }

        // 如果任务状态是失败，重置为创建状态
        if ("FAILED".equals(distribution.getStatus())) {
            dataDistributionMapper.updateStatus(distributionId, "CREATED", null, null);
            distribution.setStatus("CREATED");
        }

        log.info("重新分发设置完成: distributionId={}, failedCount={}", distributionId, failedDetails.size());

        List<DataDistributionDetail> details = getDistributionDetails(distributionId);
        return convertToTaskVO(distribution, details);
    }

    @Override
    @Transactional
    public boolean deleteDistributionTask(String distributionId, String deletedBy) {
        log.info("删除分发任务: distributionId={}, deletedBy={}", distributionId, deletedBy);

        DataDistribution distribution = dataDistributionMapper.selectById(distributionId);
        if (distribution == null) {
            return false;
        }

        if ("IN_PROGRESS".equals(distribution.getStatus())) {
            throw new RuntimeException("无法删除正在进行的分发任务");
        }

        // 删除详情记录
        dataDistributionDetailMapper.deleteByDistributionId(distributionId);

        // 删除分发任务
        return dataDistributionMapper.deleteById(distributionId) > 0;
    }

    @Override
    public boolean verifyDistributionIntegrity(String distributionId) {
        log.info("验证分发数据完整性: distributionId={}", distributionId);

        List<DataDistributionDetail> completedDetails = dataDistributionDetailMapper
                .selectByDistributionIdAndStatus(distributionId, "COMPLETED");

        boolean allValid = true;
        for (DataDistributionDetail detail : completedDetails) {
            // 这里可以添加实际的文件完整性验证逻辑
            // 目前简化处理
            if (!StringUtils.hasText(detail.getChecksum())) {
                allValid = false;
                break;
            }
        }

        log.info("完整性验证结果: distributionId={}, valid={}", distributionId, allValid);
        return allValid;
    }

    @Override
    public Double getDistributionProgress(String distributionId) {
        Map<String, Object> progress = dataDistributionDetailMapper.getDistributionProgress(distributionId);
        
        Integer total = (Integer) progress.getOrDefault("total", 0);
        Integer completed = (Integer) progress.getOrDefault("completed", 0);

        if (total == 0) {
            return 0.0;
        }

        return (double) completed / total * 100;
    }

    @Override
    public List<DataDistributionTaskVO.VmDataInfo> getVmDataDetails(String distributionId, String vmId) {
        log.debug("获取虚拟机数据分发详情: distributionId={}, vmId={}", distributionId, vmId);

        List<DataDistributionDetail> details = dataDistributionDetailMapper
                .selectByDistributionIdAndVmId(distributionId, vmId);

        return details.stream()
                .map(this::convertToVmDataInfo)
                .collect(Collectors.toList());
    }

    @Override
    public DataDistributionTaskVO.DataBalanceAnalysis getDataBalanceAnalysis(String distributionId) {
        log.debug("获取数据分布分析: distributionId={}", distributionId);

        List<Map<String, Object>> balanceStats = dataDistributionDetailMapper
                .getDataBalanceStatistics(distributionId);

        if (balanceStats.isEmpty()) {
            return DataDistributionTaskVO.DataBalanceAnalysis.builder()
                    .isBalanced(true)
                    .balanceScore(100.0)
                    .maxVariancePercentage(0.0)
                    .standardDeviation(0.0)
                    .giniCoefficient(0.0)
                    .build();
        }

        // 计算数据分布统计
        List<Long> dataSizes = balanceStats.stream()
                .map(stat -> ((Number) stat.get("total_data_size")).longValue())
                .collect(Collectors.toList());

        double mean = dataSizes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = dataSizes.stream()
                .mapToDouble(size -> Math.pow(size - mean, 2))
                .average().orElse(0.0);
        double standardDeviation = Math.sqrt(variance);

        long maxSize = dataSizes.stream().mapToLong(Long::longValue).max().orElse(0L);
        long minSize = dataSizes.stream().mapToLong(Long::longValue).min().orElse(0L);
        double maxVariancePercentage = mean == 0 ? 0 : Math.abs(maxSize - minSize) / mean * 100;

        double balanceScore = Math.max(0, 100 - maxVariancePercentage);
        boolean isBalanced = maxVariancePercentage <= 20.0; // 20%以内认为平衡

        return DataDistributionTaskVO.DataBalanceAnalysis.builder()
                .isBalanced(isBalanced)
                .balanceScore(balanceScore)
                .maxVariancePercentage(maxVariancePercentage)
                .standardDeviation(standardDeviation)
                .giniCoefficient(calculateGiniCoefficient(dataSizes))
                .build();
    }

    @Override
    public DataDistributionTaskVO.QualityAssessment getQualityAssessment(String distributionId) {
        log.debug("获取分发质量评估: distributionId={}", distributionId);

        Map<String, Object> progress = dataDistributionDetailMapper.getDistributionProgress(distributionId);
        
        Integer total = (Integer) progress.getOrDefault("total", 0);
        Integer completed = (Integer) progress.getOrDefault("completed", 0);
        Integer failed = (Integer) progress.getOrDefault("failed", 0);

        // 计算各项评分
        double integrityScore = total == 0 ? 100.0 : (double) completed / total * 100;
        double consistencyScore = verifyDistributionIntegrity(distributionId) ? 100.0 : 80.0;
        double efficiencyScore = calculateEfficiencyScore(distributionId);
        double availabilityScore = failed == 0 ? 100.0 : Math.max(0, 100 - (double) failed / total * 100);

        double overallScore = (integrityScore + consistencyScore + efficiencyScore + availabilityScore) / 4;

        return DataDistributionTaskVO.QualityAssessment.builder()
                .overallScore(overallScore)
                .integrityScore(integrityScore)
                .consistencyScore(consistencyScore)
                .efficiencyScore(efficiencyScore)
                .availabilityScore(availabilityScore)
                .assessmentTime(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public int cleanupExpiredDistributions(int daysOld) {
        log.info("清理过期分发任务: daysOld={}", daysOld);

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysOld);
        List<DataDistribution> expiredDistributions = dataDistributionMapper
                .selectByTimeRange(LocalDateTime.MIN, cutoffTime)
                .stream()
                .filter(dist -> "COMPLETED".equals(dist.getStatus()) || "FAILED".equals(dist.getStatus()))
                .collect(Collectors.toList());

        int cleanedCount = 0;
        for (DataDistribution distribution : expiredDistributions) {
            if (deleteDistributionTask(distribution.getId(), "SYSTEM")) {
                cleanedCount++;
            }
        }

        log.info("清理完成，删除了{}个过期分发任务", cleanedCount);
        return cleanedCount;
    }

    @Override
    public DataDistributionTaskVO.DistributionStatistics getDistributionStatistics(String taskId) {
        log.debug("获取分发统计信息: taskId={}", taskId);

        List<DataDistribution> distributions = StringUtils.hasText(taskId) ?
                dataDistributionMapper.selectByTaskId(taskId) :
                dataDistributionMapper.selectAll();

        if (distributions.isEmpty()) {
            return DataDistributionTaskVO.DistributionStatistics.builder().build();
        }

        // 计算统计信息
        List<DataDistribution> completedDistributions = distributions.stream()
                .filter(dist -> "COMPLETED".equals(dist.getStatus()))
                .collect(Collectors.toList());

        double avgExecutionTime = completedDistributions.stream()
                .filter(dist -> dist.getStartedAt() != null && dist.getCompletedAt() != null)
                .mapToLong(dist -> ChronoUnit.MILLIS.between(dist.getStartedAt(), dist.getCompletedAt()))
                .average().orElse(0.0);

        double successRate = distributions.isEmpty() ? 0.0 : 
                (double) completedDistributions.size() / distributions.size() * 100;

        return DataDistributionTaskVO.DistributionStatistics.builder()
                .totalExecutionTime((long) avgExecutionTime)
                .avgDistributionTime(avgExecutionTime)
                .successRate(successRate)
                .throughputPerHour(calculateThroughputPerHour(distributions))
                .build();
    }

    @Override
    @Transactional
    public DataDistributionTaskVO rebalanceDataDistribution(String distributionId, String rebalancedBy) {
        log.info("重新平衡数据分发: distributionId={}, rebalancedBy={}", distributionId, rebalancedBy);

        DataDistribution distribution = dataDistributionMapper.selectById(distributionId);
        if (distribution == null) {
            throw new RuntimeException("分发任务不存在: " + distributionId);
        }

        if (!"COMPLETED".equals(distribution.getStatus())) {
            throw new RuntimeException("只能重新平衡已完成的分发任务");
        }

        // 重新分析数据分布并调整
        // 这里可以实现具体的重新平衡逻辑
        // 目前简化处理，重置状态让用户重新分发

        dataDistributionDetailMapper.resetDistributionStatus(distributionId);
        dataDistributionMapper.updateStatus(distributionId, "CREATED", null, null);

        distribution.setStatus("CREATED");
        List<DataDistributionDetail> details = getDistributionDetails(distributionId);
        
        return convertToTaskVO(distribution, details);
    }

    @Override
    public String exportDistributionReport(String distributionId, String format) {
        log.info("导出分发报告: distributionId={}, format={}", distributionId, format);

        DataDistributionTaskVO taskVO = getDistributionTask(distributionId);
        
        try {
            // 创建报告目录
            Path reportDir = Paths.get("./data/reports");
            Files.createDirectories(reportDir);

            String fileName = String.format("distribution_report_%s_%s.%s", 
                    distributionId, System.currentTimeMillis(), format.toLowerCase());
            Path reportPath = reportDir.resolve(fileName);

            // 根据格式生成报告
            switch (format.toUpperCase()) {
                case "JSON":
                    Files.write(reportPath, objectMapper.writeValueAsBytes(taskVO));
                    break;
                case "CSV":
                    generateCsvReport(taskVO, reportPath);
                    break;
                default:
                    throw new RuntimeException("不支持的报告格式: " + format);
            }

            log.info("报告导出成功: {}", reportPath);
            return reportPath.toString();

        } catch (Exception e) {
            log.error("报告导出失败: distributionId={}, error={}", distributionId, e.getMessage());
            throw new RuntimeException("报告导出失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<DataDistributionTaskVO> batchCreateDistributions(List<DataDistributionDTO> distributionDTOs, String createdBy) {
        log.info("批量创建分发任务: count={}, createdBy={}", distributionDTOs.size(), createdBy);

        List<DataDistributionTaskVO> results = new ArrayList<>();
        for (DataDistributionDTO dto : distributionDTOs) {
            try {
                DataDistributionTaskVO taskVO = createDistributionTask(dto, createdBy);
                results.add(taskVO);
            } catch (Exception e) {
                log.error("批量创建分发任务失败: taskId={}, error={}", dto.getTaskId(), e.getMessage());
                // 继续处理其他任务
            }
        }

        return results;
    }

    @Override
    public DataDistributionTaskVO previewDistributionPlan(DataDistributionDTO distributionDTO) {
        log.debug("预览分发计划: taskId={}, strategy={}", distributionDTO.getTaskId(), distributionDTO.getDistributionStrategy());

        // 创建预览用的分发详情（不保存到数据库）
        List<DataDistributionDetail> previewDetails = createDistributionDetails(distributionDTO, "PREVIEW");

        // 创建临时分发对象
        DataDistribution previewDistribution = DataDistribution.builder()
                .id("PREVIEW")
                .taskId(distributionDTO.getTaskId())
                .distributionName("预览-" + generateDistributionName(distributionDTO))
                .strategy(distributionDTO.getDistributionStrategy())
                .status("PREVIEW")
                .config(convertToJson(distributionDTO.getDistributionConfig()))
                .createdAt(LocalDateTime.now())
                .build();

        return convertToTaskVO(previewDistribution, previewDetails);
    }

    @Override
    public DataDistributionDTO getRecommendedDistributionStrategy(String taskId, List<String> datasetIds, List<String> vmIds) {
        log.debug("获取推荐分发策略: taskId={}, datasetCount={}, vmCount={}", taskId, datasetIds.size(), vmIds.size());

        // 基于数据集大小和虚拟机资源推荐策略
        // 这里简化实现，实际可以根据历史数据和机器学习算法优化

        String recommendedStrategy;
        if (vmIds.size() <= 3) {
            recommendedStrategy = "BALANCED";
        } else if (datasetIds.size() > vmIds.size() * 2) {
            recommendedStrategy = "ROUND_ROBIN";
        } else {
            recommendedStrategy = "RANDOM";
        }

        return DataDistributionDTO.builder()
                .taskId(taskId)
                .datasetIds(datasetIds)
                .distributionStrategy(recommendedStrategy)
                .targetVmIds(vmIds)
                .shardCount(Math.min(vmIds.size() * 2, datasetIds.size()))
                .enableShuffle(true)
                .enableCompression(true)
                .verifyIntegrity(true)
                .timeoutSeconds(600)
                .maxRetries(3)
                .build();
    }

    @Override
    public DataDistributionTaskVO.DistributionStatistics getDistributionPerformanceMetrics(String distributionId) {
        log.debug("获取分发性能指标: distributionId={}", distributionId);

        Map<String, Object> progress = dataDistributionDetailMapper.getDistributionProgress(distributionId);
        
        Long totalDataSize = (Long) progress.getOrDefault("totalDataSize", 0L);
        Long totalTransferredSize = (Long) progress.getOrDefault("totalTransferredSize", 0L);

        DataDistribution distribution = dataDistributionMapper.selectById(distributionId);
        double transferRate = calculateTransferRate(distribution, totalTransferredSize);

        return DataDistributionTaskVO.DistributionStatistics.builder()
                .totalDataTransferred(totalTransferredSize)
                .avgTransferRate(transferRate)
                .throughputPerHour(transferRate * 3600) // 转换为每小时
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 执行数据分发
     */
    /**
     * 执行数据分发（v1.5.1真实WebSocket通信）
     *
     * 核心流程：
     * 1. 获取任务的所有参与者VM
     * 2. 执行数据切片（使用DataSlicingService）
     * 3. 为每个VM执行：
     *    a. 生成assignedDatasetId
     *    b. 发送DATASET_CREATE消息（包含SliceInfo）
     *    c. 分批发送DATASET_APPEND_ROWS消息（包含BatchRange和rows）
     *    d. 发送DATASET_COMPLETE消息
     * 4. 更新数据库状态
     */
    private void executeDistribution(String distributionId) {
        try {
            log.info("🔥🔥🔥 开始执行真实数据分发 (v1.5.1): distributionId={}", distributionId);
            System.out.println("🔥🔥🔥 executeDistribution 被调用: distributionId=" + distributionId);

            DataDistribution distribution = dataDistributionMapper.selectById(distributionId);
            if (distribution == null) {
                throw new BusinessException("分发任务不存在: " + distributionId);
            }

            String taskId = distribution.getTaskId();
            List<DataDistributionDetail> pendingDetails = dataDistributionDetailMapper
                    .selectByDistributionIdAndStatus(distributionId, "PENDING");

            if (pendingDetails.isEmpty()) {
                log.warn("⚠️ 没有待分发的任务: distributionId={}", distributionId);
                dataDistributionMapper.updateStatus(distributionId, "COMPLETED", null, LocalDateTime.now());
                return;
            }

            String originalDatasetId = pendingDetails.get(0).getDatasetId();

            // 步骤1：从TaskParticipant读取每个VM的比例配置
            // 查询所有参与者（包含dataRatio）
            List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
            if (participants.isEmpty()) {
                throw new BusinessException("任务没有参与者: taskId=" + taskId);
            }

            // 构建VM ID列表和比例列表（按照participants顺序，保证映射关系）
            List<String> vmIds = new ArrayList<>();
            List<Integer> ratios = new ArrayList<>();
            Map<String, Integer> vmRatioMap = new HashMap<>();

            // 检查是否所有dataRatio都已配置
            boolean allRatiosConfigured = participants.stream()
                    .allMatch(p -> p.getDataRatio() != null && p.getDataRatio() > 0);

            if (allRatiosConfigured) {
                // 方案A：所有dataRatio已配置，验证总和=1000
                int ratioSum = 0;
                for (TaskParticipant participant : participants) {
                    Integer dataRatio = participant.getDataRatio();
                    vmIds.add(participant.getVmId());
                    ratios.add(dataRatio);
                    vmRatioMap.put(participant.getVmId(), dataRatio);
                    ratioSum += dataRatio;
                }

                // 验证总和必须=1000
                if (ratioSum != 1000) {
                    throw new BusinessException(String.format(
                            "数据分配权重总和必须等于1000，当前总和=%d，请调整各VM的dataRatio配置", ratioSum));
                }

                log.info("📊 使用配置的千分比权重: taskId={}, vmRatioMap={}, sum={}",
                        taskId, vmRatioMap, ratioSum);
            } else {
                // 方案B：存在null或0的dataRatio，自动分配平均权重（总和=1000）
                int vmCount = participants.size();
                int baseRatio = 1000 / vmCount;  // 基础权重
                int remainder = 1000 % vmCount;   // 余数

                for (int i = 0; i < participants.size(); i++) {
                    TaskParticipant participant = participants.get(i);
                    // 前remainder个VM多分配1个权重单位
                    int autoRatio = baseRatio + (i < remainder ? 1 : 0);
                    vmIds.add(participant.getVmId());
                    ratios.add(autoRatio);
                    vmRatioMap.put(participant.getVmId(), autoRatio);
                }

                log.warn("⚠️ 存在未配置的dataRatio，已自动分配平均权重(总和=1000): taskId={}, vmRatioMap={}",
                        taskId, vmRatioMap);
            }

            log.info("📊 分发配置: taskId={}, vmCount={}, datasetId={}, vmRatioMap={}",
                    taskId, vmIds.size(), originalDatasetId, vmRatioMap);

            // 步骤2：读取分配策略
            com.feduwacomm.entity.FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            String strategyStr = task != null ? task.getDistributionStrategy() : null;
            AllocationStrategy strategy = parseStrategy(strategyStr);

            // 判断是否所有比例都是1（均等分配）
            boolean allRatiosEqual = ratios.stream().allMatch(r -> r == 1);

            log.info("🔪 开始数据切片: datasetId={}, vmCount={}, strategy={}, ratios={}, allEqual={}",
                    originalDatasetId, vmIds.size(), strategy, ratios, allRatiosEqual);

            List<DataSliceResult> sliceResults;
            if (strategy == AllocationStrategy.RATIO || !allRatiosEqual) {
                // 使用比例分配（如果配置了RATIO策略，或者比例不全相等）
                sliceResults = ratioDataSlicingService.sliceDatasetByRatio(
                        originalDatasetId, vmIds, ratios,
                        strategy != null ? strategy : AllocationStrategy.RATIO);
            } else {
                // 使用默认策略（IID平均分配）
                sliceResults = dataSlicingService.sliceDataset(
                        originalDatasetId, vmIds,
                        strategy != null ? strategy : AllocationStrategy.IID);
            }

            // 验证切片完整性
            int totalSamples = dataSlicingService.getDatasetSize(originalDatasetId);
            dataSlicingService.validateSlicesCoverage(sliceResults, totalSamples);
            log.info("✅ 数据切片完成并验证通过: totalSamples={}, sliceCount={}", totalSamples, sliceResults.size());

            // 步骤3：为每个VM执行分发
            boolean allSuccess = true;
            for (DataSliceResult sliceResult : sliceResults) {
                String vmId = sliceResult.getVmId();
                SliceInfo sliceInfo = sliceResult.getSliceInfo();

                // 查找对应的分发详情记录
                DataDistributionDetail detail = pendingDetails.stream()
                        .filter(d -> d.getVmId().equals(vmId))
                        .findFirst()
                        .orElse(null);

                if (detail == null) {
                    log.error("❌ 未找到VM的分发详情: vmId={}", vmId);
                    allSuccess = false;
                    continue;
                }

                try {
                    // 更新状态为处理中
                    dataDistributionDetailMapper.updateStatus(detail.getId(), "IN_PROGRESS", LocalDateTime.now(), null);

                    // 执行WebSocket分发
                    executeVmDataDistribution(taskId, vmId, originalDatasetId, sliceInfo, detail);

                    // 更新状态为完成
                    dataDistributionDetailMapper.updateStatus(detail.getId(), "COMPLETED", LocalDateTime.now(), null);
                    log.info("✅ VM数据分发完成: vmId={}, sliceSamples={}", vmId, sliceInfo.getSliceSamples());

                } catch (Exception e) {
                    log.error("❌ VM数据分发失败: vmId={}, error={}", vmId, e.getMessage(), e);
                    dataDistributionDetailMapper.updateStatus(detail.getId(), "FAILED", LocalDateTime.now(), e.getMessage());
                    allSuccess = false;
                }
            }

            // 步骤4：更新整体状态
            String finalStatus = allSuccess ? "COMPLETED" : "FAILED";
            // 🔧 保留原来的started_at，不要覆盖为null
            LocalDateTime startedAt = distribution.getStartedAt();
            dataDistributionMapper.updateStatus(distributionId, finalStatus, startedAt, LocalDateTime.now());

            // 发布完成事件
            DataDistributionCompletedEvent event = new DataDistributionCompletedEvent(
                    distributionId, taskId, "COMPLETED".equals(finalStatus), "system");
            eventPublisher.publishEvent(event);

            log.info("🎉 数据分发执行完成: distributionId={}, status={}", distributionId, finalStatus);

        } catch (Exception e) {
            log.error("💥 数据分发执行异常: distributionId={}, error={}", distributionId, e.getMessage(), e);
            dataDistributionMapper.updateStatus(distributionId, "FAILED", null, LocalDateTime.now());
        }
    }

    /**
     * 为单个VM执行数据分发（v1.5.1 WebSocket协议）
     *
     * @param taskId 任务ID
     * @param vmId 虚拟机ID
     * @param originalDatasetId 原始数据集ID
     * @param sliceInfo 切片信息
     * @param detail 分发详情记录
     */
    private void executeVmDataDistribution(String taskId, String vmId, String originalDatasetId,
                                          SliceInfo sliceInfo, DataDistributionDetail detail) {
        try {
            System.out.println("🔥🔥🔥 executeVmDataDistribution被调用: vmId=" + vmId + ", taskId=" + taskId);
            log.info("🔥🔥🔥 开始为VM分发数据: vmId={}, taskId={}, slice=[{}-{}], samples={}",
                    vmId, taskId, sliceInfo.getStartIndex(), sliceInfo.getEndIndex(), sliceInfo.getSliceSamples());

            // 生成assignedDatasetId
            String assignedDatasetId = uuidUtil.generateUuid();

            // 🔧 在新事务中更新TaskParticipant（异步线程需要显式事务）
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            // 🔥 强制创建新事务，不加入外层事务
            transactionTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            Integer updateResult = transactionTemplate.execute(status -> {
                try {
                    // 更新TaskParticipant表，记录assignedDatasetId
                    TaskParticipant participant = taskParticipantsMapper.selectParticipant(taskId, vmId);
                    System.out.println("🔍🔍🔍 查询participant: taskId=" + taskId + ", vmId=" + vmId +
                            ", found=" + (participant != null) + ", id=" + (participant != null ? participant.getId() : "null"));
                    log.info("🔍 查询到的participant: taskId={}, vmId={}, participant={}, id={}",
                            taskId, vmId, participant != null ? "存在" : "null", participant != null ? participant.getId() : "null");

                    if (participant != null) {
                        System.out.println("🔧🔧🔧 更新前: id=" + participant.getId() + ", oldAssignedDatasetId=" + participant.getAssignedDatasetId());
                        log.info("🔧 更新前状态: participant.id={}, oldAssignedDatasetId={}",
                                participant.getId(), participant.getAssignedDatasetId());

                        participant.setAssignedDatasetId(assignedDatasetId);
                        participant.setDatasetStatus("UPLOADING");
                        participant.setDatasetCreatedAt(LocalDateTime.now());
                        participant.setLocalPath("/data/assigned/" + assignedDatasetId);
                        participant.setUpdatedAt(LocalDateTime.now());

                        int updated = taskParticipantsMapper.updateParticipant(participant);
                        System.out.println("✅✅✅ 更新结果: vmId=" + vmId + ", assignedDatasetId=" + assignedDatasetId + ", updateCount=" + updated);
                        log.info("✅ TaskParticipant更新执行: vmId={}, assignedDatasetId={}, updateCount={}",
                                vmId, assignedDatasetId, updated);

                        if (updated == 0) {
                            System.out.println("❌❌❌ 更新失败: updateCount=0, participant.id=" + participant.getId());
                            log.error("❌ TaskParticipant更新失败：updateCount=0，可能WHERE条件不匹配，participant.id={}", participant.getId());
                        }
                        return updated;
                    } else {
                        System.out.println("❌❌❌ 未找到participant: taskId=" + taskId + ", vmId=" + vmId);
                        log.error("❌ 未找到TaskParticipant记录: taskId={}, vmId={}", taskId, vmId);
                        return 0;
                    }
                } catch (Exception e) {
                    log.error("❌ 事务中更新participant失败: vmId={}, error={}", vmId, e.getMessage(), e);
                    status.setRollbackOnly();
                    return 0;
                }
            });

            log.info("🔄 事务提交完成: vmId={}, updateResult={}", vmId, updateResult);

            // 步骤1：发送DATASET_CREATE消息（包含SliceInfo）
            sendDatasetCreateV151(vmId, taskId, assignedDatasetId, originalDatasetId, sliceInfo);
            log.info("✅ DATASET_CREATE消息已发送: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);

            // 等待VM创建数据集并准备接收数据 - 增加等待时间避免消息发送过快
            log.info("⏳ 等待VM创建数据集: vmId={}, 等待时间=2000ms", vmId);
            Thread.sleep(2000);

            // 步骤2：分批发送DATASET_APPEND_ROWS消息
            int startIndex = sliceInfo.getStartIndex();
            int endIndex = sliceInfo.getEndIndex();
            int totalRows = endIndex - startIndex + 1;

            int offset = startIndex;
            int batchCount = 0;
            long totalTransferredBytes = 0;

            while (offset <= endIndex) {
                int remainingRows = endIndex - offset + 1;
                int currentBatchSize = Math.min(BATCH_SIZE, remainingRows);

                // 从数据库查询数据行
                List<TrainingDataRow> rows = trainingDatasetRowMapper.selectByDatasetId(
                        originalDatasetId, offset, currentBatchSize);

                if (rows.isEmpty()) {
                    log.warn("⚠️ 数据行查询为空: datasetId={}, offset={}, limit={}",
                            originalDatasetId, offset, currentBatchSize);
                    break;
                }

                // 构建BatchRange
                int localStartIndex = offset - startIndex;
                int localEndIndex = localStartIndex + rows.size() - 1;
                BatchRange batchRange = BatchRange.builder()
                        .localStartIndex(localStartIndex)
                        .localEndIndex(localEndIndex)
                        .globalStartIndex(offset)
                        .globalEndIndex(offset + rows.size() - 1)
                        .build();

                // 发送DATASET_APPEND_ROWS消息
                sendDatasetAppendRowsV151(vmId, assignedDatasetId, batchRange, rows);

                batchCount++;
                offset += rows.size();
                totalTransferredBytes += estimateDataSize(rows);

                log.debug("📦 批次{}已发送: vmId={}, batchSize={}, progress={}/{}",
                        batchCount, vmId, rows.size(), offset - startIndex, totalRows);

                // 避免发送过快，稍微等待
                Thread.sleep(100);
            }

            log.info("✅ 数据行传输完成: vmId={}, totalBatches={}, totalRows={}", vmId, batchCount, offset - startIndex);

            // 步骤3：发送DATASET_COMPLETE消息
            sendDatasetCompleteV151(taskId, vmId, assignedDatasetId, offset - startIndex);
            log.info("✅ DATASET_COMPLETE消息已发送: vmId={}, totalRows={}", vmId, offset - startIndex);

            // 更新传输大小
            dataDistributionDetailMapper.updateTransferredSize(detail.getId(), totalTransferredBytes);

        } catch (Exception e) {
            log.error("❌ VM数据分发异常: vmId={}, error={}", vmId, e.getMessage(), e);
            throw new RuntimeException("VM数据分发失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送DATASET_CREATE消息（v1.5.1协议，包含SliceInfo）
     */
    private void sendDatasetCreateV151(String vmId, String taskId, String assignedDatasetId,
                                      String originalDatasetId, SliceInfo sliceInfo) {
        System.out.println("🔥🔥🔥 发送DATASET_CREATE消息: vmId=" + vmId + ", assignedDatasetId=" + assignedDatasetId);
        log.info("🔥🔥🔥 发送DATASET_CREATE消息: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("taskId", taskId);
        messageData.put("assignedDatasetId", assignedDatasetId);
        messageData.put("originalDatasetId", originalDatasetId);
        messageData.put("datasetType", "TRAINING");

        // 🆕 v1.5.1: 添加SliceInfo
        Map<String, Object> sliceInfoMap = new HashMap<>();
        sliceInfoMap.put("startIndex", sliceInfo.getStartIndex());
        sliceInfoMap.put("endIndex", sliceInfo.getEndIndex());
        sliceInfoMap.put("sliceSamples", sliceInfo.getSliceSamples());
        sliceInfoMap.put("totalSamples", sliceInfo.getTotalSamples());
        sliceInfoMap.put("sliceIndex", sliceInfo.getSliceIndex());
        sliceInfoMap.put("totalSlices", sliceInfo.getTotalSlices());
        sliceInfoMap.put("allocationStrategy", sliceInfo.getAllocationStrategy());
        messageData.put("sliceInfo", sliceInfoMap);

        System.out.println("🔥🔥🔥 SliceInfo内容: startIndex=" + sliceInfo.getStartIndex() + ", endIndex=" + sliceInfo.getEndIndex() + ", sliceSamples=" + sliceInfo.getSliceSamples());

        ProtocolMessage message = messageBuilder.buildServerMessage(
                ProtocolType.DATASET_CREATE, vmId, messageData);

        System.out.println("🔥🔥🔥 ProtocolMessage构建完成: type=" + message.getType() + ", vmId=" + message.getVmId());

        messagingTemplate.convertAndSend("/topic/vm/" + vmId, message);

        System.out.println("🔥🔥🔥 DATASET_CREATE消息已发送到WebSocket: /topic/vm/" + vmId);
        log.info("🔥🔥🔥 DATASET_CREATE消息已发送到WebSocket: destination=/topic/vm/{}", vmId);
    }

    /**
     * 发送DATASET_APPEND_ROWS消息（v1.5.1协议，包含BatchRange和rows）
     */
    private void sendDatasetAppendRowsV151(String vmId, String assignedDatasetId,
                                          BatchRange batchRange, List<TrainingDataRow> dataRows) {
        System.out.println("🔥🔥🔥 sendDatasetAppendRowsV151被调用: vmId=" + vmId + ", assignedDatasetId=" + assignedDatasetId + ", rowCount=" + dataRows.size());
        log.info("🔥🔥🔥 发送DATASET_APPEND_ROWS消息: vmId={}, assignedDatasetId={}, rowCount={}, batchRange=[{}-{}]",
                vmId, assignedDatasetId, dataRows.size(), batchRange.getGlobalStartIndex(), batchRange.getGlobalEndIndex());

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("assignedDatasetId", assignedDatasetId);
        messageData.put("rowCount", dataRows.size());

        // 🆕 v1.5.1: 添加BatchRange
        Map<String, Object> batchRangeMap = new HashMap<>();
        batchRangeMap.put("localStartIndex", batchRange.getLocalStartIndex());
        batchRangeMap.put("localEndIndex", batchRange.getLocalEndIndex());
        batchRangeMap.put("globalStartIndex", batchRange.getGlobalStartIndex());
        batchRangeMap.put("globalEndIndex", batchRange.getGlobalEndIndex());
        batchRangeMap.put("batchSamples", batchRange.getBatchSize());
        messageData.put("batchRange", batchRangeMap);

        // 转换数据行为Map列表（包含globalIndex）
        int currentGlobalIndex = batchRange.getGlobalStartIndex();
        List<Map<String, Object>> rows = dataRows.stream().map(row -> {
            Map<String, Object> rowMap = new HashMap<>();
            // rowData已经是Map类型，直接使用
            if (row.getRowData() != null) {
                rowMap.putAll(row.getRowData());
            }
            return rowMap;
        }).collect(Collectors.toList());

        // 🆕 v1.5.1: 为每行添加globalIndex
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).put("globalIndex", currentGlobalIndex + i);
        }

        messageData.put("rows", rows);

        ProtocolMessage message = messageBuilder.buildServerMessage(
                ProtocolType.DATASET_APPEND_ROWS, vmId, messageData);

        String destination = "/topic/vm/" + vmId;
        messagingTemplate.convertAndSend(destination, message);

        System.out.println("🔥🔥🔥 DATASET_APPEND_ROWS消息已发送: destination=" + destination + ", messageId=" + message.getId());
        log.info("🔥🔥🔥 DATASET_APPEND_ROWS消息已发送到WebSocket: destination={}, messageId={}", destination, message.getId());
    }

    /**
     * 发送DATASET_COMPLETE消息（v1.5.1协议）
     */
    private void sendDatasetCompleteV151(String taskId, String vmId, String assignedDatasetId, int totalRows) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("taskId", taskId);
        messageData.put("assignedDatasetId", assignedDatasetId);
        messageData.put("totalRows", totalRows);
        messageData.put("status", "COMPLETE");

        ProtocolMessage message = messageBuilder.buildServerMessage(
                ProtocolType.DATASET_COMPLETE, vmId, messageData);

        messagingTemplate.convertAndSend("/topic/vm/" + vmId, message);
    }

    /**
     * 估算数据大小（字节）
     */
    private long estimateDataSize(List<TrainingDataRow> rows) {
        return rows.stream()
                .mapToLong(row -> {
                    if (row.getRowData() == null) {
                        return 0;
                    }
                    try {
                        // 将Map序列化为JSON字符串来估算大小
                        String jsonString = objectMapper.writeValueAsString(row.getRowData());
                        return jsonString.length();
                    } catch (Exception e) {
                        // 如果序列化失败，使用粗略估算
                        return row.getRowData().size() * 50L; // 假设每个字段平均50字节
                    }
                })
                .sum();
    }

    /**
     * 创建分发详情
     */
    private List<DataDistributionDetail> createDistributionDetails(DataDistributionDTO distributionDTO, String distributionId) {
        List<DataDistributionDetail> details = new ArrayList<>();

        // 根据不同策略创建分发详情
        switch (distributionDTO.getDistributionStrategy()) {
            case "BALANCED":
                details = createBalancedDistribution(distributionDTO, distributionId);
                break;
            case "RANDOM":
                details = createRandomDistribution(distributionDTO, distributionId);
                break;
            case "ROUND_ROBIN":
                details = createRoundRobinDistribution(distributionDTO, distributionId);
                break;
            case "CUSTOM":
                details = createCustomDistribution(distributionDTO, distributionId);
                break;
            default:
                throw new RuntimeException("不支持的分发策略: " + distributionDTO.getDistributionStrategy());
        }

        return details;
    }

    /**
     * 创建均衡分发
     *
     * v1.5.1修改：为每个VM创建一个分发详情记录（使用相同的原始datasetId，后续会切片）
     */
    private List<DataDistributionDetail> createBalancedDistribution(DataDistributionDTO distributionDTO, String distributionId) {
        List<DataDistributionDetail> details = new ArrayList<>();

        int vmCount = distributionDTO.getTargetVmIds().size();
        int datasetCount = distributionDTO.getDatasetIds().size();

        // 获取原始数据集ID（v1.5.1中应该只有一个）
        String originalDatasetId = (datasetCount > 0) ? distributionDTO.getDatasetIds().get(0) : null;
        if (originalDatasetId == null) {
            log.error("❌ 无法创建分发详情：datasetIds为空");
            return details;
        }

        // 🔧 v1.5.1修复：为每个VM创建一个分发详情记录
        for (String vmId : distributionDTO.getTargetVmIds()) {
            DataDistributionDetail detail = createDistributionDetail(distributionId, originalDatasetId, vmId);
            details.add(detail);
        }

        log.info("✅ 创建了{}个分发详情记录：datasetId={}, vmCount={}", details.size(), originalDatasetId, vmCount);

        return details;
    }

    /**
     * 创建随机分发
     */
    private List<DataDistributionDetail> createRandomDistribution(DataDistributionDTO distributionDTO, String distributionId) {
        List<DataDistributionDetail> details = new ArrayList<>();
        Random random = ThreadLocalRandom.current();
        
        for (String datasetId : distributionDTO.getDatasetIds()) {
            String vmId = distributionDTO.getTargetVmIds().get(random.nextInt(distributionDTO.getTargetVmIds().size()));
            
            DataDistributionDetail detail = createDistributionDetail(distributionId, datasetId, vmId);
            details.add(detail);
        }
        
        return details;
    }

    /**
     * 创建轮询分发
     */
    private List<DataDistributionDetail> createRoundRobinDistribution(DataDistributionDTO distributionDTO, String distributionId) {
        List<DataDistributionDetail> details = new ArrayList<>();
        
        int vmIndex = 0;
        for (String datasetId : distributionDTO.getDatasetIds()) {
            String vmId = distributionDTO.getTargetVmIds().get(vmIndex % distributionDTO.getTargetVmIds().size());
            
            DataDistributionDetail detail = createDistributionDetail(distributionId, datasetId, vmId);
            details.add(detail);
            
            vmIndex++;
        }
        
        return details;
    }

    /**
     * 创建自定义分发
     */
    private List<DataDistributionDetail> createCustomDistribution(DataDistributionDTO distributionDTO, String distributionId) {
        // 自定义分发逻辑需要从distributionConfig中获取具体配置
        // 这里简化为均衡分发
        return createBalancedDistribution(distributionDTO, distributionId);
    }

    /**
     * 创建分发详情记录
     */
    private DataDistributionDetail createDistributionDetail(String distributionId, String datasetId, String vmId) {
        // 模拟数据大小（实际应从数据集获取）
        long dataSize = ThreadLocalRandom.current().nextLong(1024 * 1024, 100 * 1024 * 1024); // 1MB-100MB

        return DataDistributionDetail.builder()
                .id(uuidUtil.generateUuid())
                .distributionId(distributionId)
                .datasetId(datasetId)
                .vmId(vmId)
                .status("PENDING")
                .dataSize(dataSize)
                .transferredSize(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 获取分发详情列表
     */
    private List<DataDistributionDetail> getDistributionDetails(String distributionId) {
        return dataDistributionDetailMapper.selectByDistributionIdWithInfo(distributionId)
                .stream()
                .map(this::mapToDistributionDetail)
                .collect(Collectors.toList());
    }

    /**
     * 转换为任务VO
     */
    private DataDistributionTaskVO convertToTaskVO(DataDistribution distribution, List<DataDistributionDetail> details) {
        Map<String, Object> progressStats = dataDistributionDetailMapper.getDistributionProgress(distribution.getId());

        // 防止progressStats为null的情况
        if (progressStats == null) {
            log.warn("数据分发进度统计为null，distribution_id: {}", distribution.getId());
            progressStats = new HashMap<>();
        }

        Integer total = (Integer) progressStats.getOrDefault("total", 0);
        Integer completed = (Integer) progressStats.getOrDefault("completed", 0);
        Integer failed = (Integer) progressStats.getOrDefault("failed", 0);
        Long totalDataSize = (Long) progressStats.getOrDefault("totalDataSize", 0L);

        double progress = total == 0 ? 0.0 : (double) completed / total * 100;

        List<DataDistributionTaskVO.VmDataInfo> vmDataDetails = details.stream()
                .map(this::convertToVmDataInfo)
                .collect(Collectors.toList());

        return DataDistributionTaskVO.builder()
                .distributionId(distribution.getId())
                .taskId(distribution.getTaskId())
                .distributionName(distribution.getDistributionName())
                .distributionStrategy(distribution.getStrategy())
                .status(distribution.getStatus())
                .progress(progress)
                .createdAt(distribution.getCreatedAt())
                .startedAt(distribution.getStartedAt())
                .completedAt(distribution.getCompletedAt())
                .createdBy(distribution.getCreatedBy())
                .totalDataSize(totalDataSize)
                .totalRecords((long) total)
                .targetVmCount(getUniqueVmCount(details))
                .successVmCount(completed)
                .failedVmCount(failed)
                .configParams(parseJsonToMap(distribution.getConfig()))
                .vmDataDetails(vmDataDetails)
                .build();
    }

    /**
     * 转换为VM数据信息
     */
    private DataDistributionTaskVO.VmDataInfo convertToVmDataInfo(DataDistributionDetail detail) {
        return DataDistributionTaskVO.VmDataInfo.builder()
                .vmId(detail.getVmId())
                .status(detail.getStatus())
                .dataSize(detail.getDataSize())
                .shardIndex(0) // 可以根据实际需要设置
                .checksum(detail.getChecksum())
                .distributionStartTime(detail.getCreatedAt())
                .distributionCompletedTime(detail.getDistributedAt())
                .errorMessage(detail.getErrorMessage())
                .retryCount(0) // 可以根据实际需要设置
                .build();
    }

    /**
     * 其他辅助方法
     */
    private String generateDistributionName(DataDistributionDTO distributionDTO) {
        return String.format("%s-%s-%s", 
                distributionDTO.getDistributionStrategy(),
                distributionDTO.getDatasetIds().size(),
                System.currentTimeMillis() % 10000);
    }

    private int getUniqueVmCount(List<DataDistributionDetail> details) {
        return (int) details.stream()
                .map(DataDistributionDetail::getVmId)
                .distinct()
                .count();
    }

    private double calculateGiniCoefficient(List<Long> values) {
        if (values.isEmpty()) return 0.0;
        
        Collections.sort(values);
        int n = values.size();
        double sum = values.stream().mapToLong(Long::longValue).sum();
        
        if (sum == 0) return 0.0;
        
        double gini = 0.0;
        for (int i = 0; i < n; i++) {
            gini += (2 * (i + 1) - n - 1) * values.get(i);
        }
        
        return gini / (n * sum);
    }

    private double calculateEfficiencyScore(String distributionId) {
        // 基于分发时间和数据量计算效率评分
        // 简化实现
        return 85.0;
    }

    private double calculateThroughputPerHour(List<DataDistribution> distributions) {
        // 计算每小时吞吐量
        // 简化实现
        return distributions.size() * 1.0;
    }

    private double calculateTransferRate(DataDistribution distribution, Long transferredSize) {
        if (distribution.getStartedAt() == null || transferredSize == null || transferredSize == 0) {
            return 0.0;
        }
        
        long elapsedSeconds = ChronoUnit.SECONDS.between(distribution.getStartedAt(), LocalDateTime.now());
        if (elapsedSeconds == 0) return 0.0;
        
        return (double) transferredSize / 1024 / 1024 / elapsedSeconds; // MB/s
    }

    private void generateCsvReport(DataDistributionTaskVO taskVO, Path reportPath) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("VM ID,Status,Data Size,Record Count,Error Message\n");
        
        for (DataDistributionTaskVO.VmDataInfo vmData : taskVO.getVmDataDetails()) {
            csv.append(String.format("%s,%s,%s,%s,%s\n",
                    vmData.getVmId(),
                    vmData.getStatus(),
                    vmData.getDataSize(),
                    vmData.getRecordCount(),
                    vmData.getErrorMessage() != null ? vmData.getErrorMessage().replace(",", ";") : ""));
        }
        
        Files.write(reportPath, csv.toString().getBytes(SystemConstants.DEFAULT_CHARSET));
    }

    private String convertToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("转换JSON失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (!StringUtils.hasText(json)) return new HashMap<>();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析JSON失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private DataDistributionDetail mapToDistributionDetail(Map<String, Object> map) {
        // 防止map为null的情况
        if (map == null) {
            log.warn("数据分发详情映射的map为null，返回空的DataDistributionDetail");
            return DataDistributionDetail.builder().build();
        }

        return DataDistributionDetail.builder()
                .id((String) map.get("id"))
                .distributionId((String) map.get("distribution_id"))
                .datasetId((String) map.get("dataset_id"))
                .vmId((String) map.get("vm_id"))
                .status((String) map.get("status"))
                .dataSize(map.get("data_size") != null ? ((Number) map.get("data_size")).longValue() : null)
                .transferredSize(map.get("transferred_size") != null ? ((Number) map.get("transferred_size")).longValue() : null)
                .checksum((String) map.get("checksum"))
                .errorMessage((String) map.get("error_message"))
                .build();
    }

    // ========== v1.5数据集分片和assignedDatasetId管理功能实现 ==========

    @Override
    public DatasetSlice generateDatasetSlice(String originalDatasetPath, String vmId) {
        log.info("生成数据集分片 - 原始路径: {}, VM ID: {}", originalDatasetPath, vmId);

        try {
            // 生成唯一的分配数据集ID
            String assignedDatasetId = uuidUtil.generateUuid();

            // 获取数据集元信息
            int totalSamples = analyzeDatasetSize(originalDatasetPath);
            if (totalSamples <= 0) {
                log.error("无法分析数据集大小: {}", originalDatasetPath);
                throw new BusinessException("无效的数据集路径");
            }

            // 为该VM生成本地路径
            String localPath = generateLocalPath(vmId, assignedDatasetId);

            // 构建数据集分片
            return DatasetSlice.builder()
                    .assignedDatasetId(assignedDatasetId)
                    .vmId(vmId)
                    .localPath(localPath)
                    .sampleCount(totalSamples)
                    .startIndex(0)
                    .endIndex(totalSamples)
                    .createdAt(LocalDateTime.now())
                    .dataType("federated_learning")
                    .transferred(false)
                    .transferStatus("PENDING")
                    .metadata(String.format("{\"originalPath\":\"%s\",\"vmId\":\"%s\"}", originalDatasetPath, vmId))
                    .build();

        } catch (Exception e) {
            log.error("生成数据集分片失败: {}", e.getMessage(), e);
            throw new BusinessException("数据集分片生成失败: " + e.getMessage());
        }
    }

    @Override
    public DatasetSlice performIIDAllocation(String originalDatasetPath, int vmIndex, int totalVms) {
        log.info("执行IID数据分配 - 路径: {}, VM索引: {}, 总VM数: {}", originalDatasetPath, vmIndex, totalVms);

        if (vmIndex < 0 || vmIndex >= totalVms) {
            throw new BusinessException("VM索引超出范围: " + vmIndex);
        }

        if (totalVms <= 0) {
            throw new BusinessException("总VM数必须大于0");
        }

        try {
            // 分析数据集总大小
            int totalSamples = analyzeDatasetSize(originalDatasetPath);
            if (totalSamples <= 0) {
                throw new BusinessException("无效的数据集或数据集为空");
            }

            // IID分配：平均分配数据
            int samplesPerVm = totalSamples / totalVms;
            int remainder = totalSamples % totalVms;

            // 计算当前VM的数据范围
            int startIndex = vmIndex * samplesPerVm;
            int endIndex = startIndex + samplesPerVm;

            // 将余数分配给前面的VM
            if (vmIndex < remainder) {
                startIndex += vmIndex;
                endIndex += vmIndex + 1;
            } else {
                startIndex += remainder;
                endIndex += remainder;
            }

            int sampleCount = endIndex - startIndex;
            String assignedDatasetId = uuidUtil.generateUuid();
            String vmId = "vm_" + vmIndex; // 临时VM ID，实际使用时应传入真实VM ID
            String localPath = generateLocalPath(vmId, assignedDatasetId);

            log.info("IID分配结果 - VM{}: 样本{}到{}, 共{}个样本", vmIndex, startIndex, endIndex-1, sampleCount);

            return DatasetSlice.builder()
                    .assignedDatasetId(assignedDatasetId)
                    .vmId(vmId)
                    .localPath(localPath)
                    .sampleCount(sampleCount)
                    .startIndex(startIndex)
                    .endIndex(endIndex)
                    .createdAt(LocalDateTime.now())
                    .dataType("federated_learning")
                    .transferred(false)
                    .transferStatus("ALLOCATED")
                    .metadata(String.format("{\"originalPath\":\"%s\",\"vmIndex\":%d,\"totalVms\":%d,\"strategy\":\"IID\"}",
                            originalDatasetPath, vmIndex, totalVms))
                    .build();

        } catch (Exception e) {
            log.error("IID数据分配失败: {}", e.getMessage(), e);
            throw new BusinessException("IID数据分配失败: " + e.getMessage());
        }
    }

    @Override
    public DatasetAllocationValidation validateAllocation(String taskId) {
        log.info("验证任务数据集分配 - 任务ID: {}", taskId);

        try {
            // 获取任务的所有参与者
            List<TaskParticipant> participants = taskParticipantsMapper.selectParticipantsByTaskId(taskId);
            if (participants == null || participants.isEmpty()) {
                return new DatasetAllocationValidation(new HashMap<>(), 0,
                        LocalDateTime.now(), false, "任务无参与者");
            }

            // 构建分配信息映射
            Map<String, DatasetAllocationValidation.DatasetSliceInfo> allocationInfo = new HashMap<>();
            int totalCalculatedSamples = 0;

            for (TaskParticipant participant : participants) {
                if (participant.getAssignedDatasetId() == null) {
                    return new DatasetAllocationValidation(new HashMap<>(), 0,
                            LocalDateTime.now(), false,
                            "参与者 " + participant.getVmId() + " 未分配数据集");
                }

                // 解析数据集信息
                DatasetAllocationValidation.DatasetSliceInfo sliceInfo =
                        new DatasetAllocationValidation.DatasetSliceInfo();
                sliceInfo.setAssignedDatasetId(participant.getAssignedDatasetId());
                sliceInfo.setLocalPath(participant.getLocalPath());
                sliceInfo.setStatus(participant.getDatasetStatus());

                // 从参与者数据中获取样本数（这里简化处理，实际应从数据集元数据获取）
                int sampleCount = participant.getDataSize() != null ? participant.getDataSize() : 0;
                sliceInfo.setSampleCount(sampleCount);

                allocationInfo.put(participant.getVmId(), sliceInfo);
                totalCalculatedSamples += sampleCount;
            }

            // 验证分配完整性
            DatasetAllocationValidation validation = new DatasetAllocationValidation(
                    allocationInfo, totalCalculatedSamples);

            log.info("任务{}的数据集分配验证结果: {}", taskId, validation.isValid() ? "通过" : "失败");
            if (!validation.isValid()) {
                log.warn("验证失败原因: {}", validation.getErrorMessage());
            }

            return validation;

        } catch (Exception e) {
            log.error("验证数据集分配失败: {}", e.getMessage(), e);
            return new DatasetAllocationValidation(new HashMap<>(), 0,
                    LocalDateTime.now(), false, "验证过程发生异常: " + e.getMessage());
        }
    }

    // ========== v1.5辅助方法 ==========

    /**
     * 分析数据集大小
     * TODO: 实现真实的数据集分析逻辑
     */
    private int analyzeDatasetSize(String datasetPath) {
        // 临时实现：返回模拟的样本数
        // 实际实现应该解析数据集文件，计算真实的样本数量
        log.debug("分析数据集大小: {}", datasetPath);

        if (datasetPath == null || datasetPath.trim().isEmpty()) {
            return 0;
        }

        // 模拟不同数据集的样本数
        if (datasetPath.contains("small")) {
            return 1000;
        } else if (datasetPath.contains("medium")) {
            return 5000;
        } else if (datasetPath.contains("large")) {
            return 10000;
        } else {
            return 3000; // 默认样本数
        }
    }

    /**
     * 生成VM本地路径
     */
    private String generateLocalPath(String vmId, String assignedDatasetId) {
        return String.format("/data/federated/%s/dataset_%s", vmId, assignedDatasetId);
    }

    // ========== v1.5.1数据集切片增强功能实现 ==========

    @Override
    public List<DataSliceResult> distributeDatasetWithSlice(String taskId, String datasetId,
                                                             List<String> vmIds, AllocationStrategy strategy) {
        log.info("使用切片信息分发数据集 (v1.5.1): taskId={}, datasetId={}, vmCount={}, strategy={}",
                taskId, datasetId, vmIds.size(), strategy);

        // 参数验证
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("taskId不能为空");
        }
        if (datasetId == null || datasetId.isEmpty()) {
            throw new IllegalArgumentException("datasetId不能为空");
        }
        if (vmIds == null || vmIds.isEmpty()) {
            throw new IllegalArgumentException("vmIds列表不能为空");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("strategy不能为空");
        }

        // 调用DataSlicingService进行数据切分
        List<DataSliceResult> sliceResults = dataSlicingService.sliceDataset(datasetId, vmIds, strategy);

        log.info("数据切分完成，生成{}个切片", sliceResults.size());

        if (sliceResults.isEmpty()) {
            throw new BusinessException("数据切分失败，未生成任何切片");
        }

        return sliceResults;
    }

    @Override
    public BatchRange calculateBatchRange(SliceInfo sliceInfo, int batchIndex, int batchSize) {
        log.debug("计算批次范围 (v1.5.1): sliceIndex={}, batchIndex={}, batchSize={}",
                sliceInfo.getSliceIndex(), batchIndex, batchSize);

        // 参数验证
        if (sliceInfo == null) {
            throw new IllegalArgumentException("sliceInfo不能为空");
        }
        if (!sliceInfo.isValid()) {
            throw new IllegalArgumentException("sliceInfo无效: " + sliceInfo);
        }
        if (batchIndex < 0) {
            throw new IllegalArgumentException("batchIndex不能为负数: " + batchIndex);
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize必须大于0: " + batchSize);
        }

        // 计算本地索引范围
        int localStartIndex = batchIndex * batchSize;
        int localEndIndex = Math.min(localStartIndex + batchSize - 1, sliceInfo.getSliceSamples() - 1);

        // 检查是否超出切片范围
        if (localStartIndex >= sliceInfo.getSliceSamples()) {
            throw new IllegalArgumentException(
                    String.format("批次索引超出范围: batchIndex=%d, batchSize=%d, sliceSamples=%d",
                            batchIndex, batchSize, sliceInfo.getSliceSamples())
            );
        }

        // 计算全局索引范围
        int globalStartIndex = sliceInfo.getStartIndex() + localStartIndex;
        int globalEndIndex = sliceInfo.getStartIndex() + localEndIndex;

        BatchRange batchRange = BatchRange.builder()
                .localStartIndex(localStartIndex)
                .localEndIndex(localEndIndex)
                .globalStartIndex(globalStartIndex)
                .globalEndIndex(globalEndIndex)
                .build();

        // 验证BatchRange有效性
        if (!batchRange.isValid()) {
            throw new BusinessException("生成的BatchRange无效: " + batchRange);
        }

        // 验证索引关系
        if (!batchRange.verifyIndexRelation(sliceInfo.getStartIndex())) {
            throw new BusinessException(
                    String.format("BatchRange索引关系验证失败: batchRange=%s, sliceStartIndex=%d",
                            batchRange, sliceInfo.getStartIndex())
            );
        }

        log.debug("批次范围计算完成: localRange=[{}-{}], globalRange=[{}-{}]",
                localStartIndex, localEndIndex, globalStartIndex, globalEndIndex);

        return batchRange;
    }

    @Override
    public void addDualIndices(List<Map<String, Object>> rows, int batchStartIndex, SliceInfo sliceInfo) {
        // 参数验证（必须在使用参数之前）
        if (rows == null) {
            throw new IllegalArgumentException("rows不能为空");
        }
        if (sliceInfo == null) {
            throw new IllegalArgumentException("sliceInfo不能为空");
        }
        if (!sliceInfo.isValid()) {
            throw new IllegalArgumentException("sliceInfo无效: " + sliceInfo);
        }
        if (batchStartIndex < 0) {
            throw new IllegalArgumentException("batchStartIndex不能为负数: " + batchStartIndex);
        }

        log.debug("添加双重索引 (v1.5.1): rowCount={}, batchStartIndex={}, sliceStartIndex={}",
                rows.size(), batchStartIndex, sliceInfo.getStartIndex());

        // 为每一行添加双重索引
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);

            int localIndex = batchStartIndex + i;
            int globalIndex = sliceInfo.getStartIndex() + localIndex;

            // 添加索引字段
            row.put("localIndex", localIndex);
            row.put("globalIndex", globalIndex);

            // 验证索引关系
            if (globalIndex != sliceInfo.toGlobalIndex(localIndex)) {
                log.error("索引转换验证失败: localIndex={}, expectedGlobalIndex={}, actualGlobalIndex={}",
                        localIndex, globalIndex, sliceInfo.toGlobalIndex(localIndex));
                throw new BusinessException(
                        String.format("索引转换验证失败: localIndex=%d, globalIndex=%d", localIndex, globalIndex)
                );
            }
        }

        log.debug("双重索引添加完成: 处理{}行数据", rows.size());
    }

    /**
     * 解析分配策略字符串
     *
     * @param strategyStr 策略字符串（"IID", "RATIO", "NON_IID"）
     * @return AllocationStrategy枚举，如果无法解析则返回null
     */
    private AllocationStrategy parseStrategy(String strategyStr) {
        if (strategyStr == null || strategyStr.trim().isEmpty()) {
            return null;
        }
        return AllocationStrategy.fromCode(strategyStr.trim());
    }
}

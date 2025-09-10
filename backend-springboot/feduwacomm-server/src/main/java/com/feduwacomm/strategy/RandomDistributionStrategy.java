package com.feduwacomm.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机分发策略实现
 * 随机分配数据集到虚拟机，适合测试和简单场景
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class RandomDistributionStrategy implements DistributionStrategy {
    
    @Override
    public String getStrategyType() {
        return "RANDOM";
    }
    
    @Override
    public boolean validateConfig(DistributionConfig config) {
        if (config == null || config.getDistributionRequest() == null) {
            return false;
        }
        
        if (config.getDistributionRequest().getDatasetIds().isEmpty() ||
            config.getDistributionRequest().getTargetVmIds().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public DataAllocationPlan generateAllocationPlan(
            List<String> datasetIds, 
            List<String> vmIds, 
            DistributionConfig config) {
        
        log.info("生成随机分配计划: datasetCount={}, vmCount={}", datasetIds.size(), vmIds.size());
        
        try {
            DataAllocationPlan plan = new DataAllocationPlan();
            
            // 1. 获取随机种子（如果有配置）
            Long randomSeed = getRandomSeed(config);
            Random random = randomSeed != null ? new Random(randomSeed) : ThreadLocalRandom.current();
            
            // 2. 初始化VM分配记录
            List<VmAllocation> allocations = initializeVmAllocations(vmIds);
            
            // 3. 打乱数据集顺序
            List<String> shuffledDatasets = new ArrayList<>(datasetIds);
            Collections.shuffle(shuffledDatasets, random);
            
            // 4. 随机分配数据集
            Map<String, Long> datasetSizes = getDatasetSizes(config, datasetIds);
            allocateDatasets(shuffledDatasets, datasetSizes, allocations, random);
            
            plan.setAllocations(allocations);
            
            // 5. 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("strategyType", getStrategyType());
            metadata.put("randomSeed", randomSeed);
            metadata.put("totalDatasets", datasetIds.size());
            metadata.put("totalVms", vmIds.size());
            metadata.put("shuffled", true);
            plan.setMetadata(metadata);
            
            log.info("随机分配计划生成完成: allocations={}, seed={}", allocations.size(), randomSeed);
            
            return plan;
            
        } catch (Exception e) {
            log.error("生成随机分配计划失败", e);
            throw new RuntimeException("随机分配计划生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取随机种子
     */
    private Long getRandomSeed(DistributionConfig config) {
        if (config.getStrategyParams() != null) {
            Object seedObj = config.getStrategyParams().get("randomSeed");
            if (seedObj instanceof Number) {
                return ((Number) seedObj).longValue();
            }
        }
        return null;
    }
    
    /**
     * 初始化VM分配记录
     */
    private List<VmAllocation> initializeVmAllocations(List<String> vmIds) {
        List<VmAllocation> allocations = new ArrayList<>();
        
        for (String vmId : vmIds) {
            VmAllocation allocation = new VmAllocation();
            allocation.setVmId(vmId);
            allocation.setExpectedDataSize(0L);
            allocation.setExpectedRecordCount(0);
            allocations.add(allocation);
        }
        
        return allocations;
    }
    
    /**
     * 获取数据集大小信息
     */
    private Map<String, Long> getDatasetSizes(DistributionConfig config, List<String> datasetIds) {
        Map<String, Long> datasetSizes = config.getDatasetSizes();
        
        if (datasetSizes.isEmpty()) {
            // 使用默认大小估算
            datasetSizes = new HashMap<>();
            for (String datasetId : datasetIds) {
                // 随机生成数据集大小 (1MB - 10MB)
                long size = ThreadLocalRandom.current().nextLong(1024 * 1024, 10 * 1024 * 1024);
                datasetSizes.put(datasetId, size);
            }
        }
        
        return datasetSizes;
    }
    
    /**
     * 随机分配数据集
     */
    private void allocateDatasets(
            List<String> shuffledDatasets,
            Map<String, Long> datasetSizes,
            List<VmAllocation> allocations,
            Random random) {
        
        for (String datasetId : shuffledDatasets) {
            // 随机选择一个VM
            VmAllocation selectedVm = allocations.get(random.nextInt(allocations.size()));
            
            // 分配数据集
            selectedVm.getDatasetIds().add(datasetId);
            Long datasetSize = datasetSizes.get(datasetId);
            selectedVm.setExpectedDataSize(selectedVm.getExpectedDataSize() + datasetSize);
            selectedVm.setExpectedRecordCount(
                    selectedVm.getExpectedRecordCount() + estimateRecordCount(datasetSize));
            
            // 添加分配元数据
            selectedVm.getAllocationMetadata().put("randomAssignment", true);
            selectedVm.getAllocationMetadata().put("assignedAt", System.currentTimeMillis());
        }
    }
    
    /**
     * 估算记录数量
     */
    private Integer estimateRecordCount(Long dataSize) {
        // 简单估算：每1KB一条记录
        return (int) (dataSize / 1024);
    }
}
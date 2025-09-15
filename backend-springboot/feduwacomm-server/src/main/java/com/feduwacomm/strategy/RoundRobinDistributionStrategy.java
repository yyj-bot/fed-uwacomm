package com.feduwacomm.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 轮询分发策略实现
 * 按轮询方式依次分配数据集到虚拟机
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class RoundRobinDistributionStrategy implements DistributionStrategy {
    
    @Override
    public String getStrategyType() {
        return "ROUND_ROBIN";
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
        
        log.info("生成轮询分配计划: datasetCount={}, vmCount={}", datasetIds.size(), vmIds.size());
        
        try {
            DataAllocationPlan plan = new DataAllocationPlan();
            
            // 1. 初始化VM分配映射
            Map<String, VmAllocation> vmAllocationMap = initializeVmAllocations(vmIds);
            
            // 2. 获取数据集大小信息
            Map<String, Long> datasetSizes = getDatasetSizes(config, datasetIds);
            
            // 3. 按顺序进行轮询分配
            allocateDatasetsByRoundRobin(datasetIds, datasetSizes, vmIds, vmAllocationMap);
            
            // 4. 构建分配计划
            List<VmAllocation> allocations = new ArrayList<>(vmAllocationMap.values());
            plan.setAllocations(allocations);
            
            // 5. 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("strategyType", getStrategyType());
            metadata.put("totalDatasets", datasetIds.size());
            metadata.put("totalVms", vmIds.size());
            metadata.put("roundsCompleted", calculateCompletedRounds(datasetIds.size(), vmIds.size()));
            metadata.put("distributionPattern", "sequential");
            plan.setMetadata(metadata);
            
            log.info("轮询分配计划生成完成: allocations={}, rounds={}", 
                    allocations.size(), metadata.get("roundsCompleted"));
            
            return plan;
            
        } catch (Exception e) {
            log.error("生成轮询分配计划失败", e);
            throw new RuntimeException("轮询分配计划生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化VM分配记录
     */
    private Map<String, VmAllocation> initializeVmAllocations(List<String> vmIds) {
        Map<String, VmAllocation> vmAllocationMap = new LinkedHashMap<>(); // 保持顺序
        
        for (String vmId : vmIds) {
            VmAllocation allocation = new VmAllocation();
            allocation.setVmId(vmId);
            allocation.setExpectedDataSize(0L);
            allocation.setExpectedRecordCount(0);
            vmAllocationMap.put(vmId, allocation);
        }
        
        return vmAllocationMap;
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
                datasetSizes.put(datasetId, 2L * 1024 * 1024); // 默认2MB
            }
        }
        
        return datasetSizes;
    }
    
    /**
     * 按轮询方式分配数据集
     */
    private void allocateDatasetsByRoundRobin(
            List<String> datasetIds,
            Map<String, Long> datasetSizes,
            List<String> vmIds,
            Map<String, VmAllocation> vmAllocationMap) {
        
        int vmCount = vmIds.size();
        
        // 按轮询顺序分配每个数据集
        for (int i = 0; i < datasetIds.size(); i++) {
            String datasetId = datasetIds.get(i);
            String targetVmId = vmIds.get(i % vmCount); // 轮询选择VM
            
            VmAllocation allocation = vmAllocationMap.get(targetVmId);
            
            // 分配数据集
            allocation.getDatasetIds().add(datasetId);
            Long datasetSize = datasetSizes.get(datasetId);
            allocation.setExpectedDataSize(allocation.getExpectedDataSize() + datasetSize);
            allocation.setExpectedRecordCount(
                    allocation.getExpectedRecordCount() + estimateRecordCount(datasetSize));
            
            // 添加轮询分配元数据
            allocation.getAllocationMetadata().put("roundNumber", i / vmCount + 1);
            allocation.getAllocationMetadata().put("positionInRound", i % vmCount + 1);
            allocation.getAllocationMetadata().put("assignmentIndex", i);
            allocation.getAllocationMetadata().put("assignedAt", System.currentTimeMillis());
        }
        
        // 记录每个VM的分配统计
        for (VmAllocation allocation : vmAllocationMap.values()) {
            int assignedCount = allocation.getDatasetIds().size();
            allocation.getAllocationMetadata().put("assignedDatasetCount", assignedCount);
            allocation.getAllocationMetadata().put("averageDatasetSize", 
                    assignedCount > 0 ? allocation.getExpectedDataSize() / assignedCount : 0);
        }
    }
    
    /**
     * 计算完成的轮数
     */
    private int calculateCompletedRounds(int datasetCount, int vmCount) {
        return (int) Math.ceil((double) datasetCount / vmCount);
    }
    
    /**
     * 估算记录数量
     */
    private Integer estimateRecordCount(Long dataSize) {
        // 简单估算：每1KB一条记录
        return (int) (dataSize / 1024);
    }
}
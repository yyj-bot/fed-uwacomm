package com.feduwacomm.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 均衡分发策略实现
 * 根据虚拟机容量和数据大小进行均衡分配
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class BalancedDistributionStrategy implements DistributionStrategy {
    
    @Override
    public String getStrategyType() {
        return "BALANCED";
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
        
        log.info("生成均衡分配计划: datasetCount={}, vmCount={}", datasetIds.size(), vmIds.size());
        
        try {
            DataAllocationPlan plan = new DataAllocationPlan();
            
            // 1. 计算数据集总大小和每个数据集的大小
            Map<String, Long> datasetSizes = config.getDatasetSizes();
            if (datasetSizes.isEmpty()) {
                // 如果没有提供数据集大小信息，使用默认估算
                datasetSizes = estimateDatasetSizes(datasetIds);
            }
            
            // 2. 获取虚拟机容量信息
            Map<String, Integer> vmCapacities = config.getVmCapacities();
            if (vmCapacities.isEmpty()) {
                // 如果没有提供VM容量信息，假设所有VM容量相等
                vmCapacities = getDefaultVmCapacities(vmIds);
            }
            
            // 3. 计算总容量和数据分配比例
            int totalCapacity = vmCapacities.values().stream().mapToInt(Integer::intValue).sum();
            long totalDataSize = datasetSizes.values().stream().mapToLong(Long::longValue).sum();
            
            // 4. 为每个VM分配数据集
            List<VmAllocation> allocations = allocateDatasets(datasetIds, datasetSizes, vmIds, vmCapacities, totalCapacity);
            plan.setAllocations(allocations);
            
            // 5. 添加元数据信息
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("strategyType", getStrategyType());
            metadata.put("totalDataSize", totalDataSize);
            metadata.put("totalCapacity", totalCapacity);
            metadata.put("balanceScore", calculateBalanceScore(allocations));
            plan.setMetadata(metadata);
            
            log.info("均衡分配计划生成完成: allocations={}, balanceScore={}", 
                    allocations.size(), metadata.get("balanceScore"));
            
            return plan;
            
        } catch (Exception e) {
            log.error("生成均衡分配计划失败", e);
            throw new RuntimeException("均衡分配计划生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 分配数据集到虚拟机
     */
    private List<VmAllocation> allocateDatasets(
            List<String> datasetIds,
            Map<String, Long> datasetSizes,
            List<String> vmIds,
            Map<String, Integer> vmCapacities,
            int totalCapacity) {
        
        List<VmAllocation> allocations = new ArrayList<>();
        
        // 初始化VM分配记录
        Map<String, VmAllocation> vmAllocations = new HashMap<>();
        for (String vmId : vmIds) {
            VmAllocation allocation = new VmAllocation();
            allocation.setVmId(vmId);
            allocation.setExpectedDataSize(0L);
            allocation.setExpectedRecordCount(0);
            vmAllocations.put(vmId, allocation);
            allocations.add(allocation);
        }
        
        // 按数据集大小排序（从大到小），优先分配大的数据集
        List<String> sortedDatasetIds = new ArrayList<>(datasetIds);
        sortedDatasetIds.sort((a, b) -> Long.compare(datasetSizes.get(b), datasetSizes.get(a)));
        
        // 使用贪心算法进行分配
        for (String datasetId : sortedDatasetIds) {
            // 找到当前数据量最少的VM
            VmAllocation targetVm = findLeastLoadedVm(vmAllocations, vmCapacities);
            
            // 分配数据集
            targetVm.getDatasetIds().add(datasetId);
            Long datasetSize = datasetSizes.get(datasetId);
            targetVm.setExpectedDataSize(targetVm.getExpectedDataSize() + datasetSize);
            targetVm.setExpectedRecordCount(targetVm.getExpectedRecordCount() + estimateRecordCount(datasetSize));
            
            // 添加分配元数据
            targetVm.getAllocationMetadata().put("lastAssignedDataset", datasetId);
            targetVm.getAllocationMetadata().put("assignedAt", System.currentTimeMillis());
        }
        
        return allocations;
    }
    
    /**
     * 找到当前负载最少的虚拟机
     */
    private VmAllocation findLeastLoadedVm(Map<String, VmAllocation> vmAllocations, Map<String, Integer> vmCapacities) {
        VmAllocation leastLoaded = null;
        double minLoadRatio = Double.MAX_VALUE;
        
        for (VmAllocation allocation : vmAllocations.values()) {
            String vmId = allocation.getVmId();
            int capacity = vmCapacities.get(vmId);
            double loadRatio = (double) allocation.getExpectedDataSize() / capacity;
            
            if (loadRatio < minLoadRatio) {
                minLoadRatio = loadRatio;
                leastLoaded = allocation;
            }
        }
        
        return leastLoaded;
    }
    
    /**
     * 估算数据集大小
     */
    private Map<String, Long> estimateDatasetSizes(List<String> datasetIds) {
        Map<String, Long> sizes = new HashMap<>();
        
        // 简单估算：每个数据集1MB
        for (String datasetId : datasetIds) {
            sizes.put(datasetId, 1024L * 1024L);
        }
        
        return sizes;
    }
    
    /**
     * 获取默认VM容量
     */
    private Map<String, Integer> getDefaultVmCapacities(List<String> vmIds) {
        Map<String, Integer> capacities = new HashMap<>();
        
        // 假设所有VM容量相等
        for (String vmId : vmIds) {
            capacities.put(vmId, 100); // 默认容量权重100
        }
        
        return capacities;
    }
    
    /**
     * 估算记录数量
     */
    private Integer estimateRecordCount(Long dataSize) {
        // 简单估算：每1KB一条记录
        return (int) (dataSize / 1024);
    }
    
    /**
     * 计算分配平衡度评分 (0-100)
     */
    private Double calculateBalanceScore(List<VmAllocation> allocations) {
        if (allocations.isEmpty()) {
            return 100.0;
        }
        
        // 计算数据大小的方差
        double[] dataSizes = allocations.stream()
                .mapToDouble(a -> a.getExpectedDataSize().doubleValue())
                .toArray();
        
        double mean = Arrays.stream(dataSizes).average().orElse(0.0);
        double variance = Arrays.stream(dataSizes)
                .map(size -> Math.pow(size - mean, 2))
                .average()
                .orElse(0.0);
        
        double standardDeviation = Math.sqrt(variance);
        
        // 计算变异系数 (CV = 标准差 / 均值)
        double coefficientOfVariation = mean > 0 ? standardDeviation / mean : 0;
        
        // 转换为平衡度评分 (CV越小，平衡度越高)
        double balanceScore = Math.max(0, 100 - coefficientOfVariation * 100);
        
        return Math.min(100.0, balanceScore);
    }
}
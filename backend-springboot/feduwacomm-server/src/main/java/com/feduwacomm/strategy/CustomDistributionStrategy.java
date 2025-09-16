package com.feduwacomm.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 自定义分发策略实现
 * 根据用户配置的自定义规则进行数据分发
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class CustomDistributionStrategy implements DistributionStrategy {
    
    @Override
    public String getStrategyType() {
        return "CUSTOM";
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
        
        // 自定义策略需要有策略参数
        if (config.getStrategyParams() == null || config.getStrategyParams().isEmpty()) {
            log.warn("自定义分发策略缺少策略参数");
            return false;
        }
        
        return true;
    }
    
    @Override
    public DataAllocationPlan generateAllocationPlan(
            List<String> datasetIds, 
            List<String> vmIds, 
            DistributionConfig config) {
        
        log.info("生成自定义分配计划: datasetCount={}, vmCount={}", datasetIds.size(), vmIds.size());
        
        try {
            DataAllocationPlan plan = new DataAllocationPlan();
            
            // 1. 解析自定义策略参数
            Map<String, Object> strategyParams = config.getStrategyParams();
            CustomAllocationRules rules = parseAllocationRules(strategyParams);
            
            // 2. 初始化VM分配记录
            Map<String, VmAllocation> vmAllocationMap = initializeVmAllocations(vmIds);
            
            // 3. 获取数据集信息
            Map<String, Long> datasetSizes = getDatasetSizes(config, datasetIds);
            
            // 4. 执行自定义分配逻辑
            executeCustomAllocation(datasetIds, datasetSizes, vmAllocationMap, rules);
            
            // 5. 构建分配计划
            List<VmAllocation> allocations = new ArrayList<>(vmAllocationMap.values());
            plan.setAllocations(allocations);
            
            // 6. 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("strategyType", getStrategyType());
            metadata.put("customRules", rules.getRulesSummary());
            metadata.put("totalDatasets", datasetIds.size());
            metadata.put("totalVms", vmIds.size());
            metadata.put("allocationMethod", rules.getAllocationMethod());
            plan.setMetadata(metadata);
            
            log.info("自定义分配计划生成完成: allocations={}, method={}", 
                    allocations.size(), rules.getAllocationMethod());
            
            return plan;
            
        } catch (Exception e) {
            log.error("生成自定义分配计划失败", e);
            throw new RuntimeException("自定义分配计划生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析分配规则
     */
    private CustomAllocationRules parseAllocationRules(Map<String, Object> strategyParams) {
        CustomAllocationRules rules = new CustomAllocationRules();
        
        // 解析分配方法
        String method = (String) strategyParams.getOrDefault("allocationMethod", "PROPORTIONAL");
        rules.setAllocationMethod(method);
        
        // 解析VM权重
        @SuppressWarnings("unchecked")
        Map<String, Object> vmWeights = (Map<String, Object>) strategyParams.get("vmWeights");
        if (vmWeights != null) {
            Map<String, Double> weights = new HashMap<>();
            for (Map.Entry<String, Object> entry : vmWeights.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    weights.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }
            }
            rules.setVmWeights(weights);
        }
        
        // 解析数据集优先级
        @SuppressWarnings("unchecked")
        Map<String, Object> datasetPriorities = (Map<String, Object>) strategyParams.get("datasetPriorities");
        if (datasetPriorities != null) {
            Map<String, Integer> priorities = new HashMap<>();
            for (Map.Entry<String, Object> entry : datasetPriorities.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    priorities.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                }
            }
            rules.setDatasetPriorities(priorities);
        }
        
        // 解析VM容量限制
        @SuppressWarnings("unchecked")
        Map<String, Object> vmLimits = (Map<String, Object>) strategyParams.get("vmLimits");
        if (vmLimits != null) {
            Map<String, Long> limits = new HashMap<>();
            for (Map.Entry<String, Object> entry : vmLimits.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    limits.put(entry.getKey(), ((Number) entry.getValue()).longValue());
                }
            }
            rules.setVmLimits(limits);
        }
        
        // 解析数据集过滤规则
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filterRules = (List<Map<String, Object>>) strategyParams.get("filterRules");
        if (filterRules != null) {
            rules.setFilterRules(filterRules);
        }
        
        return rules;
    }
    
    /**
     * 初始化VM分配记录
     */
    private Map<String, VmAllocation> initializeVmAllocations(List<String> vmIds) {
        Map<String, VmAllocation> vmAllocationMap = new HashMap<>();
        
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
                datasetSizes.put(datasetId, 3L * 1024 * 1024); // 默认3MB
            }
        }
        
        return datasetSizes;
    }
    
    /**
     * 执行自定义分配逻辑
     */
    private void executeCustomAllocation(
            List<String> datasetIds,
            Map<String, Long> datasetSizes,
            Map<String, VmAllocation> vmAllocationMap,
            CustomAllocationRules rules) {
        
        String method = rules.getAllocationMethod();
        
        switch (method) {
            case "PROPORTIONAL":
                executeProportionalAllocation(datasetIds, datasetSizes, vmAllocationMap, rules);
                break;
            case "PRIORITY":
                executePriorityAllocation(datasetIds, datasetSizes, vmAllocationMap, rules);
                break;
            case "CAPACITY_BASED":
                executeCapacityBasedAllocation(datasetIds, datasetSizes, vmAllocationMap, rules);
                break;
            case "EXPLICIT":
                executeExplicitAllocation(datasetIds, datasetSizes, vmAllocationMap, rules);
                break;
            default:
                log.warn("未知的自定义分配方法: {}, 回退到按比例分配", method);
                executeProportionalAllocation(datasetIds, datasetSizes, vmAllocationMap, rules);
        }
    }
    
    /**
     * 按比例分配
     */
    private void executeProportionalAllocation(
            List<String> datasetIds,
            Map<String, Long> datasetSizes,
            Map<String, VmAllocation> vmAllocationMap,
            CustomAllocationRules rules) {
        
        Map<String, Double> vmWeights = rules.getVmWeights();
        if (vmWeights.isEmpty()) {
            // 如果没有权重，均等分配
            vmWeights = new HashMap<>();
            for (String vmId : vmAllocationMap.keySet()) {
                vmWeights.put(vmId, 1.0);
            }
        }
        
        // 计算总权重
        double totalWeight = vmWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // 按权重分配数据集
        int datasetIndex = 0;
        for (Map.Entry<String, Double> entry : vmWeights.entrySet()) {
            String vmId = entry.getKey();
            double weight = entry.getValue();
            
            // 计算该VM应分配的数据集数量
            int expectedCount = (int) Math.round(datasetIds.size() * weight / totalWeight);
            
            VmAllocation allocation = vmAllocationMap.get(vmId);
            for (int i = 0; i < expectedCount && datasetIndex < datasetIds.size(); i++) {
                String datasetId = datasetIds.get(datasetIndex++);
                allocateDatasetToVm(datasetId, datasetSizes.get(datasetId), allocation);
            }
            
            allocation.getAllocationMetadata().put("weight", weight);
            allocation.getAllocationMetadata().put("expectedRatio", weight / totalWeight);
        }
        
        // 分配剩余的数据集
        while (datasetIndex < datasetIds.size()) {
            // 找到当前分配最少的VM
            VmAllocation leastLoaded = vmAllocationMap.values().stream()
                    .min(Comparator.comparing(a -> a.getDatasetIds().size()))
                    .orElse(vmAllocationMap.values().iterator().next());
            
            String datasetId = datasetIds.get(datasetIndex++);
            allocateDatasetToVm(datasetId, datasetSizes.get(datasetId), leastLoaded);
        }
    }
    
    /**
     * 基于优先级的分配
     */
    private void executePriorityAllocation(
            List<String> datasetIds,
            Map<String, Long> datasetSizes,
            Map<String, VmAllocation> vmAllocationMap,
            CustomAllocationRules rules) {
        
        Map<String, Integer> priorities = rules.getDatasetPriorities();
        
        // 按优先级排序数据集
        List<String> sortedDatasets = new ArrayList<>(datasetIds);
        sortedDatasets.sort((a, b) -> {
            int priorityA = priorities.getOrDefault(a, 0);
            int priorityB = priorities.getOrDefault(b, 0);
            return Integer.compare(priorityB, priorityA); // 高优先级在前
        });
        
        // 按优先级顺序分配
        List<String> vmIdList = new ArrayList<>(vmAllocationMap.keySet());
        int vmIndex = 0;
        
        for (String datasetId : sortedDatasets) {
            String targetVmId = vmIdList.get(vmIndex % vmIdList.size());
            VmAllocation allocation = vmAllocationMap.get(targetVmId);
            
            allocateDatasetToVm(datasetId, datasetSizes.get(datasetId), allocation);
            allocation.getAllocationMetadata().put("priority_" + datasetId, priorities.getOrDefault(datasetId, 0));
            
            vmIndex++;
        }
    }
    
    /**
     * 基于容量的分配
     */
    private void executeCapacityBasedAllocation(
            List<String> datasetIds,
            Map<String, Long> datasetSizes,
            Map<String, VmAllocation> vmAllocationMap,
            CustomAllocationRules rules) {
        
        Map<String, Long> vmLimits = rules.getVmLimits();
        
        // 按数据集大小排序（从大到小）
        List<String> sortedDatasets = new ArrayList<>(datasetIds);
        sortedDatasets.sort((a, b) -> Long.compare(datasetSizes.get(b), datasetSizes.get(a)));
        
        // 贪心分配：优先分配大数据集到剩余容量最大的VM
        for (String datasetId : sortedDatasets) {
            long datasetSize = datasetSizes.get(datasetId);
            
            // 找到剩余容量最大且能容纳该数据集的VM
            VmAllocation targetVm = null;
            long maxRemainingCapacity = -1;
            
            for (VmAllocation allocation : vmAllocationMap.values()) {
                String vmId = allocation.getVmId();
                long limit = vmLimits.getOrDefault(vmId, Long.MAX_VALUE);
                long used = allocation.getExpectedDataSize();
                long remaining = limit - used;
                
                if (remaining >= datasetSize && remaining > maxRemainingCapacity) {
                    maxRemainingCapacity = remaining;
                    targetVm = allocation;
                }
            }
            
            if (targetVm != null) {
                allocateDatasetToVm(datasetId, datasetSize, targetVm);
                targetVm.getAllocationMetadata().put("remainingCapacity", maxRemainingCapacity - datasetSize);
            } else {
                log.warn("无法为数据集找到合适的VM: datasetId={}, size={}", datasetId, datasetSize);
            }
        }
    }
    
    /**
     * 显式分配（用户指定具体分配关系）
     */
    private void executeExplicitAllocation(
            List<String> datasetIds,
            Map<String, Long> datasetSizes,
            Map<String, VmAllocation> vmAllocationMap,
            CustomAllocationRules rules) {
        
        // TODO: 实现显式分配逻辑
        // 这里需要从规则中解析具体的数据集到VM的映射关系
        log.warn("显式分配方法尚未完全实现，回退到按比例分配");
        executeProportionalAllocation(datasetIds, datasetSizes, vmAllocationMap, rules);
    }
    
    /**
     * 分配数据集到VM
     */
    private void allocateDatasetToVm(String datasetId, Long datasetSize, VmAllocation allocation) {
        allocation.getDatasetIds().add(datasetId);
        allocation.setExpectedDataSize(allocation.getExpectedDataSize() + datasetSize);
        allocation.setExpectedRecordCount(
                allocation.getExpectedRecordCount() + estimateRecordCount(datasetSize));
        allocation.getAllocationMetadata().put("lastAssigned", datasetId);
        allocation.getAllocationMetadata().put("assignedAt", System.currentTimeMillis());
    }
    
    /**
     * 估算记录数量
     */
    private Integer estimateRecordCount(Long dataSize) {
        return (int) (dataSize / 1024);
    }
    
    /**
     * 自定义分配规则类
     */
    private static class CustomAllocationRules {
        private String allocationMethod = "PROPORTIONAL";
        private Map<String, Double> vmWeights = new HashMap<>();
        private Map<String, Integer> datasetPriorities = new HashMap<>();
        private Map<String, Long> vmLimits = new HashMap<>();
        private List<Map<String, Object>> filterRules = new ArrayList<>();
        
        // Getters and setters
        public String getAllocationMethod() { return allocationMethod; }
        public void setAllocationMethod(String allocationMethod) { this.allocationMethod = allocationMethod; }
        
        public Map<String, Double> getVmWeights() { return vmWeights; }
        public void setVmWeights(Map<String, Double> vmWeights) { this.vmWeights = vmWeights; }
        
        public Map<String, Integer> getDatasetPriorities() { return datasetPriorities; }
        public void setDatasetPriorities(Map<String, Integer> datasetPriorities) { this.datasetPriorities = datasetPriorities; }
        
        public Map<String, Long> getVmLimits() { return vmLimits; }
        public void setVmLimits(Map<String, Long> vmLimits) { this.vmLimits = vmLimits; }
        
        public List<Map<String, Object>> getFilterRules() { return filterRules; }
        public void setFilterRules(List<Map<String, Object>> filterRules) { this.filterRules = filterRules; }
        
        public Map<String, Object> getRulesSummary() {
            Map<String, Object> summary = new HashMap<>();
            summary.put("method", allocationMethod);
            summary.put("vmCount", vmWeights.size());
            summary.put("priorityDatasets", datasetPriorities.size());
            summary.put("limitedVms", vmLimits.size());
            summary.put("filterRules", filterRules.size());
            return summary;
        }
    }
}
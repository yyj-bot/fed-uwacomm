package com.feduwacomm.strategy;

import com.feduwacomm.dto.DataDistributionDTO;

import java.util.List;
import java.util.Map;

/**
 * 数据分发策略接口
 * 根据重构计划实现多种分发策略
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public interface DistributionStrategy {
    
    /**
     * 生成数据分配计划
     * 
     * @param datasetIds 数据集ID列表
     * @param vmIds 虚拟机ID列表
     * @param config 分发配置
     * @return 数据分配计划
     */
    DataAllocationPlan generateAllocationPlan(
            List<String> datasetIds, 
            List<String> vmIds, 
            DistributionConfig config
    );
    
    /**
     * 获取策略类型
     */
    String getStrategyType();
    
    /**
     * 验证策略配置
     */
    boolean validateConfig(DistributionConfig config);
    
    /**
     * 数据分配计划
     */
    class DataAllocationPlan {
        private List<VmAllocation> allocations;
        private Map<String, Object> metadata;
        
        public DataAllocationPlan() {
            this.allocations = new java.util.ArrayList<>();
            this.metadata = new java.util.HashMap<>();
        }
        
        public List<VmAllocation> getAllocations() { return allocations; }
        public void setAllocations(List<VmAllocation> allocations) { this.allocations = allocations; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * 虚拟机分配信息
     */
    class VmAllocation {
        private String vmId;
        private List<String> datasetIds;
        private Long expectedDataSize;
        private Integer expectedRecordCount;
        private Map<String, Object> allocationMetadata;
        
        public VmAllocation() {
            this.datasetIds = new java.util.ArrayList<>();
            this.allocationMetadata = new java.util.HashMap<>();
        }
        
        public String getVmId() { return vmId; }
        public void setVmId(String vmId) { this.vmId = vmId; }
        
        public List<String> getDatasetIds() { return datasetIds; }
        public void setDatasetIds(List<String> datasetIds) { this.datasetIds = datasetIds; }
        
        public Long getExpectedDataSize() { return expectedDataSize; }
        public void setExpectedDataSize(Long expectedDataSize) { this.expectedDataSize = expectedDataSize; }
        
        public Integer getExpectedRecordCount() { return expectedRecordCount; }
        public void setExpectedRecordCount(Integer expectedRecordCount) { this.expectedRecordCount = expectedRecordCount; }
        
        public Map<String, Object> getAllocationMetadata() { return allocationMetadata; }
        public void setAllocationMetadata(Map<String, Object> allocationMetadata) { this.allocationMetadata = allocationMetadata; }
    }
    
    /**
     * 分发配置
     */
    class DistributionConfig {
        private DataDistributionDTO distributionRequest;
        private Map<String, Object> strategyParams;
        private Map<String, Long> datasetSizes;
        private Map<String, Integer> vmCapacities;
        
        public DistributionConfig() {
            this.strategyParams = new java.util.HashMap<>();
            this.datasetSizes = new java.util.HashMap<>();
            this.vmCapacities = new java.util.HashMap<>();
        }
        
        public DataDistributionDTO getDistributionRequest() { return distributionRequest; }
        public void setDistributionRequest(DataDistributionDTO distributionRequest) { this.distributionRequest = distributionRequest; }
        
        public Map<String, Object> getStrategyParams() { return strategyParams; }
        public void setStrategyParams(Map<String, Object> strategyParams) { this.strategyParams = strategyParams; }
        
        public Map<String, Long> getDatasetSizes() { return datasetSizes; }
        public void setDatasetSizes(Map<String, Long> datasetSizes) { this.datasetSizes = datasetSizes; }
        
        public Map<String, Integer> getVmCapacities() { return vmCapacities; }
        public void setVmCapacities(Map<String, Integer> vmCapacities) { this.vmCapacities = vmCapacities; }
    }
}
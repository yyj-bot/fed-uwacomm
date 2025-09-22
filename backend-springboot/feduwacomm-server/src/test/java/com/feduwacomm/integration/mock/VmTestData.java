package com.feduwacomm.integration.mock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 虚拟机测试数据封装类
 * 用于存储虚拟机的基本配置信息和测试过程中动态生成的数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VmTestData {

    /**
     * 虚拟机ID - 由后端注册时动态生成
     * 格式: vm-{uuid}
     */
    private String vmId;

    /**
     * 虚拟机名称 - 固定标识符
     */
    private String name;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 端口号
     */
    private int port;

    /**
     * CPU核心数
     */
    private int cpuCores;

    /**
     * 内存大小 (MB)
     */
    private int memoryMb;

    /**
     * GPU数量
     */
    private int gpuCount;

    /**
     * 获取虚拟机能力配置
     */
    public java.util.Map<String, Object> getCapabilities() {
        java.util.Map<String, Object> capabilities = new java.util.HashMap<>();

        // 基于硬件配置决定算法支持
        java.util.List<String> algorithms = new java.util.ArrayList<>();
        algorithms.add("FEDAVG");
        if (cpuCores >= 8) {
            algorithms.add("FEDPROX");
        }
        if (gpuCount > 0) {
            algorithms.add("FEDOPT");
        }

        capabilities.put("cpuCores", cpuCores);
        capabilities.put("memoryMb", memoryMb);
        capabilities.put("gpuCount", gpuCount);
        capabilities.put("algorithms", algorithms);
        capabilities.put("maxBatchSize", Math.min(1024, memoryMb / 8));
        capabilities.put("concurrent_tasks", Math.max(1, cpuCores / 4));

        return capabilities;
    }

    /**
     * 获取系统信息
     */
    public java.util.Map<String, Object> getSystemInfo() {
        java.util.Map<String, Object> systemInfo = new java.util.HashMap<>();

        systemInfo.put("os", "Ubuntu 20.04");
        systemInfo.put("kernel", "5.4.0-74-generic");
        systemInfo.put("pythonVersion", "3.8.10");
        systemInfo.put("frameworks", java.util.Arrays.asList("pytorch", "tensorflow", "scikit-learn"));

        if (gpuCount > 0) {
            systemInfo.put("gpu", "NVIDIA RTX 3080");
            systemInfo.put("cudaVersion", "11.4");
        }

        return systemInfo;
    }

    /**
     * 基于VM名称获取数据分配数量
     * 用于测试过程中的数据分配
     */
    public int getDataPointsForTesting() {
        switch (name) {
            case "VM-Node-1": return 1600;  // 20% of 8000
            case "VM-Node-2": return 1200;  // 15% of 8000
            case "VM-Node-3": return 1200;  // 15% of 8000
            case "VM-Node-4": return 2000;  // 25% of 8000
            case "VM-Node-5": return 2000;  // 25% of 8000
            default: return 1000;
        }
    }

    /**
     * 基于VM硬件配置计算基础训练时间（毫秒）
     */
    public int getBaseTrainingTime() {
        double cpuFactor = 1.0 / (cpuCores / 4.0);
        double gpuFactor = gpuCount > 0 ? 0.6 : 1.0;
        double memoryFactor = memoryMb < 8192 ? 1.2 : 1.0;

        return (int)(2000 * cpuFactor * gpuFactor * memoryFactor);
    }

    /**
     * 获取VM性能系数（用于生成训练指标）
     */
    public double getPerformanceFactor() {
        return (cpuCores + gpuCount * 4.0) / 20.0;
    }
}
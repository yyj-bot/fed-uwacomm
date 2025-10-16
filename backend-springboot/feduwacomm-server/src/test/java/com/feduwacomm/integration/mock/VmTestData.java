package com.feduwacomm.integration.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 虚拟机测试数据模型
 * 提供联邦流程集成测试所需的虚拟机静态信息。
 */
public class VmTestData {

    private String vmId;
    private final String name;
    private final String ipAddress;
    private final int port;
    private final int cpuCores;
    private final int memoryMb;
    private final int gpuCount;
    private final Map<String, Object> capabilities;
    private final Map<String, Object> systemInfo;
    private final int baseTrainingTime;
    private final int dataPointsForTesting;
    private final double performanceFactor;

    public VmTestData(String vmId,
                      String name,
                      String ipAddress,
                      int port,
                      int cpuCores,
                      int memoryMb,
                      int gpuCount) {
        this.vmId = vmId;
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
        this.cpuCores = cpuCores;
        this.memoryMb = memoryMb;
        this.gpuCount = gpuCount;
        this.capabilities = new HashMap<>();
        capabilities.put("supportsV15", Boolean.TRUE);
        capabilities.put("features", List.of(
            gpuCount > 0 ? "GPU_ACCELERATION" : "CPU_ONLY",
            "DATA_ENCRYPTION",
            "SLICE_VERIFICATION"
        ));
        capabilities.put("supportedAlgorithms", List.of("FEDERATED_AVERAGING", "FEDERATED_PROXIMAL"));
        capabilities.put("maxBatchSize", 256);
        capabilities.put("gpuMemoryMb", gpuCount > 0 ? 8192 : 0);
        Map<String, Object> info = new HashMap<>();
        info.put("os", "Ubuntu 22.04");
        info.put("kernel", "5.15.0");
        info.put("gpuCount", gpuCount);
        info.put("cpuCores", cpuCores);
        info.put("memoryMb", memoryMb);
        this.systemInfo = info;

        this.baseTrainingTime = 120; // 默认基础训练时间（秒）
        this.dataPointsForTesting = 5000;
        this.performanceFactor = gpuCount > 0 ? 1.2 : 1.0;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public int getCpuCores() {
        return cpuCores;
    }

    public int getMemoryMb() {
        return memoryMb;
    }

    public int getGpuCount() {
        return gpuCount;
    }

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }

    public Map<String, Object> getSystemInfo() {
        return systemInfo;
    }

    public int getBaseTrainingTime() {
        return baseTrainingTime;
    }

    public int getDataPointsForTesting() {
        return dataPointsForTesting;
    }

    public double getPerformanceFactor() {
        return performanceFactor;
    }
}

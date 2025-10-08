package com.feduwacomm.enums;

import lombok.Getter;

/**
 * 数据分配策略枚举 (v1.5.1)
 * 定义联邦学习中数据集分配到虚拟机的策略类型
 *
 * <p>v1.5.1核心特性：
 * <ul>
 *   <li>IID策略：独立同分布，数据均匀随机分配</li>
 *   <li>NON_IID策略：非独立同分布，按特定规则聚类分配</li>
 * </ul>
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@Getter
public enum AllocationStrategy {

    /**
     * IID (Independent and Identically Distributed) - 独立同分布
     *
     * <p>特点：
     * <ul>
     *   <li>数据在各虚拟机间均匀分配</li>
     *   <li>每个VM接收的数据样本数量基本相等</li>
     *   <li>数据分布相对均衡，类别分布相似</li>
     *   <li>适用于标准联邦学习场景</li>
     * </ul>
     *
     * <p>分配算法：
     * <pre>
     * baseSize = totalSamples / vmCount
     * remainder = totalSamples % vmCount
     * 前remainder个VM分配(baseSize + 1)个样本
     * 其余VM分配baseSize个样本
     * </pre>
     *
     * <p>示例：
     * 10000样本分给5个VM：
     * - VM1: [0-1999]      (2000个样本)
     * - VM2: [2000-3999]   (2000个样本)
     * - VM3: [4000-5999]   (2000个样本)
     * - VM4: [6000-7999]   (2000个样本)
     * - VM5: [8000-9999]   (2000个样本)
     */
    IID("IID", "Independent and Identically Distributed", "独立同分布"),

    /**
     * NON_IID (Non-Independent and Identically Distributed) - 非独立同分布
     *
     * <p>特点：
     * <ul>
     *   <li>数据按特定规则聚类分配</li>
     *   <li>每个VM接收的数据样本数量可能不等</li>
     *   <li>数据分布不均衡，类别分布差异较大</li>
     *   <li>适用于模拟真实联邦学习场景（如不同医院的病例数据）</li>
     * </ul>
     *
     * <p>分配算法（按标签聚类）：
     * <pre>
     * 1. 按数据标签（label）分组
     * 2. 为每个VM分配特定标签的数据
     * 3. 可以控制每个VM的标签集合和样本比例
     * </pre>
     *
     * <p>示例（按标签聚类）：
     * 10000样本（5个类别，每个类别2000样本）分给5个VM：
     * - VM1: 类别0数据[0-1999]        (2000个样本，单一类别)
     * - VM2: 类别1数据[2000-3999]     (2000个样本，单一类别)
     * - VM3: 类别2数据[4000-5999]     (2000个样本，单一类别)
     * - VM4: 类别3数据[6000-7999]     (2000个样本，单一类别)
     * - VM5: 类别4数据[8000-9999]     (2000个样本，单一类别)
     */
    NON_IID("NON_IID", "Non-Independent and Identically Distributed", "非独立同分布"),

    /**
     * RATIO (Ratio-based Distribution) - 按比例分配
     *
     * <p>特点：
     * <ul>
     *   <li>根据预定义的比例分配数据</li>
     *   <li>每个VM接收的数据样本数量按比例计算</li>
     *   <li>支持自定义比例（如7:2:1）</li>
     *   <li>适用于非均匀算力场景（不同VM配置不同）</li>
     * </ul>
     *
     * <p>分配算法：
     * <pre>
     * ratios = [7, 2, 1]  // 比例配置
     * ratioSum = 7 + 2 + 1 = 10
     * VM1样本数 = totalSamples * 7/10
     * VM2样本数 = totalSamples * 2/10
     * VM3样本数 = totalSamples * 1/10
     * </pre>
     *
     * <p>示例（7:2:1比例）：
     * 10000样本分给3个VM：
     * - VM1: [0-6999]      (7000个样本，70%)
     * - VM2: [7000-8999]   (2000个样本，20%)
     * - VM3: [9000-9999]   (1000个样本，10%)
     */
    RATIO("RATIO", "Ratio-based Distribution", "按比例分配");

    /**
     * 策略代码（用于协议传输）
     */
    private final String code;

    /**
     * 策略英文名称
     */
    private final String englishName;

    /**
     * 策略中文描述
     */
    private final String chineseName;

    /**
     * 构造函数
     *
     * @param code         策略代码
     * @param englishName  英文名称
     * @param chineseName  中文名称
     */
    AllocationStrategy(String code, String englishName, String chineseName) {
        this.code = code;
        this.englishName = englishName;
        this.chineseName = chineseName;
    }

    /**
     * 根据策略代码获取枚举值
     *
     * @param code 策略代码（"IID" 或 "NON_IID"）
     * @return AllocationStrategy枚举值，如果不存在则返回null
     */
    public static AllocationStrategy fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AllocationStrategy strategy : values()) {
            if (strategy.code.equalsIgnoreCase(code)) {
                return strategy;
            }
        }
        return null;
    }

    /**
     * 判断是否为IID策略
     *
     * @return true if 策略为IID
     */
    public boolean isIID() {
        return this == IID;
    }

    /**
     * 判断是否为NON_IID策略
     *
     * @return true if 策略为NON_IID
     */
    public boolean isNonIID() {
        return this == NON_IID;
    }

    /**
     * 判断是否为RATIO策略
     *
     * @return true if 策略为RATIO
     */
    public boolean isRatio() {
        return this == RATIO;
    }

    @Override
    public String toString() {
        return code;
    }
}
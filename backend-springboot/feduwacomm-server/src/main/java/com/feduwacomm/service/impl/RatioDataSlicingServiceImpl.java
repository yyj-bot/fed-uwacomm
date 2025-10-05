package com.feduwacomm.service.impl;

import com.feduwacomm.dto.DataSliceResult;
import com.feduwacomm.dto.SliceInfo;
import com.feduwacomm.enums.AllocationStrategy;
import com.feduwacomm.mapper.TrainingDatasetMapper;
import com.feduwacomm.mapper.TrainingDatasetRowMapper;
import com.feduwacomm.service.DataSlicingService;
import com.feduwacomm.utils.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 按比例数据切分服务实现 (v1.5.1.1)
 * 提供按自定义比例的数据分配工具方法
 *
 * <p>该类是底层工具服务，不实现DataSlicingService接口，供其他服务调用
 *
 * <p>RATIO策略特点：
 * <ul>
 *   <li>千分比权重：推荐使用总和=1000的权重配置</li>
 *   <li>自定义比例：支持任意比例配置（系统自动归一化）</li>
 *   <li>灵活分配：适应不同VM算力场景</li>
 *   <li>精确切分：按比例精确计算样本数量</li>
 *   <li>余数处理：按比例权重分配余数</li>
 * </ul>
 *
 * <p>推荐配置（千分比权重，总和=1000）：
 * <pre>
 * ratios = [700, 200, 100]  // 7:2:1比例
 * ratioSum = 700 + 200 + 100 = 1000
 * VM1: totalSamples * 700/1000 = 70%数据 [0-6999]
 * VM2: totalSamples * 200/1000 = 20%数据 [7000-8999]
 * VM3: totalSamples * 100/1000 = 10%数据 [9000-9999]
 * </pre>
 *
 * <p>兼容性说明：
 * 该服务支持任意ratioSum，系统会自动归一化计算，但推荐使用1000作为标准总和。
 *
 * @author FedUWAComm Team
 * @version 1.5.1.1
 * @since 2025-10-01
 */
@Slf4j
@Service("ratioDataSlicingService")
public class RatioDataSlicingServiceImpl {

    private final TrainingDatasetMapper trainingDatasetMapper;
    private final TrainingDatasetRowMapper trainingDatasetRowMapper;
    private final UuidUtil uuidUtil;

    public RatioDataSlicingServiceImpl(TrainingDatasetMapper trainingDatasetMapper,
                                       TrainingDatasetRowMapper trainingDatasetRowMapper,
                                       UuidUtil uuidUtil) {
        this.trainingDatasetMapper = trainingDatasetMapper;
        this.trainingDatasetRowMapper = trainingDatasetRowMapper;
        this.uuidUtil = uuidUtil;
    }

    /**
     * 按均等比例切分数据集（所有VM比例相等）
     * 该方法供IIDDataSlicingServiceImpl调用
     *
     * @param datasetId 数据集ID
     * @param vmIds     虚拟机ID列表
     * @param strategy  分配策略
     * @return 数据切片结果列表
     */
    public List<DataSliceResult> sliceDatasetWithEqualRatios(String datasetId, List<String> vmIds, AllocationStrategy strategy) {
        // 默认使用均等比例（所有比例都是1）
        List<Integer> equalRatios = new ArrayList<>();
        for (int i = 0; i < vmIds.size(); i++) {
            equalRatios.add(1);
        }
        return sliceDatasetByRatio(datasetId, vmIds, equalRatios, strategy);
    }

    /**
     * 按指定比例切分数据集
     *
     * @param datasetId 原始数据集ID
     * @param vmIds     虚拟机ID列表
     * @param ratios    比例数组（例如[7, 2, 1]）
     * @param strategy  分配策略（通常为RATIO）
     * @return 数据切片结果列表
     */
    public List<DataSliceResult> sliceDatasetByRatio(String datasetId,
                                                     List<String> vmIds,
                                                     List<Integer> ratios,
                                                     AllocationStrategy strategy) {
        // 1. 参数验证
        validateParameters(datasetId, vmIds, ratios);

        log.info("开始按比例数据切分: datasetId={}, vmCount={}, ratios={}, strategy={}",
                datasetId, vmIds.size(), ratios, strategy);

        // 2. 获取数据集大小
        int totalSamples = getDatasetSize(datasetId);
        if (totalSamples == 0) {
            throw new DataSlicingService.DataSlicingException("数据集为空，无法切分: datasetId=" + datasetId);
        }

        int vmCount = vmIds.size();

        // 3. 计算比例总和
        int ratioSum = ratios.stream().mapToInt(Integer::intValue).sum();
        if (ratioSum == 0) {
            throw new DataSlicingService.DataSlicingException("比例总和不能为0");
        }

        log.debug("切分参数: totalSamples={}, ratioSum={}, ratios={}",
                totalSamples, ratioSum, ratios);

        // 4. 计算每个VM的样本数量
        List<Integer> sampleCounts = calculateSampleCounts(totalSamples, ratios, ratioSum);

        // 5. 为每个VM生成切片
        List<DataSliceResult> results = new ArrayList<>();
        int currentIndex = 0;

        for (int i = 0; i < vmCount; i++) {
            String vmId = vmIds.get(i);
            int sliceSize = sampleCounts.get(i);

            if (sliceSize == 0) {
                log.warn("VM {} 未分配数据（比例为0或样本数不足）", vmId);
                continue;
            }

            int startIndex = currentIndex;
            int endIndex = Math.min(currentIndex + sliceSize - 1, totalSamples - 1);
            int actualSize = endIndex - startIndex + 1;

            // 6. 生成SliceInfo
            SliceInfo sliceInfo = generateSliceInfo(
                    i + 1,          // sliceIndex (从1开始)
                    startIndex,
                    endIndex,
                    totalSamples,
                    vmCount,
                    strategy
            );

            // 7. 生成assignedDatasetId
            String assignedDatasetId = uuidUtil.generateUuid();

            // 8. 创建切片结果
            DataSliceResult result = DataSliceResult.builder()
                    .vmId(vmId)
                    .sliceInfo(sliceInfo)
                    .assignedDatasetId(assignedDatasetId)
                    .build();

            results.add(result);

            log.debug("VM {} 切片生成: [{}-{}], size={}, ratio={}, assignedDatasetId={}",
                    vmId, startIndex, endIndex, actualSize, ratios.get(i), assignedDatasetId);

            currentIndex += actualSize;
        }

        // 9. 验证切片完整性
        validateSlicesCoverage(results, totalSamples);

        log.info("按比例数据切分完成: 总样本数={}, 切片数={}", totalSamples, results.size());

        return results;
    }

    /**
     * 计算每个VM应分配的样本数量
     * 使用比例权重分配，处理余数问题
     *
     * @param totalSamples 总样本数
     * @param ratios       比例数组
     * @param ratioSum     比例总和
     * @return 每个VM的样本数量列表
     */
    private List<Integer> calculateSampleCounts(int totalSamples, List<Integer> ratios, int ratioSum) {
        List<Integer> sampleCounts = new ArrayList<>();
        int allocatedSamples = 0;

        // 第一遍分配：按比例计算基础样本数
        for (int i = 0; i < ratios.size(); i++) {
            int ratio = ratios.get(i);
            // 使用整数除法计算基础样本数
            int baseSamples = (totalSamples * ratio) / ratioSum;
            sampleCounts.add(baseSamples);
            allocatedSamples += baseSamples;
        }

        // 第二遍分配：处理余数
        // 余数按比例权重分配给前N个VM
        int remainder = totalSamples - allocatedSamples;
        if (remainder > 0) {
            log.debug("处理余数: remainder={}, 将按比例权重分配", remainder);

            // 为每个VM计算分配余数的权重
            // 权重 = ratio / ratioSum
            List<Double> weights = new ArrayList<>();
            for (int ratio : ratios) {
                weights.add((double) ratio / ratioSum);
            }

            // 按权重降序排列VM索引
            List<Integer> vmIndices = new ArrayList<>();
            for (int i = 0; i < ratios.size(); i++) {
                vmIndices.add(i);
            }
            vmIndices.sort((i1, i2) -> Double.compare(weights.get(i2), weights.get(i1)));

            // 分配余数（优先分配给权重大的VM）
            for (int i = 0; i < remainder && i < vmIndices.size(); i++) {
                int vmIndex = vmIndices.get(i);
                sampleCounts.set(vmIndex, sampleCounts.get(vmIndex) + 1);
            }
        }

        return sampleCounts;
    }

    /**
     * 验证参数有效性
     */
    private void validateParameters(String datasetId, List<String> vmIds, List<Integer> ratios) {
        if (datasetId == null || datasetId.isEmpty()) {
            throw new IllegalArgumentException("datasetId不能为空");
        }
        if (vmIds == null || vmIds.isEmpty()) {
            throw new IllegalArgumentException("vmIds列表不能为空");
        }
        if (ratios == null || ratios.isEmpty()) {
            throw new IllegalArgumentException("ratios不能为空");
        }
        if (vmIds.size() != ratios.size()) {
            throw new IllegalArgumentException(
                    String.format("VM数量(%d)与比例数量(%d)不匹配", vmIds.size(), ratios.size()));
        }
        // 验证比例都是非负数
        for (int i = 0; i < ratios.size(); i++) {
            if (ratios.get(i) < 0) {
                throw new IllegalArgumentException(
                        String.format("比例值不能为负数: index=%d, value=%d", i, ratios.get(i)));
            }
        }
    }

    /**
     * 生成切片信息
     */
    public SliceInfo generateSliceInfo(int sliceIndex, int startIndex, int endIndex,
                                        int totalSamples, int totalSlices,
                                        AllocationStrategy strategy) {
        int sliceSamples = endIndex - startIndex + 1;

        return SliceInfo.builder()
                .startIndex(startIndex)
                .endIndex(endIndex)
                .sliceSamples(sliceSamples)
                .totalSamples(totalSamples)
                .sliceIndex(sliceIndex)
                .totalSlices(totalSlices)
                .allocationStrategy(strategy != null ? strategy.getCode() : AllocationStrategy.RATIO.getCode())
                .build();
    }

    /**
     * 验证切片覆盖完整性
     */
    public void validateSlicesCoverage(List<DataSliceResult> sliceResults, int totalSamples) {
        if (sliceResults == null || sliceResults.isEmpty()) {
            throw new DataSlicingService.DataSlicingException("切片结果为空，无法验证");
        }

        // 1. 使用Set验证覆盖完整性
        Set<Integer> coveredIndices = new HashSet<>();

        for (DataSliceResult result : sliceResults) {
            SliceInfo sliceInfo = result.getSliceInfo();

            // 检查SliceInfo有效性
            if (!sliceInfo.isValid()) {
                throw new DataSlicingService.DataSlicingException(
                        String.format("切片信息无效: vmId=%s, sliceInfo=%s",
                                result.getVmId(), sliceInfo)
                );
            }

            // 添加该切片的所有索引
            for (int i = sliceInfo.getStartIndex(); i <= sliceInfo.getEndIndex(); i++) {
                // 检测重叠
                if (coveredIndices.contains(i)) {
                    throw new DataSlicingService.DataSlicingException(
                            String.format("检测到索引重叠: index=%d, vmId=%s",
                                    i, result.getVmId())
                    );
                }
                coveredIndices.add(i);
            }
        }

        // 2. 验证是否覆盖所有样本
        if (coveredIndices.size() != totalSamples) {
            throw new DataSlicingService.DataSlicingException(
                    String.format("切片覆盖不完整: 预期样本数=%d, 实际覆盖=%d",
                            totalSamples, coveredIndices.size())
            );
        }

        // 3. 验证索引连续性（应覆盖[0, totalSamples-1]）
        for (int i = 0; i < totalSamples; i++) {
            if (!coveredIndices.contains(i)) {
                throw new DataSlicingService.DataSlicingException(
                        String.format("检测到索引遗漏: index=%d", i)
                );
            }
        }

        log.debug("切片覆盖验证通过: 总样本数={}, 切片数={}", totalSamples, sliceResults.size());
    }

    /**
     * 获取数据集大小
     */
    public int getDatasetSize(String datasetId) {
        if (datasetId == null || datasetId.isEmpty()) {
            throw new IllegalArgumentException("datasetId不能为空");
        }

        // 查询数据集行数
        int count = trainingDatasetRowMapper.countByDataset(datasetId);
        if (count == 0) {
            log.warn("数据集为空: datasetId={}", datasetId);
        }

        return count;
    }
}
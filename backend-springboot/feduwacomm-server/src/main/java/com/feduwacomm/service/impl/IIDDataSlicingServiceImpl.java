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
 * IID数据切分服务实现 (v1.5.1)
 * 实现独立同分布（IID）数据分配策略
 *
 * <p>IID策略特点：
 * <ul>
 *   <li>数据均匀分配：每个VM接收相近数量的样本</li>
 *   <li>顺序切分：按原始数据集的顺序连续切分</li>
 *   <li>余数处理：前N个VM多分配1个样本</li>
 * </ul>
 *
 * <p>分配算法：
 * <pre>
 * baseSize = totalSamples / vmCount
 * remainder = totalSamples % vmCount
 * VM1: [0, baseSize+0] (如果i < remainder，则+1)
 * VM2: [baseSize+1, 2*baseSize+1]
 * ...
 * </pre>
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@Slf4j
@Service("iidDataSlicingService")
public class IIDDataSlicingServiceImpl implements DataSlicingService {

    private final TrainingDatasetMapper trainingDatasetMapper;
    private final TrainingDatasetRowMapper trainingDatasetRowMapper;
    private final UuidUtil uuidUtil;

    public IIDDataSlicingServiceImpl(TrainingDatasetMapper trainingDatasetMapper,
                                      TrainingDatasetRowMapper trainingDatasetRowMapper,
                                      UuidUtil uuidUtil) {
        this.trainingDatasetMapper = trainingDatasetMapper;
        this.trainingDatasetRowMapper = trainingDatasetRowMapper;
        this.uuidUtil = uuidUtil;
    }

    @Override
    public List<DataSliceResult> sliceDataset(String datasetId, List<String> vmIds, AllocationStrategy strategy) {
        // 1. 参数验证
        if (datasetId == null || datasetId.isEmpty()) {
            throw new IllegalArgumentException("datasetId不能为空");
        }
        if (vmIds == null || vmIds.isEmpty()) {
            throw new IllegalArgumentException("vmIds列表不能为空");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("strategy不能为空");
        }
        if (!strategy.isIID()) {
            throw new IllegalArgumentException("IIDDataSlicingService仅支持IID策略");
        }

        log.info("开始IID数据切分: datasetId={}, vmCount={}, strategy={}",
                datasetId, vmIds.size(), strategy);

        // 2. 获取数据集大小
        int totalSamples = getDatasetSize(datasetId);
        if (totalSamples == 0) {
            throw new DataSlicingException("数据集为空，无法切分: datasetId=" + datasetId);
        }

        int vmCount = vmIds.size();
        if (vmCount > totalSamples) {
            log.warn("虚拟机数量({})大于样本数量({})，部分VM将不分配数据", vmCount, totalSamples);
        }

        // 3. 计算切分参数
        int baseSize = totalSamples / vmCount;  // 基础切片大小
        int remainder = totalSamples % vmCount; // 余数

        log.debug("切分参数: totalSamples={}, baseSize={}, remainder={}",
                totalSamples, baseSize, remainder);

        // 4. 为每个VM生成切片
        List<DataSliceResult> results = new ArrayList<>();
        int currentIndex = 0;

        for (int i = 0; i < vmCount; i++) {
            String vmId = vmIds.get(i);

            // 前remainder个VM多分配1个样本
            int sliceSize = baseSize + (i < remainder ? 1 : 0);

            // 处理VM数量 > 样本数量的情况
            if (currentIndex >= totalSamples) {
                log.warn("VM {} 未分配数据（样本已全部分配）", vmId);
                continue;
            }

            int startIndex = currentIndex;
            int endIndex = Math.min(currentIndex + sliceSize - 1, totalSamples - 1);
            int actualSize = endIndex - startIndex + 1;

            // 5. 生成SliceInfo
            SliceInfo sliceInfo = generateSliceInfo(
                    i + 1,          // sliceIndex (从1开始)
                    startIndex,
                    endIndex,
                    totalSamples,
                    vmCount,
                    strategy
            );

            // 6. 生成assignedDatasetId
            String assignedDatasetId = uuidUtil.generateUuid();

            // 7. 创建切片结果
            DataSliceResult result = DataSliceResult.builder()
                    .vmId(vmId)
                    .sliceInfo(sliceInfo)
                    .assignedDatasetId(assignedDatasetId)
                    .build();

            results.add(result);

            log.debug("VM {} 切片生成: [{}-{}], size={}, assignedDatasetId={}",
                    vmId, startIndex, endIndex, actualSize, assignedDatasetId);

            currentIndex += actualSize;
        }

        // 8. 验证切片完整性
        validateSlicesCoverage(results, totalSamples);

        log.info("IID数据切分完成: 总样本数={}, 切片数={}", totalSamples, results.size());

        return results;
    }

    @Override
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
                .allocationStrategy(strategy.getCode())
                .build();
    }

    @Override
    public void validateSlicesCoverage(List<DataSliceResult> sliceResults, int totalSamples) {
        if (sliceResults == null || sliceResults.isEmpty()) {
            throw new DataSlicingException("切片结果为空，无法验证");
        }

        // 1. 使用Set验证覆盖完整性
        Set<Integer> coveredIndices = new HashSet<>();

        for (DataSliceResult result : sliceResults) {
            SliceInfo sliceInfo = result.getSliceInfo();

            // 检查SliceInfo有效性
            if (!sliceInfo.isValid()) {
                throw new DataSlicingException(
                        String.format("切片信息无效: vmId=%s, sliceInfo=%s",
                                result.getVmId(), sliceInfo)
                );
            }

            // 添加该切片的所有索引
            for (int i = sliceInfo.getStartIndex(); i <= sliceInfo.getEndIndex(); i++) {
                // 检测重叠
                if (coveredIndices.contains(i)) {
                    throw new DataSlicingException(
                            String.format("检测到索引重叠: index=%d, vmId=%s",
                                    i, result.getVmId())
                    );
                }
                coveredIndices.add(i);
            }
        }

        // 2. 验证是否覆盖所有样本
        if (coveredIndices.size() != totalSamples) {
            throw new DataSlicingException(
                    String.format("切片覆盖不完整: 预期样本数=%d, 实际覆盖=%d",
                            totalSamples, coveredIndices.size())
            );
        }

        // 3. 验证索引连续性（应覆盖[0, totalSamples-1]）
        for (int i = 0; i < totalSamples; i++) {
            if (!coveredIndices.contains(i)) {
                throw new DataSlicingException(
                        String.format("检测到索引遗漏: index=%d", i)
                );
            }
        }

        log.debug("切片覆盖验证通过: 总样本数={}, 切片数={}", totalSamples, sliceResults.size());
    }

    @Override
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
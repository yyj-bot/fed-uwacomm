package com.feduwacomm.service;

import com.feduwacomm.dto.SliceInfo;
import com.feduwacomm.dto.SliceVerification;
import com.feduwacomm.dto.VerificationResult;
import com.feduwacomm.enums.VerificationDecision;

import java.util.List;

/**
 * 数据切片验证服务接口 (v1.5.1)
 * 负责验证虚拟机接收的数据切片完整性
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-09-30
 */
public interface SliceVerificationService {

    /**
     * 验证切片完整性
     * 根据VM上报的SliceVerification和预期的SliceInfo进行完整性验证
     *
     * @param vmId              虚拟机ID
     * @param verification      VM上报的验证信息
     * @param expectedSliceInfo 预期的切片信息
     * @return 验证结果
     */
    VerificationResult verifySliceVerification(String vmId,
                                                SliceVerification verification,
                                                SliceInfo expectedSliceInfo);

    /**
     * 查找缺失的索引
     * 对比已接收索引和预期索引范围，找出缺失的索引
     *
     * @param receivedIndices 已接收的索引列表
     * @param startIndex      预期起始索引
     * @param endIndex        预期结束索引
     * @return 缺失的索引列表
     */
    List<Integer> findMissingIndices(List<Integer> receivedIndices,
                                      int startIndex,
                                      int endIndex);

    /**
     * 检测数据间隙
     * 检查接收的索引序列中是否存在间隙（非连续区域）
     *
     * @param receivedIndices 已接收的索引列表
     * @return 是否存在间隙
     */
    boolean hasDataGaps(List<Integer> receivedIndices);

    /**
     * 查找所有数据间隙范围
     * 识别接收索引序列中的所有间隙区间
     *
     * @param receivedIndices 已接收的索引列表
     * @return 间隙范围列表
     */
    List<VerificationResult.GapRange> findGapRanges(List<Integer> receivedIndices);

    /**
     * 基于验证失败情况做出决策
     * 根据缺失率和验证结果推荐处理策略
     *
     * @param vmId             虚拟机ID
     * @param verificationResult 验证结果
     * @return 推荐的决策
     */
    VerificationDecision decideOnVerificationFailure(String vmId,
                                                      VerificationResult verificationResult);

    /**
     * 计算缺失率
     *
     * @param missingCount    缺失样本数
     * @param expectedSamples 预期样本数
     * @return 缺失率（0.0-1.0）
     */
    double calculateMissingRate(int missingCount, int expectedSamples);

    /**
     * 验证样本数是否正确
     *
     * @param verification      VM上报的验证信息
     * @param expectedSliceInfo 预期的切片信息
     * @return 样本数是否正确
     */
    boolean isSampleCountValid(SliceVerification verification, SliceInfo expectedSliceInfo);

    /**
     * 验证索引范围是否正确
     *
     * @param verification      VM上报的验证信息
     * @param expectedSliceInfo 预期的切片信息
     * @return 索引范围是否正确
     */
    boolean isRangeValid(SliceVerification verification, SliceInfo expectedSliceInfo);
}
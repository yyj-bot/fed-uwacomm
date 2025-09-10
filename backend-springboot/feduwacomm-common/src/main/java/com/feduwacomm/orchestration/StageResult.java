package com.feduwacomm.orchestration;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 阶段执行结果
 */
@Data
public class StageResult {

    /**
     * 执行状态
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 异常对象
     */
    private Exception exception;

    /**
     * 输出数据
     */
    private Map<String, Object> outputData;

    /**
     * 是否需要重试
     */
    private boolean shouldRetry;

    /**
     * 重试延迟(毫秒)
     */
    private long retryDelayMs;

    public StageResult() {
        this.outputData = new HashMap<>();
    }

    /**
     * 创建成功结果
     */
    public static StageResult success() {
        StageResult result = new StageResult();
        result.setSuccess(true);
        return result;
    }

    /**
     * 创建成功结果，包含输出数据
     */
    public static StageResult success(Map<String, Object> outputData) {
        StageResult result = success();
        result.setOutputData(outputData);
        return result;
    }

    /**
     * 创建成功结果，包含单个输出
     */
    public static StageResult success(String key, Object value) {
        StageResult result = success();
        result.addOutput(key, value);
        return result;
    }

    /**
     * 创建失败结果
     */
    public static StageResult failure(String errorMessage) {
        StageResult result = new StageResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    /**
     * 创建失败结果，包含异常
     */
    public static StageResult failure(String errorMessage, Exception exception) {
        StageResult result = failure(errorMessage);
        result.setException(exception);
        return result;
    }

    /**
     * 创建需要重试的失败结果
     */
    public static StageResult retryableFailure(String errorMessage, long retryDelayMs) {
        StageResult result = failure(errorMessage);
        result.setShouldRetry(true);
        result.setRetryDelayMs(retryDelayMs);
        return result;
    }

    /**
     * 添加输出数据
     */
    public StageResult addOutput(String key, Object value) {
        if (outputData == null) {
            outputData = new HashMap<>();
        }
        outputData.put(key, value);
        return this;
    }

    /**
     * 获取输出数据
     */
    public Object getOutput(String key) {
        return outputData != null ? outputData.get(key) : null;
    }

    /**
     * 检查是否包含输出
     */
    public boolean hasOutput(String key) {
        return outputData != null && outputData.containsKey(key);
    }

    /**
     * 获取错误信息，如果有异常优先返回异常信息
     */
    public String getError() {
        if (exception != null && exception.getMessage() != null) {
            return exception.getMessage();
        }
        return errorMessage;
    }
}
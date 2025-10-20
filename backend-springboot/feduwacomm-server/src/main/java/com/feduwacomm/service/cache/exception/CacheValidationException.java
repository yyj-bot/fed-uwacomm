package com.feduwacomm.service.cache.exception;

/**
 * 缓存验证异常
 * 当缓存数据无效、不完整或过期时抛出此异常
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 */
public class CacheValidationException extends RuntimeException {

    private final String cacheType;
    private final String cacheKey;
    private final String validationFailure;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public CacheValidationException(String message) {
        super(message);
        this.cacheType = null;
        this.cacheKey = null;
        this.validationFailure = null;
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     * @param cause 异常原因
     */
    public CacheValidationException(String message, Throwable cause) {
        super(message, cause);
        this.cacheType = null;
        this.cacheKey = null;
        this.validationFailure = null;
    }

    /**
     * 详细构造函数
     *
     * @param cacheType 缓存类型
     * @param cacheKey 缓存键
     * @param validationFailure 验证失败原因
     */
    public CacheValidationException(String cacheType, String cacheKey, String validationFailure) {
        super(String.format("缓存验证失败 [类型=%s, 键=%s, 原因=%s]", cacheType, cacheKey, validationFailure));
        this.cacheType = cacheType;
        this.cacheKey = cacheKey;
        this.validationFailure = validationFailure;
    }

    /**
     * 详细构造函数
     *
     * @param cacheType 缓存类型
     * @param cacheKey 缓存键
     * @param validationFailure 验证失败原因
     * @param cause 异常原因
     */
    public CacheValidationException(String cacheType, String cacheKey, String validationFailure, Throwable cause) {
        super(String.format("缓存验证失败 [类型=%s, 键=%s, 原因=%s]", cacheType, cacheKey, validationFailure), cause);
        this.cacheType = cacheType;
        this.cacheKey = cacheKey;
        this.validationFailure = validationFailure;
    }

    /**
     * 创建数据为null的异常
     *
     * @param cacheType 缓存类型
     * @param cacheKey 缓存键
     * @return 异常实例
     */
    public static CacheValidationException nullData(String cacheType, String cacheKey) {
        return new CacheValidationException(cacheType, cacheKey, "缓存数据为null");
    }

    /**
     * 创建数据不完整的异常
     *
     * @param cacheType 缓存类型
     * @param cacheKey 缓存键
     * @return 异常实例
     */
    public static CacheValidationException incompleteData(String cacheType, String cacheKey) {
        return new CacheValidationException(cacheType, cacheKey, "缓存数据不完整");
    }

    /**
     * 创建数据过期的异常
     *
     * @param cacheType 缓存类型
     * @param cacheKey 缓存键
     * @return 异常实例
     */
    public static CacheValidationException expiredData(String cacheType, String cacheKey) {
        return new CacheValidationException(cacheType, cacheKey, "缓存数据已过期");
    }

    /**
     * 创建缓存不存在的异常
     *
     * @param cacheType 缓存类型
     * @param cacheKey 缓存键
     * @return 异常实例
     */
    public static CacheValidationException notFound(String cacheType, String cacheKey) {
        return new CacheValidationException(cacheType, cacheKey, "缓存数据不存在");
    }

    // Getters

    public String getCacheType() {
        return cacheType;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public String getValidationFailure() {
        return validationFailure;
    }
}
package com.feduwacomm.exception;

/**
 * 资源不存在异常
 * 当请求的资源（用户、模型、任务等）不存在时抛出此异常
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    /**
     * 构造函数
     *
     * @param resourceType 资源类型（如：用户、模型、任务）
     * @param resourceId 资源ID
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s不存在: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     * @param cause 原因异常
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.resourceId = null;
    }

    /**
     * 获取资源类型
     *
     * @return 资源类型
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * 获取资源ID
     *
     * @return 资源ID
     */
    public String getResourceId() {
        return resourceId;
    }
}
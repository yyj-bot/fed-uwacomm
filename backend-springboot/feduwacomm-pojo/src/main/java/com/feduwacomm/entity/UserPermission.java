package com.feduwacomm.entity;

import java.time.LocalDateTime;

/**
 * 用户权限实体类
 */
public class UserPermission {

    private String id;
    private String userId;
    private String resourceType;
    private String resourceId;
    private String permission;
    private LocalDateTime grantedAt;
    private String grantedBy;
    private LocalDateTime expiresAt;

    // 构造函数
    public UserPermission() {
    }

    public UserPermission(String userId, String resourceType, String permission, String grantedBy) {
        this.userId = userId;
        this.resourceType = resourceType;
        this.permission = permission;
        this.grantedBy = grantedBy;
        this.grantedAt = LocalDateTime.now();
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return "UserPermission{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", permission='" + permission + '\'' +
                ", grantedAt=" + grantedAt +
                ", grantedBy='" + grantedBy + '\'' +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
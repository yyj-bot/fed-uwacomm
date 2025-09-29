package com.feduwacomm.entity;

import com.feduwacomm.enums.VmSecretStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * VM密钥实体类 - vm_secrets表
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmSecret {

    /**
     * 凭证唯一标识(32位UUID)
     */
    private String id;

    /**
     * 虚拟机ID(32位UUID)
     */
    private String vmId;

    /**
     * secretId 哈希(如SHA-256)
     */
    private String secretHash;

    /**
     * 哈希盐值
     */
    private String salt;

    /**
     * 状态
     */
    private VmSecretStatus status;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;

    /**
     * 最近旋转时间
     */
    private LocalDateTime rotatedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
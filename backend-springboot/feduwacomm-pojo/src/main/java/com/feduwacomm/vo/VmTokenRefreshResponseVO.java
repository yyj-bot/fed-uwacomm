package com.feduwacomm.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 虚拟机Token刷新响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmTokenRefreshResponseVO {

    private String accessToken;
    private Long tokenExpireSeconds;
    private String secretId;  // 新的刷新凭证（可选，如果启用凭证旋转）
}
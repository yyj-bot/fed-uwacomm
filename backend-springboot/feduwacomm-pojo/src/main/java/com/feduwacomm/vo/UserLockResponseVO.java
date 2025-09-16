package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户锁定响应视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLockResponseVO {

    private String userId;
    private String status;
    private LocalDateTime lockedUntil;
    private String lockReason;
    private String lockedBy;
    private LocalDateTime lockedAt;
}
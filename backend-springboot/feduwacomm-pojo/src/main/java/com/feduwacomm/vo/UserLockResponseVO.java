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
    private LocalDateTime lockedUntil;
}
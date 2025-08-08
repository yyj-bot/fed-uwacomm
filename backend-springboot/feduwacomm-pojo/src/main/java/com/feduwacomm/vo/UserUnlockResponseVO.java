package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户解锁响应视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUnlockResponseVO {

    private String userId;
    private String status;
}
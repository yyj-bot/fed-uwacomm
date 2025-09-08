package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户锁定数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLockDTO {

    private Integer duration; // 锁定时长（秒）
    private Integer lockHours; // 锁定时长（小时）
    private String lockReason; // 锁定原因
}
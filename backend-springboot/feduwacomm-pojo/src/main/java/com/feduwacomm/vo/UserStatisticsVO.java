package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户统计信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsVO {

    /**
     * 用户总数
     */
    private Long totalUsers;

    /**
     * 活跃用户数
     */
    private Long activeUsers;

    /**
     * 锁定用户数
     */
    private Long lockedUsers;

    /**
     * 管理员用户数
     */
    private Long adminUsers;

    /**
     * 研究人员用户数
     */
    private Long researcherUsers;

    /**
     * 操作员用户数
     */
    private Long operatorUsers;

    /**
     * 查看者用户数
     */
    private Long viewerUsers;

    /**
     * 今日新增用户数
     */
    private Long todayNewUsers;

    /**
     * 统计时间戳
     */
    private Long timestamp;

    /**
     * 是否有用户数据
     */
    private Boolean hasUsers;
}
package com.feduwacomm.service;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.*;
import com.feduwacomm.vo.*;

/**
 * 虚拟机实例服务接口
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public interface VmInstanceService {

    /**
     * 注册虚拟机实例
     *
     * @param registerDTO 注册请求DTO
     * @return 注册响应VO
     */
    VmRegisterResponseVO register(VmRegisterDTO registerDTO);

    /**
     * 刷新访问令牌
     *
     * @param refreshDTO 刷新请求DTO
     * @return 刷新响应VO
     */
    VmTokenRefreshResponseVO refreshToken(VmTokenRefreshDTO refreshDTO);

    /**
     * 验证访问令牌
     *
     * @param accessToken 访问令牌
     * @return 虚拟机ID，如果令牌无效返回null
     */
    String validateAccessToken(String accessToken);

    /**
     * 更新虚拟机连接状态
     *
     * @param vmId 虚拟机ID
     * @param connectionStatus 连接状态
     * @param wsSessionId WebSocket会话ID
     */
    void updateConnectionStatus(String vmId, String connectionStatus, String wsSessionId);

    /**
     * 更新虚拟机心跳时间
     *
     * @param vmId 虚拟机ID
     */
    void updateHeartbeat(String vmId);

    /**
     * 断开虚拟机连接
     * 将连接状态更新为DISCONNECTED，清除WebSocket会话ID
     *
     * @param vmId 虚拟机ID
     */
    void disconnectVm(String vmId);

    /**
     * 断开虚拟机连接
     *
     * @param vmId 虚拟机ID
     */
    void disconnect(String vmId);

    // ==================== 用户端CRUD接口 ====================

    /**
     * 分页查询虚拟机列表
     *
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    PageResult<VmListVO> queryVmList(VmQueryDTO queryDTO);

    /**
     * 获取虚拟机详情
     *
     * @param vmId 虚拟机ID
     * @param userId 用户ID（用于权限控制）
     * @return 虚拟机详情
     */
    VmDetailVO getVmDetail(String vmId, String userId);

    /**
     * 更新虚拟机信息
     *
     * @param vmId 虚拟机ID
     * @param updateDTO 更新数据
     * @param userId 用户ID（用于权限控制）
     * @return 更新响应
     */
    VmUpdateResponseVO updateVm(String vmId, VmUpdateDTO updateDTO, String userId);

    /**
     * 删除虚拟机
     *
     * @param vmId 虚拟机ID
     * @param force 是否强制删除
     * @param userId 用户ID（用于权限控制）
     * @return 删除响应
     */
    VmDeleteResponseVO deleteVm(String vmId, Boolean force, String userId);

    // ==================== 虚拟机控制接口 ====================

    /**
     * 启动虚拟机
     *
     * @param vmId 虚拟机ID
     * @param controlDTO 控制参数
     * @param userId 用户ID
     * @return 控制响应
     */
    VmControlResponseVO startVm(String vmId, VmControlDTO controlDTO, String userId);

    /**
     * 停止虚拟机
     *
     * @param vmId 虚拟机ID
     * @param controlDTO 控制参数
     * @param userId 用户ID
     * @return 控制响应
     */
    VmControlResponseVO stopVm(String vmId, VmControlDTO controlDTO, String userId);

    /**
     * 重启虚拟机
     *
     * @param vmId 虚拟机ID
     * @param controlDTO 控制参数
     * @param userId 用户ID
     * @return 控制响应
     */
    VmControlResponseVO restartVm(String vmId, VmControlDTO controlDTO, String userId);

    // ==================== 状态查询接口 ====================

    /**
     * 获取虚拟机实时状态
     *
     * @param vmId 虚拟机ID
     * @param userId 用户ID
     * @return 虚拟机状态
     */
    VmStatusVO getVmStatus(String vmId, String userId);

    /**
     * 检查用户是否有权限访问虚拟机
     *
     * @param vmId 虚拟机ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    Boolean hasVmPermission(String vmId, String userId);
}
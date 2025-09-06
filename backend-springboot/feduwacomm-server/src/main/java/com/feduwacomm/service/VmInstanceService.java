package com.feduwacomm.service;

import com.feduwacomm.dto.VmRegisterDTO;
import com.feduwacomm.dto.VmTokenRefreshDTO;
import com.feduwacomm.vo.VmRegisterResponseVO;
import com.feduwacomm.vo.VmTokenRefreshResponseVO;

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
     *
     * @param vmId 虚拟机ID
     */
    void disconnect(String vmId);
}
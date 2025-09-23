package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import com.feduwacomm.dto.VmRegisterDTO;
import com.feduwacomm.dto.VmTokenRefreshDTO;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.vo.VmRegisterResponseVO;
import com.feduwacomm.vo.VmTokenRefreshResponseVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 虚拟机实例控制器
 * 提供虚拟机自身操作：注册、Token刷新、心跳、健康检查
 * 注意：用户管理虚拟机的CRUD操作已迁移到VmManagementController
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/vm")
@CrossOrigin(origins = "*")
public class VmInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(VmInstanceController.class);
    private static final Logger accessLog = LoggerFactory.getLogger("ACCESS_LOG");

    @Autowired
    private VmInstanceService vmInstanceService;

    /**
     * 虚拟机注册接口
     * 虚拟机端调用，无需JWT认证
     *
     * @param registerDTO 注册请求DTO
     * @param request HTTP请求对象
     * @return 注册响应
     */
    @PostMapping("/register")
    public Result<VmRegisterResponseVO> register(@RequestBody VmRegisterDTO registerDTO,
                                                HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        logger.info("收到虚拟机注册请求: vmId={}, name={}, ip={}, clientIp={}", 
                   registerDTO.getVmId(), registerDTO.getName(), 
                   registerDTO.getIpAddress(), clientIp);
        accessLog.info("虚拟机注册请求: vmId={}, name={}, ip={}, clientIp={}, userAgent={}", 
                      registerDTO.getVmId(), registerDTO.getName(), 
                      registerDTO.getIpAddress(), clientIp, userAgent);

        try {
            VmRegisterResponseVO response = vmInstanceService.register(registerDTO);
            logger.info("虚拟机注册成功: vmId={}, sessionId={}, clientIp={}", 
                       registerDTO.getVmId(), response.getSessionId(), clientIp);
            return Result.success("虚拟机注册成功", response);
            
        } catch (Exception e) {
            logger.error("虚拟机注册失败: vmId={}, clientIp={}", 
                        registerDTO.getVmId(), clientIp, e);
            throw e;
        }
    }

    /**
     * Token刷新接口
     * 虚拟机端调用，使用secretId认证，无需JWT Token
     *
     * @param refreshDTO 刷新请求DTO
     * @param request HTTP请求对象
     * @return 刷新响应
     */
    @PostMapping("/token/refresh")
    public Result<VmTokenRefreshResponseVO> refreshToken(@Valid @RequestBody VmTokenRefreshDTO refreshDTO,
                                                        HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        logger.info("收到Token刷新请求: vmId={}, clientIp={}", refreshDTO.getVmId(), clientIp);
        accessLog.info("Token刷新请求: vmId={}, clientIp={}, userAgent={}", 
                      refreshDTO.getVmId(), clientIp, userAgent);

        try {
            VmTokenRefreshResponseVO response = vmInstanceService.refreshToken(refreshDTO);
            logger.info("Token刷新成功: vmId={}, clientIp={}", refreshDTO.getVmId(), clientIp);
            return Result.success("刷新成功", response);
            
        } catch (Exception e) {
            logger.error("Token刷新失败: vmId={}, clientIp={}", refreshDTO.getVmId(), clientIp, e);
            throw e;
        }
    }

    /**
     * 心跳接口
     * 虚拟机端调用，用于保持连接活跃
     * 无需JWT认证，通过vmId参数标识
     *
     * @param vmId 虚拟机ID
     * @param request HTTP请求对象
     * @return 心跳响应
     */
    @PostMapping("/{vmId}/heartbeat")
    public Result<String> heartbeat(@PathVariable String vmId, HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        
        logger.debug("收到心跳请求: vmId={}, clientIp={}", vmId, clientIp);

        try {
            vmInstanceService.updateHeartbeat(vmId);
            return Result.success("心跳更新成功", null);
            
        } catch (Exception e) {
            logger.error("心跳更新失败: vmId={}, clientIp={}", vmId, clientIp, e);
            throw e;
        }
    }

    /**
     * 健康检查接口
     * 供虚拟机端调用，检查服务可用性
     *
     * @return 健康检查响应
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("虚拟机服务运行正常", "VM_SERVICE_UP");
    }

    // 注意：用户管理虚拟机的CRUD和控制操作已迁移到VmManagementController (/api/vm/**)
}
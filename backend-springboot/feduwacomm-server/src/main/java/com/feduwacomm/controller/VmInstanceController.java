package com.feduwacomm.controller;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.common.Result;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.utils.UserJwtUtil;
import com.feduwacomm.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 虚拟机实例控制器
 * 提供虚拟机注册、Token刷新等功能
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

    @Autowired
    private UserJwtUtil userJwtUtil;

    /**
     * 虚拟机注册接口
     * 虚拟机端调用，无需JWT认证
     *
     * @param registerDTO 注册请求DTO
     * @param request HTTP请求对象
     * @return 注册响应
     */
    @PostMapping("/register")
    public Result<VmRegisterResponseVO> register(@Valid @RequestBody VmRegisterDTO registerDTO,
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

    // ==================== 用户端CRUD接口 ====================

    /**
     * 虚拟机列表查询接口
     * 获取系统中所有虚拟机的列表信息，支持分页和过滤
     *
     * @param page 页码，默认1
     * @param size 每页大小，默认20，最大100
     * @param status 状态过滤
     * @param osType 操作系统类型过滤
     * @param keyword 关键词搜索
     * @param connectionStatus 连接状态过滤
     * @param sortField 排序字段
     * @param sortOrder 排序方向
     * @param request HTTP请求对象
     * @return 分页查询结果
     */
    @GetMapping("/list")
    public Result<PageResult<VmListVO>> queryVmList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String osType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String connectionStatus,
            @RequestParam(defaultValue = "created_at") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIpAddress(request);
        String token = request.getHeader("Authorization");
        String userId = userJwtUtil.getUserIdFromToken(token);

        logger.info("收到虚拟机列表查询请求: page={}, size={}, status={}, userId={}, clientIp={}", 
                   page, size, status, userId, clientIp);

        try {
            VmQueryDTO queryDTO = VmQueryDTO.builder()
                    .page(page)
                    .size(size)
                    .status(status)
                    .osType(osType)
                    .keyword(keyword)
                    .connectionStatus(connectionStatus)
                    .sortField(sortField)
                    .sortOrder(sortOrder)
                    .userId(userId)
                    .build();

            PageResult<VmListVO> result = vmInstanceService.queryVmList(queryDTO);
            return Result.success("查询成功", result);

        } catch (Exception e) {
            logger.error("虚拟机列表查询失败: userId={}, clientIp={}", userId, clientIp, e);
            throw e;
        }
    }

    /**
     * 虚拟机详情查询接口
     * 获取指定虚拟机的详细信息
     *
     * @param vmId 虚拟机ID
     * @param request HTTP请求对象
     * @return 虚拟机详情
     */
    @GetMapping("/{vmId}")
    public Result<VmDetailVO> getVmDetail(@PathVariable String vmId, HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String token = request.getHeader("Authorization");
        String userId = userJwtUtil.getUserIdFromToken(token);

        logger.info("收到虚拟机详情查询请求: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);
        accessLog.info("虚拟机详情查询: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);

        try {
            VmDetailVO result = vmInstanceService.getVmDetail(vmId, userId);
            return Result.success("查询成功", result);

        } catch (Exception e) {
            logger.error("虚拟机详情查询失败: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp, e);
            throw e;
        }
    }

    /**
     * 虚拟机更新接口
     * 更新虚拟机的配置信息
     *
     * @param vmId 虚拟机ID
     * @param updateDTO 更新数据
     * @param request HTTP请求对象
     * @return 更新响应
     */
    @PutMapping("/{vmId}")
    public Result<VmUpdateResponseVO> updateVm(@PathVariable String vmId,
                                              @Valid @RequestBody VmUpdateDTO updateDTO,
                                              HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String token = request.getHeader("Authorization");
        String userId = userJwtUtil.getUserIdFromToken(token);

        logger.info("收到虚拟机更新请求: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);
        accessLog.info("虚拟机更新: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);

        try {
            VmUpdateResponseVO result = vmInstanceService.updateVm(vmId, updateDTO, userId);
            return Result.success("虚拟机更新成功", result);

        } catch (Exception e) {
            logger.error("虚拟机更新失败: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp, e);
            throw e;
        }
    }

    /**
     * 虚拟机删除接口
     * 从系统中删除指定的虚拟机
     *
     * @param vmId 虚拟机ID
     * @param force 是否强制删除，默认false
     * @param request HTTP请求对象
     * @return 删除响应
     */
    @DeleteMapping("/{vmId}")
    public Result<VmDeleteResponseVO> deleteVm(@PathVariable String vmId,
                                              @RequestParam(defaultValue = "false") Boolean force,
                                              HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String token = request.getHeader("Authorization");
        String userId = userJwtUtil.getUserIdFromToken(token);

        logger.info("收到虚拟机删除请求: vmId={}, force={}, userId={}, clientIp={}", vmId, force, userId, clientIp);
        accessLog.info("虚拟机删除: vmId={}, force={}, userId={}, clientIp={}", vmId, force, userId, clientIp);

        try {
            VmDeleteResponseVO result = vmInstanceService.deleteVm(vmId, force, userId);
            return Result.success("虚拟机删除成功", result);

        } catch (Exception e) {
            logger.error("虚拟机删除失败: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp, e);
            throw e;
        }
    }

    // ==================== 虚拟机控制接口 ====================

    /**
     * 虚拟机启动接口
     * 启动指定的虚拟机
     *
     * @param vmId 虚拟机ID
     * @param controlDTO 控制参数
     * @param request HTTP请求对象
     * @return 控制响应
     */
    @PostMapping("/{vmId}/start")
    public Result<VmControlResponseVO> startVm(@PathVariable String vmId,
                                              @RequestBody(required = false) VmControlDTO controlDTO,
                                              HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String token = request.getHeader("Authorization");
        String userId = userJwtUtil.getUserIdFromToken(token);

        if (controlDTO == null) {
            controlDTO = VmControlDTO.builder()
                    .operation("start")
                    .timeout(300)
                    .build();
        }

        logger.info("收到虚拟机启动请求: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);
        accessLog.info("虚拟机启动: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);

        try {
            VmControlResponseVO result = vmInstanceService.startVm(vmId, controlDTO, userId);
            return Result.success("虚拟机启动命令已发送", result);

        } catch (Exception e) {
            logger.error("虚拟机启动失败: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp, e);
            throw e;
        }
    }

    /**
     * 虚拟机停止接口
     * 停止指定的虚拟机
     *
     * @param vmId 虚拟机ID
     * @param controlDTO 控制参数
     * @param request HTTP请求对象
     * @return 控制响应
     */
    @PostMapping("/{vmId}/stop")
    public Result<VmControlResponseVO> stopVm(@PathVariable String vmId,
                                             @RequestBody(required = false) VmControlDTO controlDTO,
                                             HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String token = request.getHeader("Authorization");
        String userId = userJwtUtil.getUserIdFromToken(token);

        if (controlDTO == null) {
            controlDTO = VmControlDTO.builder()
                    .operation("stop")
                    .timeout(60)
                    .graceful(true)
                    .saveState(true)
                    .build();
        }

        logger.info("收到虚拟机停止请求: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);
        accessLog.info("虚拟机停止: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);

        try {
            VmControlResponseVO result = vmInstanceService.stopVm(vmId, controlDTO, userId);
            return Result.success("虚拟机停止命令已发送", result);

        } catch (Exception e) {
            logger.error("虚拟机停止失败: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp, e);
            throw e;
        }
    }

    /**
     * 虚拟机重启接口
     * 重启指定的虚拟机
     *
     * @param vmId 虚拟机ID
     * @param controlDTO 控制参数
     * @param request HTTP请求对象
     * @return 控制响应
     */
    @PostMapping("/{vmId}/restart")
    public Result<VmControlResponseVO> restartVm(@PathVariable String vmId,
                                                @RequestBody(required = false) VmControlDTO controlDTO,
                                                HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String token = request.getHeader("Authorization");
        String userId = userJwtUtil.getUserIdFromToken(token);

        if (controlDTO == null) {
            controlDTO = VmControlDTO.builder()
                    .operation("restart")
                    .timeout(300)
                    .graceful(true)
                    .build();
        }

        logger.info("收到虚拟机重启请求: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);
        accessLog.info("虚拟机重启: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);

        try {
            VmControlResponseVO result = vmInstanceService.restartVm(vmId, controlDTO, userId);
            return Result.success("虚拟机重启命令已发送", result);

        } catch (Exception e) {
            logger.error("虚拟机重启失败: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp, e);
            throw e;
        }
    }

    // ==================== 状态查询接口 ====================

    /**
     * 虚拟机状态查询接口
     * 获取指定虚拟机的实时状态信息
     *
     * @param vmId 虚拟机ID
     * @param request HTTP请求对象
     * @return 虚拟机状态
     */
    @GetMapping("/{vmId}/status")
    public Result<VmStatusVO> getVmStatus(@PathVariable String vmId, HttpServletRequest request) {
        String clientIp = IpUtil.getClientIpAddress(request);
        String token = request.getHeader("Authorization");
        String userId = userJwtUtil.getUserIdFromToken(token);

        logger.info("收到虚拟机状态查询请求: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp);

        try {
            VmStatusVO result = vmInstanceService.getVmStatus(vmId, userId);
            return Result.success("查询成功", result);

        } catch (Exception e) {
            logger.error("虚拟机状态查询失败: vmId={}, userId={}, clientIp={}", vmId, userId, clientIp, e);
            throw e;
        }
    }
}
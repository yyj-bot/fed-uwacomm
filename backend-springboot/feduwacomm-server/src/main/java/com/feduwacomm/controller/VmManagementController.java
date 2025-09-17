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
 * 虚拟机管理控制器
 * 提供用户管理虚拟机的CRUD和控制功能
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/vm")
@CrossOrigin(origins = "*")
public class VmManagementController {

    private static final Logger logger = LoggerFactory.getLogger(VmManagementController.class);
    private static final Logger accessLog = LoggerFactory.getLogger("ACCESS_LOG");

    @Autowired
    private VmInstanceService vmInstanceService;

    @Autowired
    private UserJwtUtil userJwtUtil;

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
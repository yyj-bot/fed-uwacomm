package com.feduwacomm.service;

import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.mapper.VmInstancesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * VM连接监控服务
 * 负责定时检查VM连接状态，处理超时断线的情况
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Service
public class VmConnectionMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(VmConnectionMonitorService.class);

    /**
     * 心跳超时阈值（秒）
     * 超过90秒没有心跳的VM将被标记为离线
     */
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 90;

    /**
     * 连接检查间隔（毫秒）
     * 每30秒检查一次
     */
    private static final long CHECK_INTERVAL_MS = 30000;

    @Autowired
    private VmInstancesMapper vmInstancesMapper;

    /**
     * 定时检查VM连接状态
     * 每30秒执行一次，检查是否有VM心跳超时
     */
    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void checkVmConnectionStatus() {
        try {
            logger.debug("开始检查VM连接状态...");

            // 查询所有标记为CONNECTED的VM
            List<VmInstance> connectedVms = vmInstancesMapper.selectByConnectionStatus("CONNECTED");

            if (connectedVms.isEmpty()) {
                logger.debug("当前没有已连接的VM需要检查");
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            int timeoutCount = 0;

            for (VmInstance vm : connectedVms) {
                if (isVmHeartbeatTimeout(vm, now)) {
                    markVmAsDisconnected(vm);
                    timeoutCount++;
                }
            }

            if (timeoutCount > 0) {
                logger.info("检查完成，发现{}个VM连接超时，已标记为离线", timeoutCount);
            } else {
                logger.debug("检查完成，所有VM连接正常");
            }

        } catch (Exception e) {
            logger.error("VM连接状态检查过程中发生异常", e);
        }
    }

    /**
     * 检查VM心跳是否超时
     *
     * @param vm  虚拟机实例
     * @param now 当前时间
     * @return true如果心跳超时，false否则
     */
    private boolean isVmHeartbeatTimeout(VmInstance vm, LocalDateTime now) {
        LocalDateTime lastHeartbeat = vm.getLastHeartbeat();

        // 如果没有心跳记录，视为超时
        if (lastHeartbeat == null) {
            logger.warn("VM心跳记录为空，视为超时 - VmId: {}, Name: {}", vm.getId(), vm.getName());
            return true;
        }

        try {
            long secondsSinceLastHeartbeat = ChronoUnit.SECONDS.between(lastHeartbeat, now);

            if (secondsSinceLastHeartbeat > HEARTBEAT_TIMEOUT_SECONDS) {
                logger.warn("VM心跳超时 - VmId: {}, Name: {}, 上次心跳: {}, 超时时长: {}秒",
                           vm.getId(), vm.getName(), lastHeartbeat, secondsSinceLastHeartbeat);
                return true;
            }

            logger.debug("VM心跳正常 - VmId: {}, Name: {}, 上次心跳: {}, 距今: {}秒",
                        vm.getId(), vm.getName(), lastHeartbeat, secondsSinceLastHeartbeat);
            return false;

        } catch (Exception e) {
            logger.error("计算VM心跳时间失败，视为超时 - VmId: {}, Name: {}, LastHeartbeat: {}",
                        vm.getId(), vm.getName(), lastHeartbeat, e);
            return true;
        }
    }


    /**
     * 将VM标记为断开连接
     *
     * @param vm 需要标记的VM实例
     */
    private void markVmAsDisconnected(VmInstance vm) {
        try {
            int result = vmInstancesMapper.updateWebSocketSession(vm.getId(), null, "DISCONNECTED");

            if (result > 0) {
                logger.info("VM已标记为离线 - VmId: {}, Name: {}, IP: {}",
                           vm.getId(), vm.getName(), vm.getIpAddress());
            } else {
                logger.warn("更新VM离线状态失败 - VmId: {}", vm.getId());
            }

        } catch (Exception e) {
            logger.error("标记VM为离线时发生异常 - VmId: {}", vm.getId(), e);
        }
    }

    /**
     * 获取当前已连接的VM数量
     *
     * @return 已连接的VM数量
     */
    public long getConnectedVmCount() {
        try {
            List<VmInstance> connectedVms = vmInstancesMapper.selectByConnectionStatus("CONNECTED");
            return connectedVms.size();
        } catch (Exception e) {
            logger.error("获取已连接VM数量时发生异常", e);
            return 0;
        }
    }

    /**
     * 强制检查所有VM连接状态
     * 可以通过API手动触发检查
     */
    public void forceCheckAllVmConnections() {
        logger.info("手动触发VM连接状态检查");
        checkVmConnectionStatus();
    }
}
package com.feduwacomm.mapper;

import com.feduwacomm.entity.VmInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 虚拟机实例数据访问层
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Mapper
public interface VmInstancesMapper {

    /**
     * 插入新的虚拟机实例
     *
     * @param vmInstance 虚拟机实例对象
     * @return 插入行数
     */
    int insert(VmInstance vmInstance);

    /**
     * 根据id查询虚拟机实例
     *
     * @param id 虚拟机唯一标识
     * @return 虚拟机实例对象
     */
    VmInstance selectByVmId(@Param("id") String id);


    /**
     * 根据secretId查询虚拟机实例
     *
     * @param secretId 刷新凭证
     * @return 虚拟机实例对象
     */
    VmInstance selectBySecretId(@Param("secretId") String secretId);

    /**
     * 更新虚拟机实例
     *
     * @param vmInstance 虚拟机实例对象
     * @return 更新行数
     */
    int update(VmInstance vmInstance);

    /**
     * 更新连接状态
     *
     * @param vmId 虚拟机ID
     * @param connectionStatus 连接状态
     * @param lastHeartbeat 最后心跳时间
     * @return 更新行数
     */
    int updateConnection(@Param("id") String id,
                         @Param("connectionStatus") String connectionStatus,
                         @Param("lastHeartbeat") String lastHeartbeat);

    /**
     * 更新WebSocket会话信息
     *
     * @param vmId 虚拟机ID
     * @param wsSessionId WebSocket会话ID
     * @param connectionStatus 连接状态
     * @return 更新行数
     */
    int updateWebSocketSession(@Param("id") String id,
                              @Param("wsSessionId") String wsSessionId,
                              @Param("connectionStatus") String connectionStatus);


    /**
     * 更新刷新凭证
     *
     * @param vmId 虚拟机ID
     * @param secretId 新的刷新凭证
     * @param secretExpireTime 凭证过期时间
     * @return 更新行数
     */
    int updateSecretId(@Param("id") String id,
                      @Param("secretId") String secretId,
                      @Param("secretExpireTime") String secretExpireTime);

    /**
     * 删除虚拟机实例
     *
     * @param vmId 虚拟机ID
     * @return 删除行数
     */
    int deleteByVmId(@Param("id") String id);

    /**
     * 查询所有虚拟机实例
     *
     * @return 虚拟机实例列表
     */
    List<VmInstance> selectAll();

    /**
     * 根据状态查询虚拟机实例
     *
     * @param status 虚拟机状态
     * @return 虚拟机实例列表
     */
    List<VmInstance> selectByStatus(@Param("status") String status);

    /**
     * 根据连接状态查询虚拟机实例
     *
     * @param connectionStatus 连接状态
     * @return 虚拟机实例列表
     */
    List<VmInstance> selectByConnectionStatus(@Param("connectionStatus") String connectionStatus);

    /**
     * 检查虚拟机ID是否存在
     *
     * @param vmId 虚拟机ID
     * @return 存在返回1，不存在返回0
     */
    int existsByVmId(@Param("id") String id);

    // ==================== CRUD扩展方法 ====================

    /**
     * 分页查询虚拟机列表
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @param status 状态过滤
     * @param osType 操作系统类型过滤
     * @param keyword 关键词搜索
     * @param connectionStatus 连接状态过滤
     * @param sortField 排序字段
     * @param sortOrder 排序方向
     * @return 虚拟机实例列表
     */
    List<VmInstance> selectPagedList(@Param("offset") Integer offset,
                                    @Param("limit") Integer limit,
                                    @Param("status") String status,
                                    @Param("osType") String osType,
                                    @Param("keyword") String keyword,
                                    @Param("connectionStatus") String connectionStatus,
                                    @Param("sortField") String sortField,
                                    @Param("sortOrder") String sortOrder);

    /**
     * 查询虚拟机总数（用于分页）
     *
     * @param status 状态过滤
     * @param osType 操作系统类型过滤
     * @param keyword 关键词搜索
     * @param connectionStatus 连接状态过滤
     * @return 总数
     */
    int countVmInstances(@Param("status") String status,
                        @Param("osType") String osType,
                        @Param("keyword") String keyword,
                        @Param("connectionStatus") String connectionStatus);

    /**
     * 根据ID查询虚拟机详情（包含所有字段）
     *
     * @param vmId 虚拟机ID
     * @return 虚拟机详情
     */
    VmInstance selectDetailById(@Param("id") String id);

    /**
     * 更新虚拟机基本信息
     *
     * @param vmInstance 虚拟机实例
     * @return 更新行数
     */
    int updateBasicInfo(VmInstance vmInstance);

    /**
     * 更新虚拟机状态
     *
     * @param vmId 虚拟机ID
     * @param status 新状态
     * @return 更新行数
     */
    int updateStatus(@Param("id") String id, @Param("status") String status);

    /**
     * 更新虚拟机系统信息
     *
     * @param vmId 虚拟机ID
     * @param systemInfo 系统信息JSON字符串
     * @return 更新行数
     */
    int updateSystemInfo(@Param("id") String id, @Param("systemInfo") String systemInfo);

    /**
     * 更新虚拟机能力信息
     *
     * @param vmId 虚拟机ID
     * @param capabilities 能力信息JSON字符串
     * @return 更新行数
     */
    int updateCapabilities(@Param("id") String id, @Param("capabilities") String capabilities);

    /**
     * 批量删除虚拟机
     *
     * @param vmIds 虚拟机ID列表
     * @return 删除行数
     */
    int batchDeleteByIds(@Param("ids") List<String> ids);

    /**
     * 查询用户有权限的虚拟机ID列表（占位方法，具体权限控制逻辑后续实现）
     *
     * @param userId 用户ID
     * @return 虚拟机ID列表
     */
    List<String> selectVmIdsByUserId(@Param("userId") String userId);
} 
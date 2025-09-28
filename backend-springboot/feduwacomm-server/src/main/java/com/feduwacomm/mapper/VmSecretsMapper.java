package com.feduwacomm.mapper;

import com.feduwacomm.entity.VmSecret;
import com.feduwacomm.enums.VmSecretStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * VM密钥Mapper接口
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Mapper
public interface VmSecretsMapper {

    /**
     * 插入VM密钥记录
     *
     * @param vmSecret VM密钥实体
     * @return 影响行数
     */
    int insert(VmSecret vmSecret);

    /**
     * 根据ID查询VM密钥
     *
     * @param id 密钥ID
     * @return VM密钥实体
     */
    VmSecret selectById(@Param("id") String id);

    /**
     * 根据VM ID查询活跃的密钥
     *
     * @param vmId VM ID
     * @return VM密钥实体
     */
    VmSecret selectActiveByVmId(@Param("vmId") String vmId);

    /**
     * 根据VM ID查询所有密钥
     *
     * @param vmId VM ID
     * @return VM密钥列表
     */
    List<VmSecret> selectByVmId(@Param("vmId") String vmId);

    /**
     * 更新密钥状态
     *
     * @param id 密钥ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") String id, @Param("status") VmSecretStatus status);

    /**
     * 更新最后使用时间
     *
     * @param id 密钥ID
     * @return 影响行数
     */
    int updateLastUsedTime(@Param("id") String id);

    /**
     * 撤销VM的所有活跃密钥
     *
     * @param vmId VM ID
     * @return 影响行数
     */
    int revokeAllActiveByVmId(@Param("vmId") String vmId);

    /**
     * 检查VM是否存在活跃密钥
     *
     * @param vmId VM ID
     * @return 活跃密钥数量
     */
    int countActiveByVmId(@Param("vmId") String vmId);
}
package com.feduwacomm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 虚拟机Token刷新请求DTO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
public class VmTokenRefreshDTO {

    @NotBlank(message = "虚拟机ID不能为空")
    @Size(min = 32, max = 32, message = "虚拟机ID必须为32位UUID格式")
    @Pattern(regexp = "^[a-f0-9]{32}$", message = "虚拟机ID格式不正确，应为32位UUID格式")
    private String vmId;

    @NotBlank(message = "刷新凭证不能为空")
    private String secretId;
}
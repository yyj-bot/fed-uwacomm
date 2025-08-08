package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证错误视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorVO {

    private String field;
    private String error;
}
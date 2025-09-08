package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> {

    private Long total;
    private Long pages;
    private Integer current;
    private Integer page;
    private Integer size;
    private List<T> list;
    private List<T> records;
}
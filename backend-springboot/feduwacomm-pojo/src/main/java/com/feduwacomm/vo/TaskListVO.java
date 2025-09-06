package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 联邦学习任务列表响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskListVO {

    private Integer total;
    private Integer page;
    private Integer size;
    private List<TaskVO> tasks;
}
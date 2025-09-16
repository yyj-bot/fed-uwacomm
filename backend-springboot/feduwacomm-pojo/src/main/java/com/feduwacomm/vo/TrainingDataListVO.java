package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 训练数据列表响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataListVO {

    private Long total;

    private Integer page;

    private Integer size;

    private List<TrainingDataItemVO> dataList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrainingDataItemVO {
        private String datasetId;
        private String datasetDescription;
        private String datasetType;
        private String status;
        private List<String> tags;
    }
}
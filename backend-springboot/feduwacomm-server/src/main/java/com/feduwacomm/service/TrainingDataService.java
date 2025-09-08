package com.feduwacomm.service;

import com.feduwacomm.dto.*;
import com.feduwacomm.vo.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 训练数据服务接口
 * 提供训练数据管理功能
 */
public interface TrainingDataService {

    /**
     * 文件上传
     */
    TrainingDataUploadVO uploadFile(TrainingDataUploadDTO uploadDTO, MultipartFile file, String userId);

    /**
     * 文本信息上传
     */
    TrainingDataUploadVO uploadText(TrainingDataTextDTO textDTO, String userId);

    /**
     * 数据列表查询
     */
    TrainingDataListVO queryDataList(TrainingDataQueryDTO queryDTO);

    /**
     * 数据详情查询
     */
    TrainingDataVO getDataDetail(String datasetId);

    /**
     * 数据下载
     */
    byte[] downloadData(String datasetId);

    /**
     * 数据预处理
     */
    TrainingDataPreprocessVO preprocessData(String datasetId, TrainingDataPreprocessDTO preprocessDTO);

    /**
     * 数据验证
     */
    TrainingDataValidateVO validateData(String datasetId, TrainingDataValidateDTO validateDTO);

    /**
     * 数据更新
     */
    TrainingDataUpdateVO updateData(String datasetId, TrainingDataUpdateDTO updateDTO, String userId);

    /**
     * 数据删除
     */
    TrainingDataDeleteVO deleteData(String datasetId, TrainingDataDeleteDTO deleteDTO, String userId);

    /**
     * 批量数据操作
     */
    TrainingDataBatchVO batchOperation(TrainingDataBatchDTO batchDTO);

    /**
     * 数据统计
     */
    TrainingDataStatisticsVO getStatistics(TrainingDataStatisticsDTO statisticsDTO);

    /**
     * 数据导出
     */
    TrainingDataExportVO exportData(TrainingDataExportDTO exportDTO);
}
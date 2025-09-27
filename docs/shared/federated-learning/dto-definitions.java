/**
 * 联邦学习相关数据传输对象定义
 *
 * 本文件定义了联邦学习系统中使用的所有数据传输对象(DTO)，
 * 包括客户端更新、全局模型、聚合结果等核心数据结构。
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-25
 */
package com.feduwacomm.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 客户端更新数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientUpdateDTO {

    @NotBlank(message = "客户端ID不能为空")
    private String clientId;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @Min(value = 1, message = "轮次必须大于0")
    private Integer round;

    @NotNull(message = "模型参数不能为空")
    @Valid
    private ModelParametersDTO modelParameters;

    @Valid
    private ParameterDeltasDTO parameterDeltas;

    @NotNull(message = "训练元数据不能为空")
    @Valid
    private TrainingMetadataDTO trainingMetadata;

    @NotBlank(message = "时间戳不能为空")
    private String timestamp;
}

/**
 * 模型参数数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelParametersDTO {

    @NotEmpty(message = "特征重要性不能为空")
    @Size(min = 1, message = "至少需要一个特征")
    private List<@DecimalMin(value = "0.0", message = "特征重要性不能为负数") Double> featureImportances;

    @Min(value = 1, message = "树的数量必须大于0")
    private Integer nEstimators;

    // 可选的额外参数，用于支持其他模型类型
    private List<Double> coef;
    private Double intercept;
    private Integer nFeatures;
    private Integer nClasses;
    private List<String> classes;
}

/**
 * 参数变化量数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterDeltasDTO {

    private List<Double> featureImportancesDelta;
    private List<Double> importanceWeights;
    private Double interceptDelta;
    private List<Double> coefDelta;
}

/**
 * 训练元数据数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingMetadataDTO {

    @Min(value = 1, message = "样本数量必须大于0")
    private Long samplesCount;

    @DecimalMin(value = "0.0", message = "训练时间不能为负数")
    private Double trainingTime;

    @DecimalMin(value = "0.0", message = "准确率不能为负数")
    @DecimalMax(value = "1.0", message = "准确率不能大于1.0")
    private Double localAccuracy;

    @NotBlank(message = "收敛状态不能为空")
    private String convergenceStatus;

    private String algorithm;
    private String framework;
    private Integer localEpochs;
    private Double learningRate;
    private Long trainingDataSize;
    private Double validationAccuracy;
}

/**
 * 全局模型数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalModelDTO {

    @NotBlank(message = "模型ID不能为空")
    private String modelId;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @Min(value = 1, message = "轮次必须大于0")
    private Integer round;

    @NotNull(message = "模型参数不能为空")
    @Valid
    private ModelParametersDTO parameters;

    @NotBlank(message = "聚合方法不能为空")
    private String aggregationMethod;

    @Min(value = 1, message = "参与者数量必须大于0")
    private Integer participants;

    @NotNull(message = "创建时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private String version;
    private String description;
    private Map<String, Object> metadata;
}

/**
 * 聚合配置数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationConfigDTO {

    @NotBlank(message = "聚合方法不能为空")
    private String aggregationMethod;

    @Min(value = 1, message = "最小参与者数量必须大于0")
    private Integer minParticipants;

    @Min(value = 1, message = "超时时间必须大于0")
    private Integer timeoutSeconds;

    @DecimalMin(value = "0.0", message = "正则化系数不能为负数")
    @DecimalMax(value = "1.0", message = "正则化系数不能大于1.0")
    private Double regularizationMu;

    private Boolean autoTrigger;
    private Double convergenceThreshold;
    private Integer maxRounds;
}

/**
 * 聚合结果数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationResultDTO {

    @NotNull(message = "全局模型不能为空")
    @Valid
    private GlobalModelDTO globalModel;

    @Min(value = 1, message = "参与者数量必须大于0")
    private Integer totalParticipants;

    @DecimalMin(value = "0.0", message = "聚合时间不能为负数")
    private Double aggregationTimeSeconds;

    @NotNull(message = "聚合开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime aggregationStartTime;

    @NotNull(message = "聚合结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime aggregationEndTime;

    private List<String> participantClientIds;
    private Map<String, Double> clientWeights;
    private List<Double> convergenceMetrics;
    private String status;
    private String errorMessage;
}

/**
 * 模型广播消息数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelBroadcastDTO {

    @NotBlank(message = "消息类型不能为空")
    private String messageType;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @Min(value = 1, message = "轮次必须大于0")
    private Integer round;

    @NotNull(message = "全局模型不能为空")
    private GlobalModelBroadcastData globalModel;

    @NotNull(message = "聚合信息不能为空")
    private AggregationInfoDTO aggregationInfo;

    @NotNull(message = "下轮配置不能为空")
    private NextRoundConfigDTO nextRoundConfig;

    @NotNull(message = "时间戳不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}

/**
 * 全局模型广播数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalModelBroadcastData {

    @NotNull(message = "模型元数据不能为空")
    private ModelMetadataDTO modelMetadata;

    @NotNull(message = "模型参数不能为空")
    private ModelParametersDTO parameters;

    private Map<String, Object> trainingConfig;
}

/**
 * 模型元数据数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelMetadataDTO {

    @NotBlank(message = "模型ID不能为空")
    private String modelId;

    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    @NotBlank(message = "算法名称不能为空")
    private String algorithm;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @NotBlank(message = "创建时间不能为空")
    private String createdAt;

    @NotBlank(message = "版本不能为空")
    private String version;

    private String description;
    private Map<String, Object> additionalInfo;
}

/**
 * 聚合信息数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationInfoDTO {

    @NotBlank(message = "聚合方法不能为空")
    private String method;

    @Min(value = 1, message = "参与者数量必须大于0")
    private Integer participants;

    @NotNull(message = "收敛指标不能为空")
    private ConvergenceMetricsDTO convergenceMetrics;

    private Double aggregationTime;
    private String status;
}

/**
 * 收敛指标数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvergenceMetricsDTO {

    @DecimalMin(value = "0.0", message = "参数变化不能为负数")
    private Double parameterChange;

    private Double improvement;
    private Double accuracy;
    private Double loss;
    private Boolean converged;
}

/**
 * 下轮配置数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextRoundConfigDTO {

    @Min(value = 1, message = "本地训练轮次必须大于0")
    private Integer localEpochs;

    @DecimalMin(value = "0.0", message = "目标准确率不能为负数")
    @DecimalMax(value = "1.0", message = "目标准确率不能大于1.0")
    private Double targetAccuracy;

    @Min(value = 1, message = "最大训练时间必须大于0")
    private Integer maxTrainingTime;

    @DecimalMin(value = "0.0", message = "学习率不能为负数")
    private Double learningRate;

    private Integer batchSize;
    private Boolean earlystopping;
    private Double validationSplit;
}

/**
 * WebSocket消息数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDTO {

    @NotBlank(message = "消息类型不能为空")
    private String messageType;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    private String clientId;

    @NotNull(message = "消息内容不能为空")
    private Object payload;

    @NotNull(message = "时间戳不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private Map<String, String> headers;
    private String sessionId;
}

/**
 * 任务状态数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusDTO {

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @NotBlank(message = "状态不能为空")
    private String status;

    private Integer connectedClients;
    private Integer currentRound;
    private Integer totalRounds;
    private String aggregationMethod;
    private Double progress;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdateTime;

    private List<String> activeClientIds;
    private Map<String, Object> statistics;
}

/**
 * 客户端连接信息数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientConnectionDTO {

    @NotBlank(message = "客户端ID不能为空")
    private String clientId;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @NotBlank(message = "状态不能为空")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime connectedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivity;

    private String ipAddress;
    private String userAgent;
    private Map<String, Object> clientInfo;
}

/**
 * 错误响应数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {

    @NotBlank(message = "错误代码不能为空")
    private String errorCode;

    @NotBlank(message = "错误消息不能为空")
    private String errorMessage;

    @NotNull(message = "时间戳不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private String taskId;
    private String clientId;
    private String details;
    private Map<String, Object> context;
}

/**
 * 统计信息数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDTO {

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    private Integer totalRounds;
    private Integer totalParticipants;
    private Double averageAggregationTime;
    private Double averageAccuracy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime taskStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime taskEndTime;

    private Map<String, Double> performanceMetrics;
    private List<Double> accuracyHistory;
    private List<Double> lossHistory;
    private Map<String, Integer> clientParticipationCount;
}

/**
 * 模型验证结果数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelValidationDTO {

    private Boolean isValid;
    private String validationStatus;
    private List<String> errors;
    private List<String> warnings;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validationTime;

    private Map<String, Object> validationDetails;
    private String modelId;
    private String taskId;
}
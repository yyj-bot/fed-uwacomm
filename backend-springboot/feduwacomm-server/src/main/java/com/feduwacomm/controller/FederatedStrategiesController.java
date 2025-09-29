package com.feduwacomm.controller;

import com.feduwacomm.aggregation.AggregationStrategyFactory;
import com.feduwacomm.common.BaseContext;
import com.feduwacomm.common.Result;
import com.feduwacomm.enums.FederatedAlgorithm;
import com.feduwacomm.enums.ModelType;
import com.feduwacomm.service.LogService;
import com.feduwacomm.utils.IpUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 联邦学习策略查询控制器
 * 提供可用聚合策略查询和配置信息获取功能
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 */
@Slf4j
@RestController
@RequestMapping("/api/federated/strategies")
public class FederatedStrategiesController {

    @Autowired
    private AggregationStrategyFactory strategyFactory;

    @Autowired
    private LogService logService;

    /**
     * 查询可用的聚合策略
     *
     * @param modelType 模型类型过滤 (可选)
     * @param category 策略分类过滤 (可选)
     * @param request HTTP请求对象
     * @return 可用策略列表和详细配置信息
     */
    @GetMapping("/available")
    public Result<StrategiesResponseVO> getAvailableStrategies(
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String category,
            HttpServletRequest request) {

        String userId = BaseContext.getCurrentId();
        String clientIp = IpUtil.getClientIpAddress(request);

        log.info("收到可用策略查询请求: modelType={}, category={}, userId={}, ip={}",
                modelType, category, userId, clientIp);

        // 记录访问日志
        logService.logInfo("策略查询",
                userId, null, request.getRequestURI(), clientIp, "SYSTEM.config");

        try {
            // 获取所有策略
            List<StrategyVO> strategies = buildAllStrategies();

            // 根据过滤条件筛选策略
            if (modelType != null && !modelType.trim().isEmpty()) {
                strategies = strategies.stream()
                        .filter(strategy -> strategy.getSupportedModelTypes().contains(modelType.toUpperCase()))
                        .collect(Collectors.toList());
            }

            if (category != null && !category.trim().isEmpty()) {
                strategies = strategies.stream()
                        .filter(strategy -> category.equalsIgnoreCase(strategy.getCategory()))
                        .collect(Collectors.toList());
            }

            // 构建响应
            StrategiesResponseVO response = StrategiesResponseVO.builder()
                    .total(strategies.size())
                    .strategies(strategies)
                    .categories(buildCategories())
                    .build();

            log.info("策略查询成功: 返回{}个策略, modelType={}, category={}, userId={}",
                    strategies.size(), modelType, category, userId);

            return Result.success(response);

        } catch (Exception e) {
            log.error("策略查询失败: modelType={}, category={}, userId={}, error={}",
                    modelType, category, userId, e.getMessage(), e);
            return Result.error("策略查询失败: " + e.getMessage());
        }
    }

    /**
     * 构建所有可用策略信息
     */
    private List<StrategyVO> buildAllStrategies() {
        return Arrays.stream(FederatedAlgorithm.values())
                .map(this::buildStrategyVO)
                .collect(Collectors.toList());
    }

    /**
     * 构建单个策略的VO对象
     */
    private StrategyVO buildStrategyVO(FederatedAlgorithm algorithm) {
        switch (algorithm) {
            case FEDERATED_AVERAGING:
                return StrategyVO.builder()
                        .algorithm(algorithm.getCode())
                        .name("联邦平均算法")
                        .description("经典的FedAvg算法，适用于大多数联邦学习场景")
                        .category("AVERAGING")
                        .supportedModelTypes(Arrays.asList("RANDOM_FOREST", "NEURAL_NETWORK"))
                        .parameters(Arrays.asList(
                                ParameterVO.builder()
                                        .name("learningRate")
                                        .type("DOUBLE")
                                        .description("学习率")
                                        .defaultValue(0.01)
                                        .range(ParameterRangeVO.builder().min(0.0001).max(1.0).build())
                                        .build(),
                                ParameterVO.builder()
                                        .name("momentum")
                                        .type("DOUBLE")
                                        .description("动量参数")
                                        .defaultValue(0.9)
                                        .range(ParameterRangeVO.builder().min(0.0).max(1.0).build())
                                        .build(),
                                ParameterVO.builder()
                                        .name("batchSize")
                                        .type("INTEGER")
                                        .description("批处理大小")
                                        .defaultValue(32)
                                        .range(ParameterRangeVO.builder().min(1.0).max(1024.0).build())
                                        .build()
                        ))
                        .requirements(RequirementsVO.builder()
                                .minParticipants(2)
                                .maxParticipants(100)
                                .recommendedParticipants(5)
                                .build())
                        .build();

            case FEDERATED_PROXIMAL:
                return StrategyVO.builder()
                        .algorithm(algorithm.getCode())
                        .name("联邦近端算法")
                        .description("FedProx算法，适用于非独立同分布数据的联邦学习")
                        .category("PROXIMAL")
                        .supportedModelTypes(Arrays.asList("NEURAL_NETWORK"))
                        .parameters(Arrays.asList(
                                ParameterVO.builder()
                                        .name("learningRate")
                                        .type("DOUBLE")
                                        .description("学习率")
                                        .defaultValue(0.01)
                                        .range(ParameterRangeVO.builder().min(0.0001).max(1.0).build())
                                        .build(),
                                ParameterVO.builder()
                                        .name("proximalMu")
                                        .type("DOUBLE")
                                        .description("近端参数μ")
                                        .defaultValue(0.1)
                                        .range(ParameterRangeVO.builder().min(0.0).max(10.0).build())
                                        .build()
                        ))
                        .requirements(RequirementsVO.builder()
                                .minParticipants(3)
                                .maxParticipants(50)
                                .recommendedParticipants(8)
                                .build())
                        .build();

            case FEDERATED_NOVA:
                return StrategyVO.builder()
                        .algorithm(algorithm.getCode())
                        .name("联邦Nova算法")
                        .description("FedNova算法，解决客户端异构性问题")
                        .category("NORMALIZATION")
                        .supportedModelTypes(Arrays.asList("NEURAL_NETWORK"))
                        .parameters(Arrays.asList(
                                ParameterVO.builder()
                                        .name("learningRate")
                                        .type("DOUBLE")
                                        .description("学习率")
                                        .defaultValue(0.01)
                                        .range(ParameterRangeVO.builder().min(0.0001).max(1.0).build())
                                        .build(),
                                ParameterVO.builder()
                                        .name("momentumFactor")
                                        .type("DOUBLE")
                                        .description("动量因子")
                                        .defaultValue(0.9)
                                        .range(ParameterRangeVO.builder().min(0.0).max(1.0).build())
                                        .build()
                        ))
                        .requirements(RequirementsVO.builder()
                                .minParticipants(2)
                                .maxParticipants(30)
                                .recommendedParticipants(6)
                                .build())
                        .build();

            case SCAFFOLD:
                return StrategyVO.builder()
                        .algorithm(algorithm.getCode())
                        .name("SCAFFOLD算法")
                        .description("SCAFFOLD算法，使用控制变量减少客户端漂移")
                        .category("VARIANCE_REDUCTION")
                        .supportedModelTypes(Arrays.asList("NEURAL_NETWORK"))
                        .parameters(Arrays.asList(
                                ParameterVO.builder()
                                        .name("learningRate")
                                        .type("DOUBLE")
                                        .description("学习率")
                                        .defaultValue(0.01)
                                        .range(ParameterRangeVO.builder().min(0.0001).max(1.0).build())
                                        .build(),
                                ParameterVO.builder()
                                        .name("clientLearningRate")
                                        .type("DOUBLE")
                                        .description("客户端学习率")
                                        .defaultValue(0.1)
                                        .range(ParameterRangeVO.builder().min(0.01).max(1.0).build())
                                        .build()
                        ))
                        .requirements(RequirementsVO.builder()
                                .minParticipants(3)
                                .maxParticipants(20)
                                .recommendedParticipants(5)
                                .build())
                        .build();

            default:
                return StrategyVO.builder()
                        .algorithm(algorithm.getCode())
                        .name(algorithm.getDescription())
                        .description("未知算法")
                        .category("UNKNOWN")
                        .supportedModelTypes(Arrays.asList("NEURAL_NETWORK"))
                        .parameters(Arrays.asList())
                        .requirements(RequirementsVO.builder()
                                .minParticipants(2)
                                .maxParticipants(10)
                                .recommendedParticipants(3)
                                .build())
                        .build();
        }
    }

    /**
     * 构建策略分类信息
     */
    private List<CategoryVO> buildCategories() {
        return Arrays.asList(
                CategoryVO.builder()
                        .category("AVERAGING")
                        .name("平均类算法")
                        .description("基于模型参数平均的联邦学习算法")
                        .build(),
                CategoryVO.builder()
                        .category("PROXIMAL")
                        .name("近端类算法")
                        .description("使用近端项处理数据异构性的算法")
                        .build(),
                CategoryVO.builder()
                        .category("NORMALIZATION")
                        .name("归一化类算法")
                        .description("通过归一化解决客户端差异的算法")
                        .build(),
                CategoryVO.builder()
                        .category("VARIANCE_REDUCTION")
                        .name("方差减少类算法")
                        .description("通过控制变量减少训练方差的算法")
                        .build()
        );
    }

    // VO类定义

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategiesResponseVO {
        private Integer total;
        private List<StrategyVO> strategies;
        private List<CategoryVO> categories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyVO {
        private String algorithm;
        private String name;
        private String description;
        private String category;
        private List<String> supportedModelTypes;
        private List<ParameterVO> parameters;
        private RequirementsVO requirements;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterVO {
        private String name;
        private String type;
        private String description;
        private Object defaultValue;
        private ParameterRangeVO range;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterRangeVO {
        private Double min;
        private Double max;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequirementsVO {
        private Integer minParticipants;
        private Integer maxParticipants;
        private Integer recommendedParticipants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryVO {
        private String category;
        private String name;
        private String description;
    }
}
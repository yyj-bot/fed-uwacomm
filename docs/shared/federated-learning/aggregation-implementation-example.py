#!/usr/bin/env python3
"""
联邦学习梯度聚合算法实现示例

本文件展示了在 FedUWAComm 系统中实现 RandomForest 联邦学习聚合算法的具体代码。
这些算法专门针对 Scikit-learn 框架的树模型进行了优化。

使用方法：
    python aggregation-implementation-example.py

依赖要求：
    pip install numpy scikit-learn websockets stomp.py json-logging
"""

import json
import logging
import numpy as np
import time
from datetime import datetime
from typing import Dict, List, Any, Optional, Tuple
from dataclasses import dataclass
from enum import Enum
import asyncio
import websockets


# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class AggregationMethod(Enum):
    """聚合方法枚举"""
    FEDAVG_RF = "FedAvg-RF"
    FEDPROX_RF = "FedProx-RF"
    ADAPTIVE_RF = "Adaptive-RF"


@dataclass
class ClientUpdate:
    """客户端更新数据结构"""
    client_id: str
    task_id: str
    round: int
    feature_importances: List[float]
    n_estimators: int
    samples_count: int
    training_time: float
    local_accuracy: float
    convergence_status: str
    timestamp: str


@dataclass
class GlobalModel:
    """全局模型数据结构"""
    model_id: str
    feature_importances: List[float]
    n_estimators: int
    aggregation_method: str
    participants: int
    round: int
    created_at: str


class RandomForestFederatedAggregator:
    """RandomForest 联邦学习聚合器"""

    def __init__(self, min_participants: int = 3):
        self.min_participants = min_participants
        self.logger = logging.getLogger(f"{__name__}.{self.__class__.__name__}")

    def fed_avg_aggregate(self, client_updates: List[ClientUpdate]) -> Dict[str, Any]:
        """
        FedAvg 聚合算法实现

        Args:
            client_updates: 客户端更新列表

        Returns:
            聚合后的模型参数
        """
        if len(client_updates) < self.min_participants:
            raise ValueError(f"参与者数量不足，需要至少 {self.min_participants} 个")

        # 1. 计算权重（基于样本数量）
        total_samples = sum(update.samples_count for update in client_updates)
        weights = [update.samples_count / total_samples for update in client_updates]

        # 2. 获取特征数量
        n_features = len(client_updates[0].feature_importances)

        # 3. 验证所有客户端的特征数量一致
        for update in client_updates:
            if len(update.feature_importances) != n_features:
                raise ValueError(f"客户端 {update.client_id} 的特征数量不匹配")

        # 4. 加权平均聚合特征重要性
        global_importances = np.zeros(n_features)
        for i, update in enumerate(client_updates):
            importance_array = np.array(update.feature_importances)
            global_importances += weights[i] * importance_array

        # 5. 归一化特征重要性
        total_importance = np.sum(global_importances)
        if total_importance > 0:
            normalized_importances = global_importances / total_importance
        else:
            # 如果所有重要性都为0，设置为均匀分布
            normalized_importances = np.ones(n_features) / n_features

        self.logger.info(f"FedAvg聚合完成，参与者: {len(client_updates)}")

        return {
            'feature_importances_': normalized_importances.tolist(),
            'n_estimators': client_updates[0].n_estimators,
            'aggregation_method': AggregationMethod.FEDAVG_RF.value,
            'participants': len(client_updates),
            'weights_used': weights
        }

    def fed_prox_aggregate(self, client_updates: List[ClientUpdate],
                          global_model: Optional[GlobalModel],
                          mu: float = 0.1) -> Dict[str, Any]:
        """
        FedProx 聚合算法实现

        Args:
            client_updates: 客户端更新列表
            global_model: 上一轮的全局模型
            mu: 正则化系数

        Returns:
            聚合后的模型参数
        """
        # 1. 首先执行 FedAvg 聚合
        fedavg_result = self.fed_avg_aggregate(client_updates)

        # 2. 如果有上一轮的全局模型，应用 FedProx 正则化
        if global_model is not None:
            global_importances = np.array(global_model.feature_importances)
            fedavg_importances = np.array(fedavg_result['feature_importances_'])

            # 3. FedProx 公式：θ_new = (1-μ) * θ_fedavg + μ * θ_global
            regularized_importances = (
                (1 - mu) * fedavg_importances + mu * global_importances
            )

            # 4. 重新归一化
            total_importance = np.sum(regularized_importances)
            if total_importance > 0:
                regularized_importances = regularized_importances / total_importance

            fedavg_result['feature_importances_'] = regularized_importances.tolist()
            fedavg_result['aggregation_method'] = AggregationMethod.FEDPROX_RF.value
            fedavg_result['regularization_mu'] = mu

        self.logger.info(f"FedProx聚合完成，正则化系数: {mu}")
        return fedavg_result

    def adaptive_aggregate(self, client_updates: List[ClientUpdate]) -> Dict[str, Any]:
        """
        自适应聚合算法实现

        基于模型不确定性调整客户端权重
        """
        # 1. 计算每个客户端的模型不确定性
        uncertainties = []
        performance_weights = []

        for update in client_updates:
            # 特征重要性的方差作为不确定性度量
            importance_array = np.array(update.feature_importances)
            variance = np.var(importance_array)
            uncertainty = 1.0 / (1.0 + variance)  # 方差越小，权重越大
            uncertainties.append(uncertainty)

            # 考虑本地准确率
            accuracy_weight = update.local_accuracy
            performance_weights.append(accuracy_weight)

        # 2. 组合不确定性权重和性能权重
        combined_weights = []
        for i in range(len(client_updates)):
            # 结合不确定性和性能，α 和 β 是权衡参数
            alpha, beta = 0.6, 0.4
            combined_weight = (
                alpha * uncertainties[i] + beta * performance_weights[i]
            )
            combined_weights.append(combined_weight)

        # 3. 归一化权重
        total_weight = sum(combined_weights)
        adaptive_weights = [w / total_weight for w in combined_weights]

        # 4. 使用自适应权重进行聚合
        n_features = len(client_updates[0].feature_importances)
        global_importances = np.zeros(n_features)

        for i, update in enumerate(client_updates):
            importance_array = np.array(update.feature_importances)
            global_importances += adaptive_weights[i] * importance_array

        # 5. 归一化
        total_importance = np.sum(global_importances)
        if total_importance > 0:
            normalized_importances = global_importances / total_importance
        else:
            normalized_importances = np.ones(n_features) / n_features

        self.logger.info(f"自适应聚合完成，权重分布: {adaptive_weights}")

        return {
            'feature_importances_': normalized_importances.tolist(),
            'n_estimators': client_updates[0].n_estimators,
            'aggregation_method': AggregationMethod.ADAPTIVE_RF.value,
            'participants': len(client_updates),
            'adaptive_weights': adaptive_weights,
            'uncertainty_scores': uncertainties,
            'performance_scores': performance_weights
        }


class FederatedAggregationService:
    """联邦学习聚合服务"""

    def __init__(self, aggregation_method: AggregationMethod = AggregationMethod.FEDAVG_RF):
        self.aggregation_method = aggregation_method
        self.aggregator = RandomForestFederatedAggregator()
        self.current_round = 0
        self.global_model: Optional[GlobalModel] = None
        self.client_updates: List[ClientUpdate] = []
        self.logger = logging.getLogger(f"{__name__}.{self.__class__.__name__}")

    def add_client_update(self, update_data: Dict[str, Any]) -> bool:
        """
        添加客户端更新

        Args:
            update_data: 客户端更新数据

        Returns:
            是否添加成功
        """
        try:
            # 解析客户端更新数据
            training_result = update_data.get('training_result', {})
            model_params = training_result.get('model_parameters', {})
            metadata = training_result.get('training_metadata', {})

            client_update = ClientUpdate(
                client_id=update_data['client_id'],
                task_id=update_data['task_id'],
                round=update_data['round'],
                feature_importances=model_params['feature_importances_'],
                n_estimators=model_params['n_estimators'],
                samples_count=metadata['samples_count'],
                training_time=metadata['training_time'],
                local_accuracy=metadata['local_accuracy'],
                convergence_status=metadata['convergence_status'],
                timestamp=update_data['timestamp']
            )

            # 验证更新有效性
            if self.validate_client_update(client_update):
                self.client_updates.append(client_update)
                self.logger.info(f"收到客户端更新: {client_update.client_id}")
                return True
            else:
                self.logger.warning(f"客户端更新验证失败: {client_update.client_id}")
                return False

        except Exception as e:
            self.logger.error(f"处理客户端更新时出错: {str(e)}")
            return False

    def validate_client_update(self, update: ClientUpdate) -> bool:
        """验证客户端更新的有效性"""
        try:
            # 检查特征重要性
            if not all(imp >= 0 for imp in update.feature_importances):
                return False

            # 检查特征重要性总和
            total = sum(update.feature_importances)
            if abs(total - 1.0) > 0.1:  # 允许10%的误差
                return False

            # 检查其他参数
            if update.n_estimators <= 0:
                return False

            if update.samples_count <= 0:
                return False

            if not 0 <= update.local_accuracy <= 1:
                return False

            return True

        except Exception as e:
            self.logger.error(f"验证客户端更新时出错: {str(e)}")
            return False

    def aggregate_updates(self) -> Optional[GlobalModel]:
        """
        聚合所有客户端更新

        Returns:
            聚合后的全局模型
        """
        if len(self.client_updates) < self.aggregator.min_participants:
            self.logger.warning(f"参与者数量不足: {len(self.client_updates)}")
            return None

        try:
            start_time = time.time()

            # 根据聚合方法执行聚合
            if self.aggregation_method == AggregationMethod.FEDAVG_RF:
                aggregated_params = self.aggregator.fed_avg_aggregate(self.client_updates)
            elif self.aggregation_method == AggregationMethod.FEDPROX_RF:
                aggregated_params = self.aggregator.fed_prox_aggregate(
                    self.client_updates, self.global_model, mu=0.1
                )
            elif self.aggregation_method == AggregationMethod.ADAPTIVE_RF:
                aggregated_params = self.aggregator.adaptive_aggregate(self.client_updates)
            else:
                raise ValueError(f"未知的聚合方法: {self.aggregation_method}")

            # 创建全局模型
            self.current_round += 1
            global_model = GlobalModel(
                model_id=f"global_model_round_{self.current_round}",
                feature_importances=aggregated_params['feature_importances_'],
                n_estimators=aggregated_params['n_estimators'],
                aggregation_method=aggregated_params['aggregation_method'],
                participants=len(self.client_updates),
                round=self.current_round,
                created_at=datetime.now().isoformat()
            )

            # 验证全局模型
            if self.validate_global_model(global_model):
                self.global_model = global_model
                aggregation_time = time.time() - start_time

                self.logger.info(f"聚合完成 - 轮次: {self.current_round}, "
                               f"参与者: {len(self.client_updates)}, "
                               f"用时: {aggregation_time:.2f}秒")

                # 清空客户端更新缓存
                self.client_updates.clear()

                return global_model
            else:
                self.logger.error("生成的全局模型验证失败")
                return None

        except Exception as e:
            self.logger.error(f"聚合过程出错: {str(e)}")
            return None

    def validate_global_model(self, model: GlobalModel) -> bool:
        """验证全局模型的有效性"""
        try:
            # 检查特征重要性
            if not all(imp >= 0 for imp in model.feature_importances):
                return False

            # 检查归一化
            total = sum(model.feature_importances)
            if abs(total - 1.0) > 0.01:
                return False

            # 检查模型结构
            if model.n_estimators <= 0:
                return False

            if model.participants < 1:
                return False

            return True

        except Exception as e:
            self.logger.error(f"验证全局模型时出错: {str(e)}")
            return False

    def get_global_model_for_broadcast(self) -> Optional[Dict[str, Any]]:
        """
        获取用于广播的全局模型格式

        Returns:
            适用于WebSocket广播的模型数据
        """
        if self.global_model is None:
            return None

        return {
            'message_type': 'GLOBAL_MODEL_UPDATE',
            'task_id': 'current_task',  # 应该从上下文获取
            'round': self.global_model.round,
            'global_model': {
                'model_metadata': {
                    'model_id': self.global_model.model_id,
                    'model_type': 'sklearn',
                    'algorithm': 'RandomForest',
                    'created_at': self.global_model.created_at,
                    'version': f"round_{self.global_model.round}"
                },
                'parameters': {
                    'feature_importances_': self.global_model.feature_importances,
                    'n_estimators': self.global_model.n_estimators
                }
            },
            'aggregation_info': {
                'method': self.global_model.aggregation_method,
                'participants': self.global_model.participants,
                'convergence_metrics': {
                    'parameter_change': self.calculate_parameter_change(),
                    'improvement': self.calculate_improvement()
                }
            },
            'next_round_config': {
                'local_epochs': 5,
                'target_accuracy': 0.90,
                'max_training_time': 300
            }
        }

    def calculate_parameter_change(self) -> float:
        """计算参数变化程度"""
        if self.current_round <= 1:
            return 1.0  # 第一轮没有比较基准

        # 这里应该与上一轮模型比较
        # 简化实现，返回固定值
        return 0.023

    def calculate_improvement(self) -> float:
        """计算模型改进程度"""
        # 这里应该基于验证集性能计算
        # 简化实现，返回固定值
        return 0.012


# WebSocket 通信处理器
class WebSocketHandler:
    """WebSocket 通信处理器"""

    def __init__(self, aggregation_service: FederatedAggregationService):
        self.aggregation_service = aggregation_service
        self.logger = logging.getLogger(f"{__name__}.{self.__class__.__name__}")

    async def handle_client_message(self, websocket, path):
        """处理客户端消息"""
        try:
            async for message in websocket:
                data = json.loads(message)
                message_type = data.get('message_type')

                if message_type == 'GRADIENT_UPLOAD':
                    await self.handle_gradient_upload(websocket, data)
                elif message_type == 'REQUEST_GLOBAL_MODEL':
                    await self.handle_model_request(websocket, data)
                else:
                    self.logger.warning(f"未知消息类型: {message_type}")

        except websockets.exceptions.ConnectionClosed:
            self.logger.info("客户端连接已断开")
        except Exception as e:
            self.logger.error(f"处理WebSocket消息时出错: {str(e)}")

    async def handle_gradient_upload(self, websocket, data):
        """处理梯度上传"""
        try:
            success = self.aggregation_service.add_client_update(data)

            # 发送确认消息
            response = {
                'message_type': 'UPLOAD_CONFIRMATION',
                'client_id': data.get('client_id'),
                'success': success,
                'timestamp': datetime.now().isoformat()
            }
            await websocket.send(json.dumps(response))

            # 如果收集够了足够的更新，进行聚合
            if len(self.aggregation_service.client_updates) >= self.aggregation_service.aggregator.min_participants:
                global_model = self.aggregation_service.aggregate_updates()
                if global_model:
                    await self.broadcast_global_model(global_model)

        except Exception as e:
            self.logger.error(f"处理梯度上传时出错: {str(e)}")

    async def handle_model_request(self, websocket, data):
        """处理全局模型请求"""
        try:
            model_data = self.aggregation_service.get_global_model_for_broadcast()
            if model_data:
                await websocket.send(json.dumps(model_data))
            else:
                error_response = {
                    'message_type': 'ERROR',
                    'error': '全局模型尚未可用',
                    'timestamp': datetime.now().isoformat()
                }
                await websocket.send(json.dumps(error_response))

        except Exception as e:
            self.logger.error(f"处理模型请求时出错: {str(e)}")

    async def broadcast_global_model(self, global_model: GlobalModel):
        """广播全局模型（简化实现）"""
        # 实际实现中，这里应该向所有连接的客户端广播
        model_data = self.aggregation_service.get_global_model_for_broadcast()
        self.logger.info(f"广播全局模型: {global_model.model_id}")
        # 这里省略具体的广播实现


# 示例使用代码
def main():
    """主函数示例"""
    print("联邦学习梯度聚合算法实现示例")
    print("=" * 50)

    # 1. 创建聚合服务
    aggregation_service = FederatedAggregationService(
        aggregation_method=AggregationMethod.FEDAVG_RF
    )

    # 2. 模拟客户端更新数据
    sample_updates = [
        {
            'client_id': 'vm_client_001',
            'task_id': 'fed_task_001',
            'round': 1,
            'training_result': {
                'model_parameters': {
                    'feature_importances_': [0.25, 0.20, 0.15, 0.18, 0.12, 0.10],
                    'n_estimators': 100
                },
                'training_metadata': {
                    'samples_count': 1500,
                    'training_time': 45.2,
                    'local_accuracy': 0.87,
                    'convergence_status': 'converged'
                }
            },
            'timestamp': '2025-09-25T10:30:45Z'
        },
        {
            'client_id': 'vm_client_002',
            'task_id': 'fed_task_001',
            'round': 1,
            'training_result': {
                'model_parameters': {
                    'feature_importances_': [0.22, 0.23, 0.17, 0.16, 0.11, 0.11],
                    'n_estimators': 100
                },
                'training_metadata': {
                    'samples_count': 1200,
                    'training_time': 38.7,
                    'local_accuracy': 0.84,
                    'convergence_status': 'converged'
                }
            },
            'timestamp': '2025-09-25T10:32:15Z'
        },
        {
            'client_id': 'vm_client_003',
            'task_id': 'fed_task_001',
            'round': 1,
            'training_result': {
                'model_parameters': {
                    'feature_importances_': [0.27, 0.18, 0.16, 0.19, 0.13, 0.07],
                    'n_estimators': 100
                },
                'training_metadata': {
                    'samples_count': 1800,
                    'training_time': 52.1,
                    'local_accuracy': 0.89,
                    'convergence_status': 'converged'
                }
            },
            'timestamp': '2025-09-25T10:33:22Z'
        }
    ]

    # 3. 添加客户端更新
    print("添加客户端更新...")
    for update in sample_updates:
        success = aggregation_service.add_client_update(update)
        print(f"客户端 {update['client_id']}: {'成功' if success else '失败'}")

    # 4. 执行聚合
    print("\n执行聚合...")
    global_model = aggregation_service.aggregate_updates()

    if global_model:
        print(f"聚合成功！")
        print(f"全局模型ID: {global_model.model_id}")
        print(f"特征重要性: {global_model.feature_importances}")
        print(f"聚合方法: {global_model.aggregation_method}")
        print(f"参与者数量: {global_model.participants}")

        # 5. 获取广播格式
        print("\n生成广播消息...")
        broadcast_data = aggregation_service.get_global_model_for_broadcast()
        if broadcast_data:
            print("广播消息格式:")
            print(json.dumps(broadcast_data, indent=2, ensure_ascii=False))
    else:
        print("聚合失败！")

    print("\n示例完成。")


if __name__ == "__main__":
    main()
#!/usr/bin/env python3
"""
Scikit-learn 初始模型 JSON 格式验证脚本模板

本脚本用于验证初始模型 JSON 格式是否符合 ModelWrapper 兼容性要求。
使用方法：
    python model-validation-template.py <model_json_file>

依赖要求：
    pip install jsonschema scikit-learn numpy
"""

import json
import sys
import logging
from pathlib import Path
from typing import Dict, Any, Optional
import numpy as np

# 可选依赖
try:
    import jsonschema
    JSONSCHEMA_AVAILABLE = True
except ImportError:
    JSONSCHEMA_AVAILABLE = False
    print("警告: jsonschema 未安装，将跳过 JSON Schema 验证")

try:
    from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False
    print("警告: scikit-learn 未安装，将跳过 ModelWrapper 兼容性测试")


class ModelJSONValidator:
    """初始模型 JSON 验证器"""

    def __init__(self, schema_file: Optional[str] = None):
        self.logger = logging.getLogger(__name__)
        self.schema = None

        # 加载 JSON Schema
        if schema_file and JSONSCHEMA_AVAILABLE:
            try:
                with open(schema_file, 'r', encoding='utf-8') as f:
                    self.schema = json.load(f)
                self.logger.info(f"已加载 JSON Schema: {schema_file}")
            except Exception as e:
                self.logger.warning(f"加载 Schema 失败: {e}")

    def validate_json_format(self, model_data: Dict[str, Any]) -> bool:
        """验证 JSON 格式合法性"""
        self.logger.info("=== JSON 格式验证 ===")

        try:
            # 基本结构验证
            required_fields = ['model_metadata', 'parameters']
            for field in required_fields:
                if field not in model_data:
                    self.logger.error(f"缺少必需字段: {field}")
                    return False

            # model_metadata 验证
            metadata = model_data['model_metadata']
            required_metadata = ['model_id', 'model_type', 'algorithm', 'task_type']
            for field in required_metadata:
                if field not in metadata:
                    self.logger.error(f"model_metadata 缺少字段: {field}")
                    return False

            # 验证 model_type
            if metadata['model_type'] != 'sklearn':
                self.logger.error(f"model_type 必须为 'sklearn'，当前为: {metadata['model_type']}")
                return False

            self.logger.info("✓ JSON 格式验证通过")
            return True

        except Exception as e:
            self.logger.error(f"JSON 格式验证失败: {e}")
            return False

    def validate_json_schema(self, model_data: Dict[str, Any]) -> bool:
        """使用 JSON Schema 验证"""
        if not JSONSCHEMA_AVAILABLE or not self.schema:
            self.logger.info("跳过 JSON Schema 验证")
            return True

        self.logger.info("=== JSON Schema 验证 ===")

        try:
            jsonschema.validate(instance=model_data, schema=self.schema)
            self.logger.info("✓ JSON Schema 验证通过")
            return True
        except jsonschema.ValidationError as e:
            self.logger.error(f"JSON Schema 验证失败: {e.message}")
            self.logger.error(f"错误路径: {' -> '.join(str(x) for x in e.path)}")
            return False
        except Exception as e:
            self.logger.error(f"JSON Schema 验证异常: {e}")
            return False

    def validate_parameters_consistency(self, model_data: Dict[str, Any]) -> bool:
        """验证参数一致性"""
        self.logger.info("=== 参数一致性验证 ===")

        try:
            parameters = model_data['parameters']

            # 验证特征重要性数组存在性
            if 'feature_importances_' not in parameters:
                self.logger.error("必需参数 feature_importances_ 缺失")
                return False

            # 验证特征重要性数值范围
            if 'feature_importances_' in parameters:
                importances = parameters['feature_importances_']
                if any(imp < 0 for imp in importances):
                    self.logger.error("feature_importances_ 包含负值")
                    return False

                importance_sum = sum(importances)
                if abs(importance_sum - 1.0) > 0.1:
                    self.logger.warning(f"feature_importances_ 总和 ({importance_sum:.4f}) 偏离 1.0 较多")

            # 验证n_estimators存在性和有效性
            if 'n_estimators' not in parameters:
                self.logger.error("必需参数 n_estimators 缺失")
                return False

            n_estimators = parameters['n_estimators']
            if not isinstance(n_estimators, int) or n_estimators <= 0:
                self.logger.error(f"n_estimators 必须为正整数，当前为: {n_estimators}")
                return False

            # 验证分类模型的类别一致性（可选参数，如果存在才检查）
            if 'n_classes_' in parameters and 'classes_' in parameters:
                n_classes = parameters['n_classes_']
                classes_len = len(parameters['classes_'])
                if n_classes != classes_len:
                    self.logger.warning(f"n_classes_ ({n_classes}) 与 classes_ 长度 ({classes_len}) 不一致")
                    # 注意：这里改为警告而非错误，因为这些参数通常在训练后生成

            self.logger.info("✓ 参数一致性验证通过")
            return True

        except Exception as e:
            self.logger.error(f"参数一致性验证失败: {e}")
            return False

    def validate_modelwrapper_compatibility(self, model_data: Dict[str, Any]) -> bool:
        """验证 ModelWrapper 兼容性"""
        if not SKLEARN_AVAILABLE:
            self.logger.info("跳过 ModelWrapper 兼容性测试")
            return True

        self.logger.info("=== ModelWrapper 兼容性验证 ===")

        try:
            parameters = model_data['parameters']
            task_type = model_data['model_metadata']['task_type']

            # 创建对应的 sklearn 模型
            if task_type == 'regression':
                model = RandomForestRegressor(n_estimators=100, random_state=42)
            elif task_type == 'classification':
                model = RandomForestClassifier(n_estimators=100, random_state=42)
            else:
                self.logger.error(f"不支持的任务类型: {task_type}")
                return False

            # 模拟 ModelWrapper.set_parameters() 行为
            try:
                # 验证参数是否可以设置到模型
                for key, value in parameters.items():
                    if hasattr(model, key):
                        # 尝试设置参数
                        if isinstance(value, list):
                            value = np.array(value)
                        setattr(model, key, value)
                        self.logger.debug(f"成功设置参数: {key}")
                    else:
                        self.logger.warning(f"模型没有参数: {key}")

                # 验证关键参数
                required_params = ['feature_importances_', 'n_estimators']
                for param in required_params:
                    if param not in parameters:
                        self.logger.error(f"缺少必需参数: {param}")
                        return False

                self.logger.info("✓ ModelWrapper 兼容性验证通过")
                return True

            except Exception as e:
                self.logger.error(f"参数设置失败: {e}")
                return False

        except Exception as e:
            self.logger.error(f"ModelWrapper 兼容性验证失败: {e}")
            return False

    def validate_training_config(self, model_data: Dict[str, Any]) -> bool:
        """验证训练配置"""
        self.logger.info("=== 训练配置验证 ===")

        try:
            if 'training_config' not in model_data:
                self.logger.info("无训练配置，跳过验证")
                return True

            config = model_data['training_config']

            # 验证特征名称与特征重要性的一致性
            if 'feature_names' in config and 'parameters' in model_data:
                feature_names = config['feature_names']
                if 'feature_importances_' in model_data['parameters']:
                    importances = model_data['parameters']['feature_importances_']
                    if len(feature_names) != len(importances):
                        self.logger.error(f"feature_names 长度 ({len(feature_names)}) 与 feature_importances_ 长度 ({len(importances)}) 不一致")
                        return False

            # 验证随机种子
            if 'random_state' in config:
                random_state = config['random_state']
                if random_state is not None and (not isinstance(random_state, int) or random_state < 0):
                    self.logger.error(f"random_state 必须为非负整数或 null")
                    return False

            self.logger.info("✓ 训练配置验证通过")
            return True

        except Exception as e:
            self.logger.error(f"训练配置验证失败: {e}")
            return False

    def validate_model_json(self, model_json_file: str) -> bool:
        """完整验证模型 JSON 文件"""
        self.logger.info(f"开始验证模型文件: {model_json_file}")

        try:
            # 加载 JSON 文件
            with open(model_json_file, 'r', encoding='utf-8') as f:
                model_data = json.load(f)

            # 执行各项验证
            validations = [
                self.validate_json_format,
                self.validate_json_schema,
                self.validate_parameters_consistency,
                self.validate_modelwrapper_compatibility,
                self.validate_training_config
            ]

            all_passed = True
            for validation in validations:
                if not validation(model_data):
                    all_passed = False

            if all_passed:
                self.logger.info("🎉 所有验证通过！模型 JSON 格式正确")
            else:
                self.logger.error("❌ 验证失败，请检查模型 JSON 格式")

            return all_passed

        except json.JSONDecodeError as e:
            self.logger.error(f"JSON 解析失败: {e}")
            return False
        except FileNotFoundError:
            self.logger.error(f"文件不存在: {model_json_file}")
            return False
        except Exception as e:
            self.logger.error(f"验证过程发生异常: {e}")
            return False


def main():
    """主函数"""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(levelname)s - %(message)s'
    )

    if len(sys.argv) < 2:
        print("使用方法: python model-validation-template.py <model_json_file> [schema_file]")
        print("示例:")
        print("  python model-validation-template.py random-forest-regressor-example.json")
        print("  python model-validation-template.py random-forest-regressor-example.json model-parameter-schema.json")
        sys.exit(1)

    model_json_file = sys.argv[1]
    schema_file = sys.argv[2] if len(sys.argv) > 2 else "model-parameter-schema.json"

    # 检查文件是否存在
    if not Path(model_json_file).exists():
        print(f"错误: 模型文件不存在: {model_json_file}")
        sys.exit(1)

    # 检查 Schema 文件
    if not Path(schema_file).exists():
        print(f"警告: Schema 文件不存在: {schema_file}")
        schema_file = None

    # 创建验证器并执行验证
    validator = ModelJSONValidator(schema_file)
    success = validator.validate_model_json(model_json_file)

    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
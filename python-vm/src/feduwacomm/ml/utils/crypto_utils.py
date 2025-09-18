"""
加密工具

提供消息签名、验证等安全功能，用于WebSocket消息的安全传输。
"""

import hashlib
import hmac
import base64
import json
import time
import logging
from typing import Dict, Any, Optional

# 可选依赖：cryptography
try:
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import rsa, padding
    from cryptography.hazmat.primitives.serialization import load_pem_private_key, load_pem_public_key
    CRYPTO_AVAILABLE = True
except ImportError:
    CRYPTO_AVAILABLE = False


class CryptoUtils:
    """加密工具类"""
    
    def __init__(self, private_key_pem: Optional[str] = None, public_key_pem: Optional[str] = None):
        """初始化加密工具
        
        Args:
            private_key_pem: PEM格式的私钥
            public_key_pem: PEM格式的公钥
        """
        self.logger = logging.getLogger("CryptoUtils")
        
        if not CRYPTO_AVAILABLE:
            self.logger.warning("cryptography库未安装，加密功能受限")
        
        self.private_key = None
        self.public_key = None
        
        if private_key_pem and CRYPTO_AVAILABLE:
            try:
                self.private_key = load_pem_private_key(
                    private_key_pem.encode(), 
                    password=None
                )
            except Exception as e:
                self.logger.error(f"加载私钥失败: {e}")
        
        if public_key_pem and CRYPTO_AVAILABLE:
            try:
                self.public_key = load_pem_public_key(public_key_pem.encode())
            except Exception as e:
                self.logger.error(f"加载公钥失败: {e}")
    
    def generate_key_pair(self) -> tuple[str, str]:
        """生成RSA密钥对
        
        Returns:
            tuple: (私钥PEM, 公钥PEM)
        """
        if not CRYPTO_AVAILABLE:
            raise ImportError("需要安装cryptography库")
        
        try:
            # 生成私钥
            private_key = rsa.generate_private_key(
                public_exponent=65537,
                key_size=2048
            )
            
            # 获取公钥
            public_key = private_key.public_key()
            
            # 序列化为PEM格式
            private_pem = private_key.private_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PrivateFormat.PKCS8,
                encryption_algorithm=serialization.NoEncryption()
            ).decode()
            
            public_pem = public_key.public_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PublicFormat.SubjectPublicKeyInfo
            ).decode()
            
            return private_pem, public_pem
            
        except Exception as e:
            self.logger.error(f"生成密钥对失败: {e}")
            raise
    
    def sign_message(self, message: Dict[str, Any]) -> str:
        """对消息进行数字签名
        
        Args:
            message: 要签名的消息字典
            
        Returns:
            str: Base64编码的签名
        """
        if not self.private_key or not CRYPTO_AVAILABLE:
            # 如果没有私钥或加密库不可用，返回简单的HMAC签名
            return self._hmac_sign_message(message)
        
        try:
            # 创建消息的标准化JSON表示
            message_json = self._normalize_message(message)
            message_bytes = message_json.encode('utf-8')
            
            # 使用私钥签名
            signature = self.private_key.sign(
                message_bytes,
                padding.PSS(
                    mgf=padding.MGF1(hashes.SHA256()),
                    salt_length=padding.PSS.MAX_LENGTH
                ),
                hashes.SHA256()
            )
            
            # 返回Base64编码的签名
            return base64.b64encode(signature).decode()
            
        except Exception as e:
            self.logger.error(f"消息签名失败: {e}")
            return ""
    
    def verify_signature(self, message: Dict[str, Any], signature: str) -> bool:
        """验证消息签名
        
        Args:
            message: 消息字典
            signature: Base64编码的签名
            
        Returns:
            bool: 签名是否有效
        """
        if not self.public_key or not CRYPTO_AVAILABLE:
            # 如果没有公钥或加密库不可用，使用HMAC验证
            return self._hmac_verify_message(message, signature)
        
        try:
            # 创建消息的标准化JSON表示
            message_json = self._normalize_message(message)
            message_bytes = message_json.encode('utf-8')
            
            # 解码签名
            signature_bytes = base64.b64decode(signature)
            
            # 验证签名
            self.public_key.verify(
                signature_bytes,
                message_bytes,
                padding.PSS(
                    mgf=padding.MGF1(hashes.SHA256()),
                    salt_length=padding.PSS.MAX_LENGTH
                ),
                hashes.SHA256()
            )
            
            return True
            
        except Exception as e:
            self.logger.error(f"签名验证失败: {e}")
            return False
    
    def _hmac_sign_message(self, message: Dict[str, Any], secret_key: str = "default_key") -> str:
        """使用HMAC对消息签名（简化版本）
        
        Args:
            message: 消息字典
            secret_key: 密钥
            
        Returns:
            str: HMAC签名
        """
        try:
            message_json = self._normalize_message(message)
            signature = hmac.new(
                secret_key.encode(),
                message_json.encode(),
                hashlib.sha256
            ).hexdigest()
            
            return signature
            
        except Exception as e:
            self.logger.error(f"HMAC签名失败: {e}")
            return ""
    
    def _hmac_verify_message(self, message: Dict[str, Any], signature: str, secret_key: str = "default_key") -> bool:
        """使用HMAC验证消息签名（简化版本）
        
        Args:
            message: 消息字典
            signature: HMAC签名
            secret_key: 密钥
            
        Returns:
            bool: 签名是否有效
        """
        try:
            expected_signature = self._hmac_sign_message(message, secret_key)
            return hmac.compare_digest(signature, expected_signature)
            
        except Exception as e:
            self.logger.error(f"HMAC验证失败: {e}")
            return False
    
    def _normalize_message(self, message: Dict[str, Any]) -> str:
        """标准化消息格式用于签名
        
        Args:
            message: 消息字典
            
        Returns:
            str: 标准化的JSON字符串
        """
        # 创建消息副本，移除signature字段
        normalized_message = message.copy()
        normalized_message.pop('signature', None)
        
        # 按键排序并生成JSON
        return json.dumps(normalized_message, sort_keys=True, separators=(',', ':'))
    
    def generate_message_id(self, prefix: str = "msg") -> str:
        """生成唯一的消息ID
        
        Args:
            prefix: ID前缀
            
        Returns:
            str: 消息ID
        """
        timestamp = int(time.time() * 1000)
        random_part = hashlib.md5(str(timestamp).encode()).hexdigest()[:8]
        return f"{prefix}-{timestamp}-{random_part}"
    
    def hash_data(self, data: str, algorithm: str = "sha256") -> str:
        """对数据进行哈希
        
        Args:
            data: 要哈希的数据
            algorithm: 哈希算法
            
        Returns:
            str: 哈希值的十六进制表示
        """
        try:
            if algorithm == "sha256":
                return hashlib.sha256(data.encode()).hexdigest()
            elif algorithm == "sha1":
                return hashlib.sha1(data.encode()).hexdigest()
            elif algorithm == "md5":
                return hashlib.md5(data.encode()).hexdigest()
            else:
                raise ValueError(f"不支持的哈希算法: {algorithm}")
                
        except Exception as e:
            self.logger.error(f"数据哈希失败: {e}")
            return ""
    
    def encode_base64(self, data: str) -> str:
        """Base64编码
        
        Args:
            data: 要编码的数据
            
        Returns:
            str: Base64编码结果
        """
        try:
            return base64.b64encode(data.encode()).decode()
        except Exception as e:
            self.logger.error(f"Base64编码失败: {e}")
            return ""
    
    def decode_base64(self, encoded_data: str) -> str:
        """Base64解码
        
        Args:
            encoded_data: Base64编码的数据
            
        Returns:
            str: 解码结果
        """
        try:
            return base64.b64decode(encoded_data).decode()
        except Exception as e:
            self.logger.error(f"Base64解码失败: {e}")
            return ""
    
    def create_checksum(self, data: Dict[str, Any]) -> str:
        """为数据创建校验和
        
        Args:
            data: 数据字典
            
        Returns:
            str: 校验和
        """
        try:
            data_json = json.dumps(data, sort_keys=True, separators=(',', ':'))
            return self.hash_data(data_json, "sha256")
        except Exception as e:
            self.logger.error(f"创建校验和失败: {e}")
            return ""
    
    def verify_checksum(self, data: Dict[str, Any], expected_checksum: str) -> bool:
        """验证数据校验和
        
        Args:
            data: 数据字典
            expected_checksum: 期望的校验和
            
        Returns:
            bool: 校验和是否正确
        """
        actual_checksum = self.create_checksum(data)
        return actual_checksum == expected_checksum

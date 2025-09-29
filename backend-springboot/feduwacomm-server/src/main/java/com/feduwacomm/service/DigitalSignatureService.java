package com.feduwacomm.service;

/**
 * 数字签名服务接口
 *
 * 提供消息的数字签名和验证功能，支持RSA和ECDSA算法
 * 替换项目中所有的临时签名实现，确保消息的完整性和安全性
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
public interface DigitalSignatureService {

    /**
     * 签名算法类型
     */
    enum SignatureAlgorithm {
        RSA_SHA256("RSA", "SHA256withRSA"),
        ECDSA_SHA256("EC", "SHA256withECDSA");

        private final String keyAlgorithm;
        private final String signatureAlgorithm;

        SignatureAlgorithm(String keyAlgorithm, String signatureAlgorithm) {
            this.keyAlgorithm = keyAlgorithm;
            this.signatureAlgorithm = signatureAlgorithm;
        }

        public String getKeyAlgorithm() { return keyAlgorithm; }
        public String getSignatureAlgorithm() { return signatureAlgorithm; }
    }

    /**
     * 对消息内容进行数字签名
     *
     * @param messageContent 待签名的消息内容
     * @param algorithm 签名算法
     * @return 签名结果的Base64编码字符串
     * @throws SignatureException 签名过程中发生错误
     */
    String signMessage(String messageContent, SignatureAlgorithm algorithm) throws SignatureException;

    /**
     * 验证消息的数字签名
     *
     * @param messageContent 原始消息内容
     * @param signature 签名的Base64编码字符串
     * @param algorithm 签名算法
     * @return 验证结果，true表示签名有效，false表示签名无效
     * @throws SignatureException 验证过程中发生错误
     */
    boolean verifySignature(String messageContent, String signature, SignatureAlgorithm algorithm) throws SignatureException;

    /**
     * 为WebSocket消息生成签名
     * 这是一个便捷方法，默认使用RSA-SHA256算法
     *
     * @param messageType 消息类型
     * @param messageId 消息ID
     * @param vmId 虚拟机ID
     * @param messageData 消息数据（JSON字符串）
     * @return 签名结果的Base64编码字符串
     * @throws SignatureException 签名过程中发生错误
     */
    String signWebSocketMessage(String messageType, String messageId, String vmId, String messageData) throws SignatureException;

    /**
     * 验证WebSocket消息签名
     *
     * @param messageType 消息类型
     * @param messageId 消息ID
     * @param vmId 虚拟机ID
     * @param messageData 消息数据（JSON字符串）
     * @param signature 签名的Base64编码字符串
     * @return 验证结果
     * @throws SignatureException 验证过程中发生错误
     */
    boolean verifyWebSocketMessage(String messageType, String messageId, String vmId, String messageData, String signature) throws SignatureException;

    /**
     * 初始化密钥对
     * 在服务启动时调用，生成或加载公私钥对
     *
     * @param algorithm 签名算法
     * @throws SignatureException 初始化过程中发生错误
     */
    void initializeKeyPair(SignatureAlgorithm algorithm) throws SignatureException;

    /**
     * 获取公钥（PEM格式）
     *
     * @param algorithm 签名算法
     * @return 公钥的PEM格式字符串
     * @throws SignatureException 获取公钥过程中发生错误
     */
    String getPublicKeyPem(SignatureAlgorithm algorithm) throws SignatureException;

    /**
     * 数字签名异常类
     */
    class SignatureException extends Exception {
        public SignatureException(String message) {
            super(message);
        }

        public SignatureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
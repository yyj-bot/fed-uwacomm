package com.feduwacomm.service.impl;

import com.feduwacomm.service.DigitalSignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 数字签名服务实现类
 *
 * 使用RSA和ECDSA算法提供消息的数字签名和验证功能
 * 支持密钥对的生成、存储和管理
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@Service
public class DigitalSignatureServiceImpl implements DigitalSignatureService {

    @Value("${feduwacomm.security.signature.rsa.key-size:2048}")
    private int rsaKeySize;

    @Value("${feduwacomm.security.signature.ecdsa.curve:secp256r1}")
    private String ecdsaCurve;

    @Value("${feduwacomm.security.signature.default-algorithm:RSA_SHA256}")
    private String defaultAlgorithm;

    // 存储不同算法的密钥对
    private final Map<SignatureAlgorithm, KeyPair> keyPairs = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            // 初始化默认算法的密钥对
            SignatureAlgorithm defaultAlg = SignatureAlgorithm.valueOf(defaultAlgorithm);
            initializeKeyPair(defaultAlg);
            log.info("数字签名服务初始化完成，默认算法: {}", defaultAlg);
        } catch (Exception e) {
            log.error("数字签名服务初始化失败", e);
            throw new RuntimeException("数字签名服务初始化失败", e);
        }
    }

    @Override
    public String signMessage(String messageContent, SignatureAlgorithm algorithm) throws SignatureException {
        try {
            KeyPair keyPair = getKeyPair(algorithm);
            Signature signature = Signature.getInstance(algorithm.getSignatureAlgorithm());
            signature.initSign(keyPair.getPrivate());
            signature.update(messageContent.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = signature.sign();
            String result = Base64.getEncoder().encodeToString(signatureBytes);

            log.debug("消息签名完成: algorithm={}, contentLength={}, signatureLength={}",
                     algorithm, messageContent.length(), result.length());
            return result;
        } catch (Exception e) {
            throw new SignatureException("消息签名失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifySignature(String messageContent, String signature, SignatureAlgorithm algorithm) throws SignatureException {
        try {
            KeyPair keyPair = getKeyPair(algorithm);
            Signature verifier = Signature.getInstance(algorithm.getSignatureAlgorithm());
            verifier.initVerify(keyPair.getPublic());
            verifier.update(messageContent.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            boolean isValid = verifier.verify(signatureBytes);

            log.debug("签名验证完成: algorithm={}, valid={}", algorithm, isValid);
            return isValid;
        } catch (Exception e) {
            throw new SignatureException("签名验证失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String signWebSocketMessage(String messageType, String messageId, String vmId, String messageData) throws SignatureException {
        // 构建标准的签名内容格式
        String signatureContent = buildWebSocketSignatureContent(messageType, messageId, vmId, messageData);
        return signMessage(signatureContent, SignatureAlgorithm.valueOf(defaultAlgorithm));
    }

    @Override
    public boolean verifyWebSocketMessage(String messageType, String messageId, String vmId, String messageData, String signature) throws SignatureException {
        String signatureContent = buildWebSocketSignatureContent(messageType, messageId, vmId, messageData);
        return verifySignature(signatureContent, signature, SignatureAlgorithm.valueOf(defaultAlgorithm));
    }

    @Override
    public void initializeKeyPair(SignatureAlgorithm algorithm) throws SignatureException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm.getKeyAlgorithm());

            if (algorithm == SignatureAlgorithm.RSA_SHA256) {
                keyPairGenerator.initialize(rsaKeySize);
            } else if (algorithm == SignatureAlgorithm.ECDSA_SHA256) {
                ECGenParameterSpec ecSpec = new ECGenParameterSpec(ecdsaCurve);
                keyPairGenerator.initialize(ecSpec);
            }

            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            keyPairs.put(algorithm, keyPair);

            log.info("密钥对初始化完成: algorithm={}, keySize={}",
                    algorithm, getKeySize(keyPair.getPublic()));
        } catch (Exception e) {
            throw new SignatureException("密钥对初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPublicKeyPem(SignatureAlgorithm algorithm) throws SignatureException {
        try {
            KeyPair keyPair = getKeyPair(algorithm);
            PublicKey publicKey = keyPair.getPublic();

            String keyType = algorithm == SignatureAlgorithm.RSA_SHA256 ? "RSA PUBLIC KEY" : "EC PUBLIC KEY";
            String base64Key = Base64.getEncoder().encodeToString(publicKey.getEncoded());

            StringBuilder pem = new StringBuilder();
            pem.append("-----BEGIN ").append(keyType).append("-----\n");

            // 每64个字符换行
            for (int i = 0; i < base64Key.length(); i += 64) {
                int endIndex = Math.min(i + 64, base64Key.length());
                pem.append(base64Key, i, endIndex).append("\n");
            }

            pem.append("-----END ").append(keyType).append("-----");

            return pem.toString();
        } catch (Exception e) {
            throw new SignatureException("获取公钥PEM格式失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取指定算法的密钥对
     */
    private KeyPair getKeyPair(SignatureAlgorithm algorithm) throws SignatureException {
        KeyPair keyPair = keyPairs.get(algorithm);
        if (keyPair == null) {
            throw new SignatureException("未找到算法的密钥对: " + algorithm);
        }
        return keyPair;
    }

    /**
     * 构建WebSocket消息的签名内容
     * 使用标准格式: messageType|messageId|vmId|messageData
     */
    private String buildWebSocketSignatureContent(String messageType, String messageId, String vmId, String messageData) {
        StringBuilder content = new StringBuilder();
        content.append(messageType != null ? messageType : "").append("|");
        content.append(messageId != null ? messageId : "").append("|");
        content.append(vmId != null ? vmId : "").append("|");
        content.append(messageData != null ? messageData : "");

        return content.toString();
    }

    /**
     * 获取密钥大小（用于日志）
     */
    private int getKeySize(PublicKey publicKey) {
        if (publicKey instanceof java.security.interfaces.RSAPublicKey) {
            return ((java.security.interfaces.RSAPublicKey) publicKey).getModulus().bitLength();
        } else if (publicKey instanceof java.security.interfaces.ECPublicKey) {
            return ((java.security.interfaces.ECPublicKey) publicKey).getParams().getOrder().bitLength();
        }
        return -1;
    }
}
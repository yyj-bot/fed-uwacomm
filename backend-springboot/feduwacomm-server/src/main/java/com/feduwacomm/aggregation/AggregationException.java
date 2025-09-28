package com.feduwacomm.aggregation;

/**
 * 聚合异常类
 *
 * 用于表示联邦学习聚合过程中出现的各种异常情况
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public class AggregationException extends Exception {

    public AggregationException(String message) {
        super(message);
    }

    public AggregationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AggregationException(Throwable cause) {
        super(cause);
    }
}
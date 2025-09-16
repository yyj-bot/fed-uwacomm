package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolMessage {
    private ProtocolType type;
    private String id;
    private Instant timestamp;
    private String vmId;
    private Map<String, Object> data;
    private String signature;
} 
package com.feduwacomm.integration;

import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.integration.mock.MockVirtualMachine;
import com.feduwacomm.integration.mock.MockVirtualMachine.AckSimulationEvent;
import com.feduwacomm.integration.mock.MockVirtualMachine.AckSimulationMode;
import com.feduwacomm.integration.mock.VmTestData;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证MockVirtualMachine中新增的ACK模拟功能。
 */
class MockVirtualMachineAckSimulationTest {

    @Test
    @DisplayName("配置ACK超时时应记录超时事件并跳过发送")
    void shouldRecordAckTimeoutEventWhenSimulationConfigured() throws Exception {
        MockVirtualMachine vm = new MockVirtualMachine(buildTestData());
        try {
            vm.clearAckSimulationEvents();
            vm.simulateAckTimeout(ProtocolType.ROUND_START_ACK, Duration.ofMillis(80));

            Map<String, Object> ackData = vm.sendAckForTesting(
                ProtocolType.ROUND_START_ACK,
                Map.of("status", "READY")
            );

            List<AckSimulationEvent> events = vm.getAckSimulationEvents();
            assertThat(events).hasSize(1);

            AckSimulationEvent event = events.get(0);
            assertThat(event.getProtocolType()).isEqualTo(ProtocolType.ROUND_START_ACK);
            assertThat(event.getMode()).isEqualTo(AckSimulationMode.TIMEOUT);
            assertThat(event.getDetail()).contains("delayMillis=");
            assertThat(ackData.get("status")).isEqualTo("READY");
        } finally {
            vm.disconnect();
        }
    }

    @Test
    @DisplayName("配置ACK失败时应注入失败元数据并记录事件")
    void shouldInjectFailureMetadataWhenAckFailureSimulationConfigured() throws Exception {
        MockVirtualMachine vm = new MockVirtualMachine(buildTestData());
        try {
            vm.clearAckSimulationEvents();
            vm.simulateAckFailure(
                ProtocolType.GLOBAL_MODEL_BROADCAST_ACK,
                "FAILED",
                "checksum mismatch"
            );

            Map<String, Object> ackData = vm.sendAckForTesting(
                ProtocolType.GLOBAL_MODEL_BROADCAST_ACK,
                Map.of("status", "MODEL_RECEIVED", "acknowledged", true)
            );

            List<AckSimulationEvent> events = vm.getAckSimulationEvents();
            assertThat(events).hasSize(1);

            AckSimulationEvent event = events.get(0);
            assertThat(event.getProtocolType()).isEqualTo(ProtocolType.GLOBAL_MODEL_BROADCAST_ACK);
            assertThat(event.getMode()).isEqualTo(AckSimulationMode.FAILURE);
            assertThat(event.getDetail())
                .contains("status=FAILED")
                .contains("reason=checksum mismatch");

            assertThat(ackData.get("status")).isEqualTo("FAILED");
            assertThat(ackData.get("acknowledged")).isEqualTo(false);
            assertThat(ackData.get("error")).isEqualTo("checksum mismatch");
            assertThat(ackData).containsEntry("simulatedFailure", true);
        } finally {
            vm.disconnect();
        }
    }

    private VmTestData buildTestData() {
        return new VmTestData(
            null,
            "VM-Ack",
            "127.0.0.1",
            9000,
            8,
            8192,
            0
        );
    }
}

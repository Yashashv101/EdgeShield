package com.shieldgate.service;

import com.shieldgate.dto.ThreatEvent;
import com.shieldgate.model.ThreatLog;
import com.shieldgate.repository.ThreatLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ThreatEventConsumerTest {

    @Mock
    private ThreatLogRepository threatLogRepository;

    @InjectMocks
    private ThreatEventConsumer threatEventConsumer;

    @Test
    void testConsumeThreatEventAndSaveLog() {
        LocalDateTime now = LocalDateTime.now();
        ThreatEvent event = new ThreatEvent("RATE_LIMIT_EXCEEDED", "192.168.1.10", "user1", "/api/v1/orders", now);

        threatEventConsumer.consume(event);

        ArgumentCaptor<ThreatLog> logCaptor = ArgumentCaptor.forClass(ThreatLog.class);
        verify(threatLogRepository, times(1)).save(logCaptor.capture());

        ThreatLog savedLog = logCaptor.getValue();
        assertEquals("RATE_LIMIT_EXCEEDED", savedLog.getThreatType());
        assertEquals("192.168.1.10", savedLog.getSourceIp());
        assertEquals("user1", savedLog.getUsername());
        assertEquals("/api/v1/orders", savedLog.getRequestPath());
        assertEquals(now, savedLog.getTimestamp());
    }
}

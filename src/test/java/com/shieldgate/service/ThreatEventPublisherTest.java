package com.shieldgate.service;

import com.shieldgate.config.RabbitMQConfig;
import com.shieldgate.dto.ThreatEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreatEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ThreatEventPublisher threatEventPublisher;

    @Test
    void testPublishEventSuccessfully() {
        ThreatEvent event = new ThreatEvent("MISSING_JWT", "127.0.0.1", "anonymous", "/api/data");

        threatEventPublisher.publish(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    void testPublishHandlesExceptionGracefully() {
        ThreatEvent event = new ThreatEvent("INVALID_JWT", "127.0.0.1", "anonymous", "/api/data");

        doThrow(new RuntimeException("RabbitMQ connection down"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        // Should not bubble up exception
        assertDoesNotThrow(() -> threatEventPublisher.publish(event));
    }
}

package com.shieldgate.service;

import com.shieldgate.config.RabbitMQConfig;
import com.shieldgate.dto.ThreatEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ThreatEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ThreatEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ThreatEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }
}

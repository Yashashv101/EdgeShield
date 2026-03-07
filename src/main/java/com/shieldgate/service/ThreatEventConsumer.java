package com.shieldgate.service;

import com.shieldgate.config.RabbitMQConfig;
import com.shieldgate.dto.ThreatEvent;
import com.shieldgate.model.ThreatLog;
import com.shieldgate.repository.ThreatLogRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class ThreatEventConsumer {

    private final ThreatLogRepository threatLogRepository;

    public ThreatEventConsumer(ThreatLogRepository threatLogRepository) {
        this.threatLogRepository = threatLogRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consume(ThreatEvent event) {
        ThreatLog log = new ThreatLog();
        log.setThreatType(event.getThreatType());
        log.setSourceIp(event.getSourceIp());
        log.setUsername(event.getUsername());
        log.setRequestPath(event.getRequestPath());
        log.setTimestamp(event.getTimestamp());
        threatLogRepository.save(log);
    }
}

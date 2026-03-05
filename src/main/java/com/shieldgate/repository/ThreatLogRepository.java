package com.shieldgate.repository;

import com.shieldgate.model.ThreatLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThreatLogRepository extends JpaRepository<ThreatLog, Long> {

    List<ThreatLog> findAllByOrderByTimestampDesc();

    List<ThreatLog> findByThreatType(String threatType);

    List<ThreatLog> findBySourceIp(String sourceIp);
}

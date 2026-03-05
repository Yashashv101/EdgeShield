package com.shieldgate.repository;

import com.shieldgate.model.ThreatLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreatLogRepository extends JpaRepository<ThreatLog, Long> {
}

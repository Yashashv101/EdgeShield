package com.shieldgate.controller;

import com.shieldgate.model.ThreatLog;
import com.shieldgate.repository.ThreatLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/threats")
public class AdminController {

    private final ThreatLogRepository threatLogRepository;

    public AdminController(ThreatLogRepository threatLogRepository) {
        this.threatLogRepository = threatLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<ThreatLog>> getAllThreats() {
        return ResponseEntity.ok(threatLogRepository.findAllByOrderByTimestampDesc());
    }

    @GetMapping("/type")
    public ResponseEntity<List<ThreatLog>> getByType(@RequestParam String type) {
        return ResponseEntity.ok(threatLogRepository.findByThreatType(type));
    }

    @GetMapping("/ip")
    public ResponseEntity<List<ThreatLog>> getByIp(@RequestParam String ip) {
        return ResponseEntity.ok(threatLogRepository.findBySourceIp(ip));
    }
}

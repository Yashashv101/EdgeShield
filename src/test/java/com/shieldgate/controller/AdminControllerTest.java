package com.shieldgate.controller;

import com.shieldgate.model.ThreatLog;
import com.shieldgate.repository.ThreatLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ThreatLogRepository threatLogRepository;

    @InjectMocks
    private AdminController adminController;

    @Test
    void testGetAllThreats() {
        ThreatLog log1 = new ThreatLog(1L, "RATE_LIMIT_EXCEEDED", "127.0.0.1", "user1", "/api/test", LocalDateTime.now());
        when(threatLogRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of(log1));

        ResponseEntity<List<ThreatLog>> response = adminController.getAllThreats();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("RATE_LIMIT_EXCEEDED", response.getBody().get(0).getThreatType());
    }

    @Test
    void testGetByType() {
        ThreatLog log1 = new ThreatLog(1L, "MISSING_JWT", "127.0.0.1", "unknown", "/api/secure", LocalDateTime.now());
        when(threatLogRepository.findByThreatType("MISSING_JWT")).thenReturn(List.of(log1));

        ResponseEntity<List<ThreatLog>> response = adminController.getByType("MISSING_JWT");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("MISSING_JWT", response.getBody().get(0).getThreatType());
    }

    @Test
    void testGetByIp() {
        ThreatLog log1 = new ThreatLog(1L, "INVALID_JWT", "192.168.1.5", "unknown", "/api/secure", LocalDateTime.now());
        when(threatLogRepository.findBySourceIp("192.168.1.5")).thenReturn(List.of(log1));

        ResponseEntity<List<ThreatLog>> response = adminController.getByIp("192.168.1.5");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("192.168.1.5", response.getBody().get(0).getSourceIp());
    }
}

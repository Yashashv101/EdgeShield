package com.shieldgate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ThreatEvent implements Serializable {
    private String threatType;
    private String sourceIp;
    private String username;
    private String requestPath;
    private LocalDateTime timestamp;

    public ThreatEvent(String threatType, String sourceIp, String username, String requestPath) {
        this.threatType = threatType;
        this.sourceIp = sourceIp;
        this.username = username;
        this.requestPath = requestPath;
    }
}

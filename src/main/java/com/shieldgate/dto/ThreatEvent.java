package com.shieldgate.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ThreatEvent implements Serializable {

    private String threatType;
    private String sourceIp;
    private String username;
    private String requestPath;
    private LocalDateTime timestamp;

    public ThreatEvent() {}

    public ThreatEvent(String threatType, String sourceIp, String username, String requestPath) {
        this.threatType = threatType;
        this.sourceIp = sourceIp;
        this.username = username;
        this.requestPath = requestPath;
        this.timestamp = LocalDateTime.now();
    }

    public String getThreatType() { return threatType; }
    public void setThreatType(String threatType) { this.threatType = threatType; }

    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

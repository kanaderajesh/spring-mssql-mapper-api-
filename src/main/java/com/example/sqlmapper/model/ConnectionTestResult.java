package com.example.sqlmapper.model;

public class ConnectionTestResult {

    private String connectionId;
    private String status;
    private String message;
    private long responseTimeMs;

    public ConnectionTestResult(String connectionId, String status, String message, long responseTimeMs) {
        this.connectionId = connectionId;
        this.status = status;
        this.message = message;
        this.responseTimeMs = responseTimeMs;
    }

    public String getConnectionId() { return connectionId; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public long getResponseTimeMs() { return responseTimeMs; }
}

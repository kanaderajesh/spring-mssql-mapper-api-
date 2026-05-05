package com.example.sqlmapper.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a database connectivity test")
public class ConnectionTestResult {

    @Schema(description = "The connection ID that was tested", example = "primary-db")
    private String connectionId;

    @Schema(description = "UP if the connection succeeded, DOWN if it failed", example = "UP",
            allowableValues = {"UP", "DOWN"})
    private String status;

    @Schema(description = "Success confirmation or the driver error message",
            example = "Connection successful")
    private String message;

    @Schema(description = "Round-trip time for the SELECT 1 probe in milliseconds", example = "12")
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

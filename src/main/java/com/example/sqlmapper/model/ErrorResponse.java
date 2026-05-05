package com.example.sqlmapper.model;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Structured error response body returned for all non-2xx responses")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "HTTP status reason phrase", example = "Not Found")
    private String error;

    @Schema(description = "Detail message explaining the error", example = "Query id 'unknown' is not configured")
    private String message;

    @Schema(description = "Request path that triggered the error", example = "/api/query/primary-db/unknown")
    private String path;

    @Schema(description = "UTC timestamp when the error occurred", example = "2024-05-01T10:00:00Z")
    private Instant timestamp;

    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public Instant getTimestamp() { return timestamp; }
}

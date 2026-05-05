package com.example.sqlmapper.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered database connection entry")
public class ConnectionInfo {

    @Schema(description = "Unique connection ID as defined in application.yaml", example = "primary-db")
    private String id;

    public ConnectionInfo(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}

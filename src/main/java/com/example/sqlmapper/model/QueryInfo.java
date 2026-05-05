package com.example.sqlmapper.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered SQL query entry")
public class QueryInfo {

    @Schema(description = "Unique query ID as defined in application.yaml", example = "get-all-users")
    private String id;

    @Schema(description = "Human-readable description of what the query returns", example = "Retrieve all users")
    private String description;

    public QueryInfo(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}

package com.example.sqlmapper.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sqlmapper.model.ConnectionInfo;
import com.example.sqlmapper.model.ConnectionTestResult;
import com.example.sqlmapper.model.ErrorResponse;
import com.example.sqlmapper.model.QueryInfo;
import com.example.sqlmapper.service.QueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    // -------------------------------------------------------------------------
    // Connections
    // -------------------------------------------------------------------------

    @Tag(name = "Connections", description = "Manage and test configured database connections")
    @Operation(
            summary = "List all registered connections",
            description = "Returns every database connection ID currently registered in application.yaml.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connection list returned successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ConnectionInfo.class)),
                            examples = @ExampleObject(value = """
                                    [
                                      { "id": "primary-db" },
                                      { "id": "analytics-db" }
                                    ]""")))
    })
    @GetMapping("/connections")
    public ResponseEntity<List<ConnectionInfo>> listConnections() {
        return ResponseEntity.ok(queryService.listConnections());
    }

    @Tag(name = "Connections")
    @Operation(
            summary = "Test a database connection",
            description = """
                    Executes `SELECT 1` against the target connection and reports whether it is reachable. \
                    Returns **200** when the connection is healthy and **503** when it is not, \
                    so HTTP-level monitors that only check the status code also work correctly.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connection is reachable (UP)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConnectionTestResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "connectionId": "primary-db",
                                      "status": "UP",
                                      "message": "Connection successful",
                                      "responseTimeMs": 12
                                    }"""))),
            @ApiResponse(responseCode = "503", description = "Connection is unreachable (DOWN)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConnectionTestResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "connectionId": "analytics-db",
                                      "status": "DOWN",
                                      "message": "The TCP/IP connection to the host analytics-host, port 1433 has failed.",
                                      "responseTimeMs": 5032
                                    }"""))),
            @ApiResponse(responseCode = "404", description = "Connection ID is not configured",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "Connection id 'unknown-db' is not configured",
                                      "path": "/api/connections/unknown-db/test",
                                      "timestamp": "2024-05-01T10:00:00Z"
                                    }""")))
    })
    @GetMapping("/connections/{connectionId}/test")
    public ResponseEntity<ConnectionTestResult> testConnection(
            @Parameter(description = "Database connection ID as defined in application.yaml", example = "primary-db", required = true)
            @PathVariable String connectionId) {

        ConnectionTestResult result = queryService.testConnection(connectionId);
        HttpStatus status = "UP".equals(result.getStatus()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(result);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Tag(name = "Queries", description = "List and execute configured SQL queries")
    @Operation(
            summary = "List all registered queries",
            description = "Returns every query ID and its description as configured in application.yaml.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Query list returned successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = QueryInfo.class)),
                            examples = @ExampleObject(value = """
                                    [
                                      { "id": "get-all-users",     "description": "Retrieve all users" },
                                      { "id": "get-active-users",  "description": "Retrieve all active users" },
                                      { "id": "get-all-products",  "description": "Retrieve all products" },
                                      { "id": "get-active-orders", "description": "Retrieve all active orders" }
                                    ]""")))
    })
    @GetMapping("/queries")
    public ResponseEntity<List<QueryInfo>> listQueries() {
        return ResponseEntity.ok(queryService.listQueries());
    }

    @Tag(name = "Queries")
    @Operation(
            summary = "Execute a configured query",
            description = """
                    Runs the SQL registered under `queryId` against the database identified by `connectionId`. \
                    Optionally filters the result set to the columns listed in `fields` \
                    (comma-separated, case-insensitive). All rows are returned as a JSON array of objects.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Query executed successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(type = "object",
                                    description = "One object per row; keys are column names")),
                            examples = {
                                    @ExampleObject(name = "All columns",
                                            value = """
                                                    [
                                                      { "id": 1, "name": "Alice Smith", "email": "alice@example.com", "created_at": "2024-01-15T10:30:00" },
                                                      { "id": 2, "name": "Bob Jones",   "email": "bob@example.com",   "created_at": "2024-02-20T08:00:00" }
                                                    ]"""),
                                    @ExampleObject(name = "Filtered columns (?fields=id,email)",
                                            value = """
                                                    [
                                                      { "id": 1, "email": "alice@example.com" },
                                                      { "id": 2, "email": "bob@example.com" }
                                                    ]""")
                            })),
            @ApiResponse(responseCode = "404", description = "Connection ID or query ID is not configured",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "Query id 'unknown-query' is not configured",
                                      "path": "/api/query/primary-db/unknown-query",
                                      "timestamp": "2024-05-01T10:00:00Z"
                                    }"""))),
            @ApiResponse(responseCode = "500", description = "Database error while executing the query",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 500,
                                      "error": "Database Error",
                                      "message": "Invalid object name 'users'.",
                                      "path": "/api/query/primary-db/get-all-users",
                                      "timestamp": "2024-05-01T10:00:01Z"
                                    }""")))
    })
    @GetMapping("/query/{connectionId}/{queryId}")
    public ResponseEntity<List<Map<String, Object>>> executeQuery(
            @Parameter(description = "Database connection ID as defined in application.yaml",
                    example = "primary-db", required = true)
            @PathVariable String connectionId,

            @Parameter(description = "Query ID as defined in application.yaml",
                    example = "get-all-users", required = true)
            @PathVariable String queryId,

            @Parameter(description = "Comma-separated list of column names to include in the response. "
                    + "Omit to return all columns. Matching is case-insensitive.",
                    example = "id,name,email")
            @RequestParam(required = false) String fields) {

        List<Map<String, Object>> result = queryService.executeQuery(connectionId, queryId, fields);
        return ResponseEntity.ok(result);
    }
}

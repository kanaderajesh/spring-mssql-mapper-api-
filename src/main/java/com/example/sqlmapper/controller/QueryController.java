package com.example.sqlmapper.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sqlmapper.model.ConnectionInfo;
import com.example.sqlmapper.model.QueryInfo;
import com.example.sqlmapper.service.QueryService;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * List all configured connection IDs.
     * GET /api/connections
     */
    @GetMapping("/connections")
    public ResponseEntity<List<ConnectionInfo>> listConnections() {
        return ResponseEntity.ok(queryService.listConnections());
    }

    /**
     * List all configured query IDs and their descriptions.
     * GET /api/queries
     */
    @GetMapping("/queries")
    public ResponseEntity<List<QueryInfo>> listQueries() {
        return ResponseEntity.ok(queryService.listQueries());
    }

    /**
     * Execute a configured query against a specific database connection.
     * GET /api/query/{connectionId}/{queryId}?fields=col1,col2
     *
     * @param connectionId the database connection ID defined in application.yaml
     * @param queryId      the query ID defined in application.yaml
     * @param fields       optional comma-separated column names to include in the response
     */
    @GetMapping("/query/{connectionId}/{queryId}")
    public ResponseEntity<List<Map<String, Object>>> executeQuery(
            @PathVariable String connectionId,
            @PathVariable String queryId,
            @RequestParam(required = false) String fields) {

        List<Map<String, Object>> result = queryService.executeQuery(connectionId, queryId, fields);
        return ResponseEntity.ok(result);
    }
}

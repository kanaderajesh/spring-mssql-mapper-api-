package com.example.sqlmapper.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.sqlmapper.config.DataSourceRegistry;
import com.example.sqlmapper.config.QueryProperties;
import com.example.sqlmapper.model.ConnectionInfo;
import com.example.sqlmapper.model.ConnectionTestResult;
import com.example.sqlmapper.model.QueryInfo;

@Service
public class QueryService {

    private final DataSourceRegistry dataSourceRegistry;
    private final QueryProperties queryProperties;

    public QueryService(DataSourceRegistry dataSourceRegistry, QueryProperties queryProperties) {
        this.dataSourceRegistry = dataSourceRegistry;
        this.queryProperties = queryProperties;
    }

    public List<Map<String, Object>> executeQuery(String connectionId, String queryId, String fields) {
        JdbcTemplate jdbcTemplate = dataSourceRegistry.get(connectionId);

        QueryProperties.QueryDefinition definition = queryProperties.getDefinitions().get(queryId);
        if (definition == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Query id '" + queryId + "' is not configured");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(definition.getSql());

        if (fields != null && !fields.isBlank()) {
            Set<String> requested = Arrays.stream(fields.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            return rows.stream()
                    .map(row -> row.entrySet().stream()
                            .filter(e -> requested.contains(e.getKey().toLowerCase()))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> e.getValue() != null ? e.getValue() : "",
                                    (a, b) -> a,
                                    LinkedHashMap::new)))
                    .collect(Collectors.toList());
        }

        return rows;
    }

    public List<QueryInfo> listQueries() {
        return queryProperties.getDefinitions().entrySet().stream()
                .map(e -> new QueryInfo(e.getKey(), e.getValue().getDescription()))
                .collect(Collectors.toList());
    }

    public List<ConnectionInfo> listConnections() {
        return dataSourceRegistry.getConnectionIds().stream()
                .map(ConnectionInfo::new)
                .collect(Collectors.toList());
    }

    public ConnectionTestResult testConnection(String connectionId) {
        JdbcTemplate jdbcTemplate = dataSourceRegistry.get(connectionId);
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long elapsed = System.currentTimeMillis() - start;
            return new ConnectionTestResult(connectionId, "UP", "Connection successful", elapsed);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            return new ConnectionTestResult(connectionId, "DOWN", cause.getMessage(), elapsed);
        }
    }
}

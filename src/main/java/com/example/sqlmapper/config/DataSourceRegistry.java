package com.example.sqlmapper.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Component
public class DataSourceRegistry implements DisposableBean {

    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private final Map<String, JdbcTemplate> jdbcTemplates = new ConcurrentHashMap<>();

    public DataSourceRegistry(DatabaseProperties props) {
        props.getConnections().forEach((id, config) -> {
            HikariConfig hc = new HikariConfig();
            hc.setPoolName("pool-" + id);
            hc.setJdbcUrl(config.getUrl());
            hc.setUsername(config.getUsername());
            hc.setPassword(config.getPassword());
            if (config.getDriverClassName() != null) {
                hc.setDriverClassName(config.getDriverClassName());
            }
            HikariDataSource ds = new HikariDataSource(hc);
            dataSources.put(id, ds);
            jdbcTemplates.put(id, new JdbcTemplate(ds));
        });
    }

    public JdbcTemplate get(String connectionId) {
        JdbcTemplate jt = jdbcTemplates.get(connectionId);
        if (jt == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Connection id '" + connectionId + "' is not configured");
        }
        return jt;
    }

    public Set<String> getConnectionIds() {
        return Collections.unmodifiableSet(jdbcTemplates.keySet());
    }

    @Override
    public void destroy() {
        dataSources.values().forEach(HikariDataSource::close);
    }
}

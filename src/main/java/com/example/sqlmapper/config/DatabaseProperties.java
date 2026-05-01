package com.example.sqlmapper.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "databases")
public class DatabaseProperties {

    private Map<String, ConnectionConfig> connections = new LinkedHashMap<>();

    public Map<String, ConnectionConfig> getConnections() {
        return connections;
    }

    public void setConnections(Map<String, ConnectionConfig> connections) {
        this.connections = connections;
    }

    public static class ConnectionConfig {

        private String url;
        private String username;
        private String password;
        private String driverClassName;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    }
}

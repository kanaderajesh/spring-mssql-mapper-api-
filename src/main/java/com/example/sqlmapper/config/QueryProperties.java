package com.example.sqlmapper.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queries")
public class QueryProperties {

    private Map<String, QueryDefinition> definitions = new LinkedHashMap<>();

    public Map<String, QueryDefinition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String, QueryDefinition> definitions) {
        this.definitions = definitions;
    }

    public static class QueryDefinition {

        private String sql;
        private String description;

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}

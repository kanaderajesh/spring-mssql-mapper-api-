package com.example.sqlmapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.sqlmapper.config.QueryProperties;

@SpringBootApplication
@EnableConfigurationProperties(QueryProperties.class)
public class SqlMapperApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlMapperApplication.class, args);
    }
}

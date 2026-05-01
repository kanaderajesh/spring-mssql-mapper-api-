package com.example.sqlmapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.sqlmapper.config.DatabaseProperties;
import com.example.sqlmapper.config.QueryProperties;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class
})
@EnableConfigurationProperties({QueryProperties.class, DatabaseProperties.class})
public class SqlMapperApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlMapperApplication.class, args);
    }
}

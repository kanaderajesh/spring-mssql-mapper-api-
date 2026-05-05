package com.example.sqlmapper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring MSSQL Mapper API")
                        .description("""
                                REST API that executes pre-configured SQL queries against one or more \
                                Microsoft SQL Server databases and returns results as JSON. \
                                Database connections and SQL queries are declared in application.yaml \
                                — no code changes are needed to add either.""")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com")));
    }
}

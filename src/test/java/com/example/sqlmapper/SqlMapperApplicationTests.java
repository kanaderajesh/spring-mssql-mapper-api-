package com.example.sqlmapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "queries.definitions.test-query.sql=SELECT 1 AS id",
    "queries.definitions.test-query.description=Test query"
})
class SqlMapperApplicationTests {

    @Test
    void contextLoads() {
    }
}

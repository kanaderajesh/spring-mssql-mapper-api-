package com.example.sqlmapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "databases.connections.test-db.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "databases.connections.test-db.driver-class-name=org.h2.Driver",
    "databases.connections.test-db.username=sa",
    "databases.connections.test-db.password=",
    "queries.definitions.test-query.sql=SELECT 1 AS id",
    "queries.definitions.test-query.description=Test query"
})
class SqlMapperApplicationTests {

    @Test
    void contextLoads() {
    }
}

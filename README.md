# Spring MSSQL Mapper API

A Spring Boot REST API that executes pre-configured SQL queries against one or more Microsoft SQL Server databases and returns results as JSON. Both database connections and SQL queries are declared entirely in `application.yaml` — no code changes are needed to add either.

---

## How It Works

1. Database connections are registered in `application.yaml` under `databases.connections`, each with a unique connection ID.
2. SQL queries are registered under `queries.definitions`, each with a unique query ID.
3. A client calls a REST endpoint with both a **connection ID** and a **query ID**, plus an optional comma-separated list of column names to return.
4. The service resolves the correct `DataSource` by connection ID, looks up the SQL by query ID, executes it via JDBC, filters columns (if requested), and returns the rows as a JSON array.

```
Client
  │
  │  GET /api/query/{connectionId}/{queryId}?fields=col1,col2
  ▼
QueryController
  │
  ├── looks up JdbcTemplate by connectionId ──▶  DataSourceRegistry
  │                                                   │
  │                                                   └── DatabaseProperties (application.yaml)
  │                                                         databases.connections.*
  │
  ├── looks up SQL by queryId ──────────────▶  QueryProperties (application.yaml)
  │                                                   queries.definitions.*
  │
  │  JdbcTemplate.queryForList(sql)
  ▼
Target MSSQL Database
  │
  ▼
Column filter (optional, case-insensitive)
  │
  ▼
JSON Response  [ { col1: ..., col2: ... }, ... ]
```

---

## Project Structure

```
sql-mapper-api/
├── pom.xml
└── src/
    └── main/
        ├── java/com/example/sqlmapper/
        │   ├── SqlMapperApplication.java              # Entry point; disables DataSource auto-config
        │   ├── config/
        │   │   ├── DatabaseProperties.java            # Binds databases.connections from YAML
        │   │   ├── DataSourceRegistry.java            # Builds and holds one JdbcTemplate per connection
        │   │   └── QueryProperties.java               # Binds queries.definitions from YAML
        │   ├── controller/
        │   │   └── QueryController.java               # REST endpoints
        │   ├── service/
        │   │   └── QueryService.java                  # Resolves connection, executes query, filters columns
        │   ├── model/
        │   │   ├── ConnectionInfo.java                # Connection ID DTO
        │   │   ├── QueryInfo.java                     # Query ID + description DTO
        │   │   └── ErrorResponse.java                 # Structured error body
        │   └── exception/
        │       └── GlobalExceptionHandler.java        # JSON error responses
        └── resources/
            └── application.yaml                       # All connections + query definitions
```

---

## Tech Stack

| Component     | Technology                       |
|--------------|-----------------------------------|
| Framework     | Spring Boot 3.2.5                |
| Language      | Java 17                          |
| Database      | Microsoft SQL Server             |
| JDBC          | Spring JDBC / JdbcTemplate       |
| Connection pool | HikariCP (bundled with Spring Boot) |
| Build tool    | Maven                            |

---

## Configuration

All configuration lives in `src/main/resources/application.yaml`.

### Registering database connections

Each entry under `databases.connections` gets its own connection pool. The map key is the **connection ID** used in the URL.

```yaml
databases:
  connections:
    primary-db:
      url: jdbc:sqlserver://localhost:1433;databaseName=testdb;encrypt=false;trustServerCertificate=true
      username: sa
      password: YourPassword123
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver

    analytics-db:
      url: jdbc:sqlserver://analytics-host:1433;databaseName=analyticsdb;encrypt=false;trustServerCertificate=true
      username: sa
      password: YourPassword456
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```

### Registering SQL queries

Each entry under `queries.definitions` is available to **all** connections. The map key is the **query ID** used in the URL.

```yaml
queries:
  definitions:
    get-all-users:
      sql: "SELECT id, name, email, created_at FROM users"
      description: "Retrieve all users"

    get-active-users:
      sql: "SELECT id, name, email FROM users WHERE active = 1"
      description: "Retrieve all active users"

    get-all-products:
      sql: "SELECT id, name, price, category, stock FROM products"
      description: "Retrieve all products"

    get-active-orders:
      sql: "SELECT id, customer_id, total_amount, status, created_at FROM orders WHERE status = 'ACTIVE'"
      description: "Retrieve all active orders"
```

No code changes are needed after adding connections or queries — just restart the service.

---

## Running the Service

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

---

## API Endpoints

### 1. List all registered connections

Returns all configured database connection IDs.

```
GET /api/connections
```

#### curl

```bash
curl -s http://localhost:8080/api/connections
```

#### Response `200 OK`

```json
[
  { "id": "primary-db" },
  { "id": "analytics-db" }
]
```

---

### 2. List all registered queries

Returns all configured query IDs and their descriptions.

```
GET /api/queries
```

#### curl

```bash
curl -s http://localhost:8080/api/queries
```

#### Response `200 OK`

```json
[
  { "id": "get-all-users",     "description": "Retrieve all users" },
  { "id": "get-active-users",  "description": "Retrieve all active users" },
  { "id": "get-all-products",  "description": "Retrieve all products" },
  { "id": "get-active-orders", "description": "Retrieve all active orders" }
]
```

---

### 3. Execute a query — all columns

Runs the SQL registered under `{queryId}` on the database identified by `{connectionId}` and returns every column.

```
GET /api/query/{connectionId}/{queryId}
```

| Parameter      | Type       | Required | Description                                    |
|----------------|------------|----------|------------------------------------------------|
| `connectionId` | path param | Yes      | Database connection ID from `application.yaml` |
| `queryId`      | path param | Yes      | Query ID from `application.yaml`               |

#### curl

```bash
curl -s http://localhost:8080/api/query/primary-db/get-all-users
```

#### Response `200 OK`

```json
[
  {
    "id": 1,
    "name": "Alice Smith",
    "email": "alice@example.com",
    "created_at": "2024-01-15T10:30:00"
  },
  {
    "id": 2,
    "name": "Bob Jones",
    "email": "bob@example.com",
    "created_at": "2024-02-20T08:00:00"
  }
]
```

#### curl — same query on a different database

```bash
curl -s http://localhost:8080/api/query/analytics-db/get-all-users
```

---

### 4. Execute a query — selected columns only

Same as above but the response is filtered to the columns listed in `fields`. Column matching is **case-insensitive**.

```
GET /api/query/{connectionId}/{queryId}?fields=col1,col2,...
```

| Parameter      | Type        | Required | Description                                          |
|----------------|-------------|----------|------------------------------------------------------|
| `connectionId` | path param  | Yes      | Database connection ID from `application.yaml`       |
| `queryId`      | path param  | Yes      | Query ID from `application.yaml`                     |
| `fields`       | query param | No       | Comma-separated column names to include in the response |

#### curl — single field

```bash
curl -s "http://localhost:8080/api/query/primary-db/get-all-users?fields=name"
```

#### Response `200 OK`

```json
[
  { "name": "Alice Smith" },
  { "name": "Bob Jones" }
]
```

#### curl — multiple fields

```bash
curl -s "http://localhost:8080/api/query/primary-db/get-all-users?fields=id,email"
```

#### Response `200 OK`

```json
[
  { "id": 1, "email": "alice@example.com" },
  { "id": 2, "email": "bob@example.com" }
]
```

#### curl — products from analytics DB, specific columns

```bash
curl -s "http://localhost:8080/api/query/analytics-db/get-all-products?fields=name,price,category"
```

#### Response `200 OK`

```json
[
  { "name": "Widget A", "price": 9.99,  "category": "Hardware" },
  { "name": "Gadget B", "price": 24.50, "category": "Electronics" }
]
```

---

## Error Responses

All errors are returned as a structured JSON body.

### 404 — Connection ID not found

```bash
curl -s http://localhost:8080/api/query/unknown-db/get-all-users
```

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Connection id 'unknown-db' is not configured",
  "path": "/api/query/unknown-db/get-all-users",
  "timestamp": "2024-05-01T10:00:00Z"
}
```

### 404 — Query ID not found

```bash
curl -s http://localhost:8080/api/query/primary-db/does-not-exist
```

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Query id 'does-not-exist' is not configured",
  "path": "/api/query/primary-db/does-not-exist",
  "timestamp": "2024-05-01T10:00:00Z"
}
```

### 500 — Database error

```json
{
  "status": 500,
  "error": "Database Error",
  "message": "Invalid object name 'users'.",
  "path": "/api/query/primary-db/get-all-users",
  "timestamp": "2024-05-01T10:00:01Z"
}
```

---

## Design Decisions

- **One HikariCP pool per connection ID** — `DataSourceRegistry` builds a `HikariDataSource` for each entry in `databases.connections` at startup and holds a corresponding `JdbcTemplate`. Pools are closed gracefully on shutdown via `DisposableBean`.
- **Spring DataSource auto-config is disabled** — because the service manages its own `DataSource` beans, `DataSourceAutoConfiguration`, `JdbcTemplateAutoConfiguration`, and `DataSourceTransactionManagerAutoConfiguration` are excluded so Spring Boot doesn't try to create a default single-datasource context.
- **Queries are connection-agnostic** — a query defined once in `queries.definitions` can be run against any registered connection, useful for running the same query across environments (e.g. prod vs reporting replica).
- **No ORM / entity classes** — `JdbcTemplate.queryForList()` returns `List<Map<String, Object>>` which serialises directly to JSON without needing per-table model classes.
- **Column filtering in Java, not SQL** — the full configured query is executed as-is; columns are stripped from the result map in the service layer, avoiding any dynamic SQL construction.
- **Case-insensitive field matching** — both the requested field names and the JDBC column names are lowercased before comparison, so `?fields=Email` matches `email`, `EMAIL`, or `Email`.
- **Centralised error handling** — `GlobalExceptionHandler` converts `ResponseStatusException` (unknown connection/query ID), `DataAccessException` (JDBC failures), and unexpected exceptions into a consistent JSON error shape.

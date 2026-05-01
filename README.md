# Spring MSSQL Mapper API

A Spring Boot REST API that executes pre-configured SQL queries against a Microsoft SQL Server database and returns results as JSON. No code changes are needed to add or modify queries — everything is declared in `application.yaml`.

---

## How It Works

1. SQL queries are registered in `application.yaml`, each with a unique ID and an optional description.
2. A client calls a REST endpoint with the query ID and, optionally, a comma-separated list of column names to return.
3. The service looks up the SQL by ID, executes it via JDBC against MSSQL, filters the result set to the requested columns (if specified), and returns the rows as a JSON array.

```
Client
  │
  │  GET /api/query/{queryId}?fields=col1,col2
  ▼
QueryController
  │
  ▼
QueryService  ──── looks up SQL by ID ────▶  QueryProperties (application.yaml)
  │
  │  JdbcTemplate.queryForList(sql)
  ▼
MSSQL Database
  │
  ▼
Column filter (optional)
  │
  ▼
JSON Response [ { col1: ..., col2: ... }, ... ]
```

---

## Project Structure

```
sql-mapper-api/
├── pom.xml
└── src/
    └── main/
        ├── java/com/example/sqlmapper/
        │   ├── SqlMapperApplication.java          # Entry point
        │   ├── config/
        │   │   └── QueryProperties.java           # Binds queries.definitions from YAML
        │   ├── controller/
        │   │   └── QueryController.java           # REST endpoints
        │   ├── service/
        │   │   └── QueryService.java              # JDBC execution + column filtering
        │   ├── model/
        │   │   ├── QueryInfo.java                 # Query ID + description DTO
        │   │   └── ErrorResponse.java             # Structured error body
        │   └── exception/
        │       └── GlobalExceptionHandler.java    # JSON error responses
        └── resources/
            └── application.yaml                   # DB config + query definitions
```

---

## Tech Stack

| Component        | Technology                          |
|-----------------|--------------------------------------|
| Framework        | Spring Boot 3.2.5                   |
| Language         | Java 17                             |
| Database         | Microsoft SQL Server                |
| JDBC             | Spring JDBC / JdbcTemplate          |
| Build tool       | Maven                               |

---

## Configuration

### Database connection

Edit `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=testdb;encrypt=false;trustServerCertificate=true
    username: sa
    password: YourPassword123
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```

### Registering SQL queries

Add entries under `queries.definitions`. Each key is the **query ID** used in the URL.

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

No code changes are needed after adding a new query — just restart the service.

---

## Running the Service

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

---

## API Endpoints

### 1. List all registered queries

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
  {
    "id": "get-all-users",
    "description": "Retrieve all users"
  },
  {
    "id": "get-active-users",
    "description": "Retrieve all active users"
  },
  {
    "id": "get-all-products",
    "description": "Retrieve all products"
  },
  {
    "id": "get-active-orders",
    "description": "Retrieve all active orders"
  }
]
```

---

### 2. Execute a query — all columns

Runs the SQL registered under `{queryId}` and returns every column.

```
GET /api/query/{queryId}
```

| Parameter  | Type        | Required | Description                         |
|------------|-------------|----------|-------------------------------------|
| `queryId`  | path param  | Yes      | The ID defined in `application.yaml` |

#### curl

```bash
curl -s http://localhost:8080/api/query/get-all-users
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

---

### 3. Execute a query — selected columns only

Same as above but the response is filtered to the columns listed in `fields`. Column matching is **case-insensitive**.

```
GET /api/query/{queryId}?fields=col1,col2,...
```

| Parameter  | Type         | Required | Description                                          |
|------------|--------------|----------|------------------------------------------------------|
| `queryId`  | path param   | Yes      | The ID defined in `application.yaml`                 |
| `fields`   | query param  | No       | Comma-separated column names to include in the response |

#### curl — single field

```bash
curl -s "http://localhost:8080/api/query/get-all-users?fields=name"
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
curl -s "http://localhost:8080/api/query/get-all-users?fields=id,email"
```

#### Response `200 OK`

```json
[
  { "id": 1, "email": "alice@example.com" },
  { "id": 2, "email": "bob@example.com" }
]
```

#### curl — products with price and category

```bash
curl -s "http://localhost:8080/api/query/get-all-products?fields=name,price,category"
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

### 404 — Query ID not found

```bash
curl -s http://localhost:8080/api/query/does-not-exist
```

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Query id 'does-not-exist' is not configured",
  "path": "/api/query/does-not-exist",
  "timestamp": "2024-05-01T10:00:00Z"
}
```

### 500 — Database error

```json
{
  "status": 500,
  "error": "Database Error",
  "message": "Invalid object name 'users'.",
  "path": "/api/query/get-all-users",
  "timestamp": "2024-05-01T10:00:01Z"
}
```

---

## Design Decisions

- **No ORM / entity classes** — `JdbcTemplate.queryForList()` returns `List<Map<String, Object>>` which serialises directly to JSON without needing model classes per table.
- **Column filtering in Java, not SQL** — the full query runs as written in the YAML; columns are stripped from the result map in the service layer. This keeps the YAML queries simple and avoids dynamic SQL construction.
- **Case-insensitive field matching** — both the requested field names and the column names from the result set are lowercased before comparison, so `?fields=Email` matches a column named `email` or `EMAIL`.
- **Centralised error handling** — `GlobalExceptionHandler` catches `ResponseStatusException` (unknown query ID), `DataAccessException` (JDBC failures), and any other exception, converting them all to a consistent JSON error shape.

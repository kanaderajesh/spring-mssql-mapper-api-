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

#### SQL Server authentication (username + password)

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

#### Named instance (`server\instance`)

When SQL Server is running as a named instance rather than the default instance, use `instanceName` in the URL **instead of a port number**. The SQL Server Browser service must be running on the host so the driver can resolve the instance to a dynamic port.

```yaml
databases:
  connections:
    named-instance-db:
      url: jdbc:sqlserver://corp-sql-server;instanceName=SQLEXPRESS;databaseName=testdb;encrypt=false;trustServerCertificate=true
      username: sa
      password: YourPassword123
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```

> **Port 1434 (UDP) must be open** — the JDBC driver contacts the SQL Server Browser on UDP 1434 to discover the dynamic TCP port assigned to the named instance. If a firewall blocks this, specify the port explicitly instead:
>
> ```yaml
> url: jdbc:sqlserver://corp-sql-server:52918;databaseName=testdb;encrypt=false;trustServerCertificate=true
> ```
>
> Run `netstat -ano | findstr LISTENING` on the server (or check SQL Server Configuration Manager) to find the port assigned to the instance.

Named instances also work with Windows Authentication:

```yaml
databases:
  connections:
    named-instance-windows-auth:
      url: jdbc:sqlserver://corp-sql-server;instanceName=SQLEXPRESS;databaseName=testdb;integratedSecurity=true;encrypt=false;trustServerCertificate=true
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```

---

#### Windows Authentication (Integrated Security)

Add `integratedSecurity=true` to the URL and omit `username` / `password`. The connection uses the Windows identity of the process that runs the JVM — no credentials are stored in the config file.

```yaml
databases:
  connections:
    corp-db:
      url: jdbc:sqlserver://corp-sql-server:1433;databaseName=corpdb;integratedSecurity=true;encrypt=false;trustServerCertificate=true
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```

> **Native library required** — Windows Authentication relies on a native DLL that ships inside the MSSQL JDBC jar but must be extracted and placed on the Java library path before the JVM starts.
>
> 1. Locate `mssql-jdbc_auth-<version>-x64.dll` inside the jar (path: `auth/x64/`), or download it from the [Microsoft JDBC driver releases](https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server).
> 2. Copy the DLL to a folder, e.g. `C:\app\lib\`.
> 3. Start the service with the library path set:
>
> ```bash
> ./mvnw spring-boot:run -Djava.library.path="C:\app\lib"
> ```
>
> Or set it permanently in `JAVA_TOOL_OPTIONS`:
>
> ```bash
> set JAVA_TOOL_OPTIONS=-Djava.library.path=C:\app\lib
> ```
>
> Without the DLL the driver throws:
> `This driver is not configured for integrated authentication.`

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

#### Multi-line queries with JOIN

For complex queries use the YAML **literal block scalar** (`|`). Each line of the SQL is indented under the `sql` key and the newlines are preserved exactly as written.

```yaml
queries:
  definitions:
    get-orders-with-customer:
      description: "Orders joined with customer and product details"
      sql: |
        SELECT
            o.id            AS order_id,
            o.status        AS order_status,
            o.total_amount,
            o.created_at    AS order_date,
            c.id            AS customer_id,
            c.name          AS customer_name,
            c.email         AS customer_email,
            p.id            AS product_id,
            p.name          AS product_name,
            p.price         AS unit_price
        FROM orders o
        INNER JOIN customers c
            ON o.customer_id = c.id
        INNER JOIN order_items oi
            ON oi.order_id = o.id
        INNER JOIN products p
            ON oi.product_id = p.id
        WHERE o.status = 'ACTIVE'
        ORDER BY o.created_at DESC
```

Calling this query with optional column filtering:

```bash
# All columns
curl -s http://localhost:8080/api/query/primary-db/get-orders-with-customer

# Only customer and order summary columns
curl -s "http://localhost:8080/api/query/primary-db/get-orders-with-customer?fields=order_id,customer_name,total_amount,order_date"
```

Response for the filtered call:

```json
[
  {
    "order_id": 101,
    "customer_name": "Alice Smith",
    "total_amount": 149.99,
    "order_date": "2024-04-30T09:15:00"
  },
  {
    "order_id": 98,
    "customer_name": "Bob Jones",
    "total_amount": 59.50,
    "order_date": "2024-04-28T14:00:00"
  }
]
```

No code changes are needed after adding connections or queries — just restart the service.

---

## Running on Linux with Kerberos Authentication

Kerberos lets a Linux service account connect to SQL Server using Active Directory tickets — no password stored anywhere in the config. The MSSQL JDBC driver ships a pure-Java Kerberos implementation (`authenticationScheme=JavaKerberos`) so **no native DLL is needed on Linux**, unlike the Windows Integrated Security path.

---

### How the JDBC driver resolves a Kerberos connection

```
Application (JVM)
  │
  │  DataSource.getConnection()
  ▼
MSSQL JDBC Driver
  │  sees: integratedSecurity=true;authenticationScheme=JavaKerberos
  │
  │  1. Calls JAAS with login context "SQLJDBCDriver"
  ▼
JAAS / Krb5LoginModule
  │  Reads /etc/krb5.conf to locate the KDC
  │
  ├─── keytab mode (service account) ──────────────────────────────┐
  │    reads keytab file, decrypts principal's long-term key        │
  │    sends AS-REQ to KDC                                          │
  │                                                                 │
  └─── ticket-cache mode (kinit) ───────────────────────────────── ┘
       reads existing TGT from /tmp/krb5cc_<uid>
  │
  ▼
KDC (Active Directory Domain Controller) — AS exchange
  │  validates the keytab key / cached TGT
  │  issues a Ticket Granting Ticket (TGT) for the service principal
  │
  ▼
MSSQL JDBC Driver — TGS exchange
  │  presents TGT to KDC and requests a Service Ticket for
  │  MSSQLSvc/<sql-server-host>:<port>@REALM
  │
  ▼
KDC
  │  issues Service Ticket encrypted with SQL Server's secret key
  │
  ▼
MSSQL JDBC Driver — TDS login
  │  sends Service Ticket inside the TDS pre-login / login7 packet
  │  to SQL Server on port 1433
  │
  ▼
SQL Server
  │  decrypts the Service Ticket using its own keytab / AD password
  │  verifies the ticket, extracts the client's AD identity
  │  applies SQL Server login permissions for that AD account
  │
  ▼
Connection established — queries run as the service account identity
```

---

### Step-by-step setup

#### 1. Install Kerberos client tools

```bash
# RHEL / CentOS / Fedora
sudo yum install -y krb5-workstation

# Ubuntu / Debian
sudo apt-get install -y krb5-user
```

#### 2. Configure `/etc/krb5.conf`

Replace `DOMAIN.COM`, `dc1.domain.com`, and `dc2.domain.com` with your AD values.

```ini
[libdefaults]
    default_realm     = DOMAIN.COM
    dns_lookup_kdc    = true
    dns_lookup_realm  = false
    forwardable       = true
    renewable         = true

[realms]
    DOMAIN.COM = {
        kdc          = dc1.domain.com
        kdc          = dc2.domain.com
        admin_server = dc1.domain.com
    }

[domain_realm]
    .domain.com = DOMAIN.COM
    domain.com  = DOMAIN.COM
```

Verify it works before proceeding:

```bash
kinit svc-spring@DOMAIN.COM
klist
```

#### 3. Create a keytab for the service account

Run this on a Windows Domain Controller (or delegate it to your AD team). The keytab lets the Linux service authenticate without ever storing a plain-text password.

```powershell
# On the Windows DC
ktpass `
  -out svc-spring.keytab `
  -mapuser svc-spring@DOMAIN.COM `
  -pass ServiceAccountPassword! `
  -ptype KRB5_NT_PRINCIPAL `
  -princ svc-spring@DOMAIN.COM `
  -crypto AES256-SHA1
```

Copy the file to the Linux host and lock down its permissions:

```bash
sudo cp svc-spring.keytab /etc/spring-app/svc-spring.keytab
sudo chmod 400 /etc/spring-app/svc-spring.keytab
sudo chown springapp:springapp /etc/spring-app/svc-spring.keytab
```

Verify the keytab resolves correctly:

```bash
kinit -kt /etc/spring-app/svc-spring.keytab svc-spring@DOMAIN.COM
klist
```

#### 4. Create a JAAS configuration file

JAAS tells the JDBC driver how to obtain a Kerberos ticket. Create `/etc/spring-app/jaas.conf`:

```
SQLJDBCDriver {
    com.sun.security.auth.module.Krb5LoginModule required
    useKeyTab=true
    keyTab="/etc/spring-app/svc-spring.keytab"
    principal="svc-spring@DOMAIN.COM"
    doNotPrompt=true
    storeKey=true
    isInitiator=true;
};
```

> If you prefer using a pre-existing `kinit` ticket cache instead of a keytab (e.g. during development), replace the block body with:
> ```
>     useTicketCache=true
>     doNotPrompt=true;
> ```

#### 5. Configure `application.yaml`

Use `authenticationScheme=JavaKerberos` and `integratedSecurity=true`. Omit `username` and `password`.
The `serverSpn` value must match the SPN registered for the SQL Server instance in Active Directory.

```yaml
databases:
  connections:
    corp-db:
      url: >-
        jdbc:sqlserver://sql-server.domain.com:1433;
        databaseName=corpdb;
        integratedSecurity=true;
        authenticationScheme=JavaKerberos;
        serverSpn=MSSQLSvc/sql-server.domain.com:1433@DOMAIN.COM;
        encrypt=true;
        trustServerCertificate=false
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```

> **Finding the SPN** — run this on any domain-joined Windows machine or DC:
> ```powershell
> setspn -L sql-server$
> # or
> setspn -Q MSSQLSvc/sql-server.domain.com:1433
> ```

#### 6. Run the service with Kerberos JVM arguments

Pass the JAAS config and Kerberos config paths as JVM system properties:

```bash
./mvnw spring-boot:run \
  -Djava.security.auth.login.config=/etc/spring-app/jaas.conf \
  -Djava.security.krb5.conf=/etc/krb5.conf
```

Or set them in `JAVA_TOOL_OPTIONS` so they apply to every JVM invocation on the host:

```bash
export JAVA_TOOL_OPTIONS="\
  -Djava.security.auth.login.config=/etc/spring-app/jaas.conf \
  -Djava.security.krb5.conf=/etc/krb5.conf"
./mvnw spring-boot:run
```

For a `systemd` service unit, add them to the `Environment` directive:

```ini
[Service]
User=springapp
Environment="JAVA_TOOL_OPTIONS=-Djava.security.auth.login.config=/etc/spring-app/jaas.conf -Djava.security.krb5.conf=/etc/krb5.conf"
ExecStart=/opt/spring-app/bin/sql-mapper-api.jar
```

---

### Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `No credentials cache file found` | No TGT in cache and no keytab configured | Run `kinit -kt <keytab> <principal>` or fix `jaas.conf` |
| `KrbException: Cannot locate default realm` | `/etc/krb5.conf` missing or `default_realm` not set | Set `default_realm` in `[libdefaults]` |
| `Server not found in Kerberos database` | Wrong or missing SPN on the SQL Server AD account | Register SPN: `setspn -A MSSQLSvc/host:1433 domain\svc-sql` |
| `Encryption type not supported` | Keytab uses an older cipher (e.g. RC4) | Recreate keytab with `-crypto AES256-SHA1` |
| `GSS-API Error: No valid credentials provided` | Keytab principal does not match `principal=` in `jaas.conf` | Ensure they are identical including case |
| `Connection refused` | Port 1433 blocked or SQL Server not listening | Check firewall and SQL Server network configuration |

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

### 2. Test a connection

Executes `SELECT 1` against the target database and reports whether the connection is reachable, along with the round-trip time. Useful for verifying credentials, network access, and connection pool health without running a real query.

```
GET /api/connections/{connectionId}/test
```

| Parameter      | Type       | Required | Description                                    |
|----------------|------------|----------|------------------------------------------------|
| `connectionId` | path param | Yes      | Database connection ID from `application.yaml` |

Returns `200 OK` when the connection is healthy, `503 Service Unavailable` when it is not — so HTTP-level monitors that only check the status code also work.

#### curl — successful connection

```bash
curl -s http://localhost:8080/api/connections/primary-db/test
```

#### Response `200 OK`

```json
{
  "connectionId": "primary-db",
  "status": "UP",
  "message": "Connection successful",
  "responseTimeMs": 12
}
```

#### curl — failed connection

```bash
curl -s http://localhost:8080/api/connections/analytics-db/test
```

#### Response `503 Service Unavailable`

```json
{
  "connectionId": "analytics-db",
  "status": "DOWN",
  "message": "The TCP/IP connection to the host analytics-host, port 1433 has failed.",
  "responseTimeMs": 5032
}
```

#### curl — unknown connection ID

```bash
curl -s http://localhost:8080/api/connections/unknown-db/test
```

#### Response `404 Not Found`

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Connection id 'unknown-db' is not configured",
  "path": "/api/connections/unknown-db/test",
  "timestamp": "2024-05-01T10:00:00Z"
}
```

---

### 3. List all registered queries


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

### 4. Execute a query — all columns

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

### 5. Execute a query — selected columns only

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

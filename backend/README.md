# TAMP Backend (orchestrator)

Java service that fronts the public API, owns authentication and persistence, and calls out to the matching service. Referred to elsewhere as *"the orchestrator"* — that is its role; `backend/` is its path.

**Stack:** Java 21 · Spring Boot 3.5 · Maven

> **Status:** scaffolding. Starts and responds; no business logic yet.

## Prerequisites

- JDK 21 or later
- Maven 3.9 or later

```bash
java -version    # expect 21.x
mvn -version     # expect 3.9.x
```

## Run

```bash
mvn spring-boot:run
```

Starts on <http://localhost:8080>. Override with `SERVER_PORT`:

```bash
SERVER_PORT=8081 mvn spring-boot:run
```

## Test

```bash
mvn test
```

## Verify

```bash
curl http://localhost:8080/
```

Expected:

```json
{"service":"backend","status":"ok","message":"Hello from TAMP backend"}
```

## Layout

```
src/main/java/za/co/ice/tamp/backend/
  BackendApplication.java     Spring Boot entrypoint
  web/                        HTTP controllers
src/main/resources/
  application.yml             Configuration; env-var driven for portability
src/test/java/                Mirrors the main package structure
```

Configuration is read from environment variables with local defaults, so the same build runs unchanged in a container or a cloud environment without code edits.

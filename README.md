# Candidate Service

A Java application for recruiters to create, view, update, delete, search and filter candidates.

- **Backend:** Java 21, Spring Boot 4, Spring Data JPA, MySQL 8, Flyway, Spring Security (JWT)
- **Frontend (optional):** React + TypeScript + Ant Design, in `frontend/`

Documents:
- [Design document](docs/DESIGN.md), including how the solution would scale
- [AI usage](docs/AI_USAGE.md)
- [API contract](docs/API_CONTRACT.md)

---

## 1. Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| Java (JDK) | 21 | backend |
| MySQL | 8.0 | database |
| Node.js | 20 or newer | frontend only |
| Docker | any recent | integration tests only (Testcontainers) |

Maven is not needed: the project includes the Maven wrapper (`mvnw` / `mvnw.cmd`).

## 2. Create the database

Create an empty database. Flyway creates the tables when the application starts.

```sql
CREATE DATABASE candidate_management CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

## 3. Set environment variables

No passwords or secrets are stored in the code. Set these before starting the backend:

| Variable | Example | Meaning |
|---|---|---|
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | `your-mysql-password` | MySQL password |
| `RECRUITER_PRIYA_PASSWORD` | `Priya@123` | password for recruiter `priya.n` |
| `RECRUITER_ARJUN_PASSWORD` | `Arjun@123` | password for recruiter `arjun.d` |
| `JWT_SECRET` | any random text, **at least 32 characters** | key used to sign login tokens |

Windows (PowerShell):
```powershell
$env:DB_USERNAME="root"; $env:DB_PASSWORD="..."
$env:RECRUITER_PRIYA_PASSWORD="Priya@123"; $env:RECRUITER_ARJUN_PASSWORD="Arjun@123"
$env:JWT_SECRET="change-me-to-a-long-random-string-1234567890"
```

macOS / Linux:
```bash
export DB_USERNAME=root DB_PASSWORD=... RECRUITER_PRIYA_PASSWORD=Priya@123 \
       RECRUITER_ARJUN_PASSWORD=Arjun@123 JWT_SECRET=change-me-to-a-long-random-string-1234567890
```

In IntelliJ: *Run → Edit Configurations → Environment variables*.

## 4. Run the backend

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

The API runs on **http://localhost:8081**. Check: http://localhost:8081/actuator/health

## 5. Run the frontend (optional)

```bash
cd frontend
npm install                     # first time only
npm run dev                     # Windows PowerShell: npm.cmd run dev
```

Open **http://localhost:5173** and sign in as `priya.n` with the password you set above.

## 6. Run the tests

```bash
./mvnw test                     # Windows: mvnw.cmd test
```

Integration tests start a real MySQL in Docker (Testcontainers), so Docker must be running.
A test-coverage report (JaCoCo) is written to `target/site/jacoco/index.html`.

## 7. Try the API

```bash
# 1. Sign in and copy the accessToken from the response
curl -X POST http://localhost:8081/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"priya.n","password":"Priya@123"}'

# 2. Search Java developers in Pune with 3-7 years of experience
curl "http://localhost:8081/api/v1/candidates?skills=java&city=Pune&minExperience=3&maxExperience=7" \
     -H "Authorization: Bearer <accessToken>"
```

A Postman collection is in `docs/testing/candidate-service.postman_collection.json`.
Import it, run **0. Login → Sign in**, copy `accessToken` from the response into the collection
variable `token`; every other request then sends it as a Bearer token.

## 8. Sample data (optional)

`docs/testing/seed_candidates_25k.sql` inserts 25,000 realistic candidates for trying search and paging at volume.
Run it in MySQL Workbench after the application has started once (so the tables exist).
`docs/testing/cleanup_seed_candidates.sql` removes them again.

## 9. Project structure

```
src/main/java/com/recruit/candidate_service/
  controllers/     REST endpoints
  services/        business rules (validation, duplicates, versions)
  repositories/    database access, search Specifications
  entities/        JPA entities (database tables)
  models/          request / response DTOs
  mapper/          MapStruct entity <-> DTO mapping
  exceptions/      error types and the global error handler
  security/        JWT login and security configuration
  logging/         request id + access logging
src/main/resources/db/migration/   Flyway database migrations
frontend/                          React UI
docs/                              design, AI usage, API contract, test material
```

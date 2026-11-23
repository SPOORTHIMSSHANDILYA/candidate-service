# Design Document — Candidate Service

## 1. Problem and scope

Recruiters need one place to keep candidate information and quickly find the right people.
The application supports: create, view, update and delete candidates; free-text search;
filtering by several criteria at once; validation and clear errors; persistent storage.

**In scope:** a recruiter-facing candidate database, used by a small number of recruiters.
**Out of scope (deliberately):** jobs and applications, resumes / file upload, candidate
self-registration, multiple organizations, roles and permissions per recruiter.

## 2. Architecture

A single Spring Boot service with a layered structure, plus an optional React UI.

```
React UI  ──HTTP/JSON──►  Controller  ──►  Service  ──►  Repository  ──►  MySQL
(optional)                (REST, @Valid)   (business     (Spring Data JPA,
                                            rules)        Specifications)
```

- **Controllers** handle HTTP only: parameters, status codes, `@Valid` input checks.
- **Services** hold the business rules: duplicate email, optimistic-lock version check,
  email/skill normalisation, transactions.
- **Repositories** do data access; search filters are built with JPA Specifications.
- **DTOs** (records) form the API; entities are never exposed. MapStruct maps between them.

**Why one service and not several microservices?** It is one business area with one team.
Splitting now would add network calls, deployments and data consistency problems without any
benefit. The packages are separated so that a split is possible later if needed.

**Stack:** Java 21, Spring Boot 4, Spring Data JPA / Hibernate, MySQL 8, Flyway, Spring Security
(JWT), MapStruct, Lombok, JUnit 5, Mockito, Testcontainers.

## 3. Data model

```
candidates (1) ───< (many) candidate_skills
```

**candidates**: id (BIGINT, auto-increment), first/last name, email (unique), phone,
current title, current company, city, country, years of experience, notice period (days),
open to work, status (ACTIVE / INACTIVE / ARCHIVED), created/updated by, created/updated at,
version.

**candidate_skills**: (candidate_id, skill) primary key, foreign key with `ON DELETE CASCADE`.

Decisions:
- **BIGINT ids, not UUIDs.** Smaller and faster indexes and ordered inserts in MySQL. A readable
  code such as `CAN-00001` is derived from the id for display.
- **Skills in their own table**, not a comma-separated column, so "has skill X" is an indexed
  lookup. Skills are stored in lower case so matching is case-insensitive.
- **Email is unique** (database constraint), and stored in lower case.
- **Status instead of only deleting:** ARCHIVED hides a candidate from the default search while
  keeping the record. DELETE removes the record permanently.
- **Database checks** back up the API validation (experience 0–60, notice ≤ 365 days, valid status).
- **Indexes** match the main queries: status + created date (default list), created date,
  country + city, city, experience, last + first name, and skill.
- Schema changes are versioned with **Flyway** migrations; Hibernate only validates the schema.

## 4. API design

REST, JSON, versioned under `/api/v1`. Full details: [API_CONTRACT.md](API_CONTRACT.md).

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/login` | sign in, returns a JWT |
| GET | `/auth/me` | current recruiter |
| GET | `/candidates` | search, filter, sort, page |
| GET | `/candidates/{id}` | full details |
| POST | `/candidates` | create → 201 + `Location` header |
| PUT | `/candidates/{id}` | update (needs `version`) |
| PATCH | `/candidates/{id}/status` | change status only |
| DELETE | `/candidates/{id}` | delete → 204 |
| GET | `/candidates/stats` | counts for the overview page |
| GET | `/skills?prefix=` | skill suggestions |

**Search and filters** (all optional, combined with AND): `q` (name, email, title, company),
`email`, `skills` with `skillMatch=ALL|ANY`, `city`, `country`, `minExperience`/`maxExperience`,
`openToWork`, `status` (several allowed; ARCHIVED hidden unless asked for), `maxNoticeDays`.
Paging with `page` and `size` (max 100) and `sort` from a fixed list of fields.

- The list returns a **summary** of each candidate; full details only come from `GET /{id}`
  (less personal data sent, smaller responses).
- Each filter is one small Specification, so adding a filter means adding one method.

## 5. Validation and error handling

Every error uses one JSON shape: `{"error": "...", "fields": {"email": "..."}}`.

| Status | When |
|---|---|
| 400 | invalid body or parameters (all invalid fields listed), bad sort field, min > max |
| 401 | not signed in, wrong password, invalid or expired token |
| 404 | candidate not found, unknown URL |
| 409 | email already used, or the record was changed by someone else (stale version) |
| 500 | unexpected error: generic message to the client, full details only in the log |

**Concurrent edits:** each candidate has a `version`. An update must send the version it read;
if someone saved in between, the update gets 409 instead of silently overwriting their change
(optimistic locking).

## 6. Security

- **Authentication:** `POST /auth/login` checks the password once (BCrypt) and returns a signed
  JWT (HS256, valid 8 hours). Every other request sends `Authorization: Bearer <token>`.
  Stateless: no server session, so any number of instances can serve requests.
- **Secrets** (database password, recruiter passwords, JWT key) come from environment variables,
  never from Git. The app refuses to start without them.
- **Recruiters are configured** in `application.yml` for this demo. With more users they would
  move to a database table, or to the company's single sign-on (SSO).
- **SQL injection:** only JPA parameters and Specifications, never SQL built from strings;
  `%` and `_` in search text are escaped.
- **Input limits:** sizes on every field, max page size, whitelisted sort fields.
- **Personal data:** list APIs return only summary fields; search text is never written to logs.
- **Audit:** `createdBy` / `updatedBy` are set from the signed-in recruiter, not from the request.
- CSRF protection is off because no cookies are used.

## 7. Logging

- Each request gets a **request id** (from the `X-Request-Id` header, or a new UUID), returned in
  the response header and shown on every log line together with the **username**.
- One **access log line** per request: method, path, status, time in ms (WARN for 4xx, ERROR for 5xx).
- Business events are logged with ids only (created / updated / deleted candidate id).
- Query strings and search text are not logged because they can contain personal data.

## 8. Testing

**Automated (53 tests, `mvnw test`):**
- **Unit tests (29)** with JUnit 5 and Mockito: business rules in the service (duplicate email,
  missing / stale version, not found, email and skill normalisation, audit fields, stats),
  sort and paging validation, LIKE escaping in skill lookup.
- **Integration tests (24)** with Testcontainers: a real MySQL 8 in Docker, the real Flyway
  migration and the full HTTP stack (MockMvc with security and filters); nothing is mocked.
  They cover every search filter (skills ALL/ANY, city, country, experience range, open to
  work, notice period, several statuses, text search, paging and sorting), create / update /
  delete, duplicate email and stale-version conflicts, validation and error JSON, JWT security
  (no token, invalid token, wrong password), the stats and skill-lookup endpoints, and the
  request id header.
- **Coverage report:** JaCoCo runs with the tests; open `target/site/jacoco/index.html`.

Testing against real MySQL rather than an in-memory database catches differences in SQL,
collation and constraints, and proves the Flyway migration works on an empty database.

**Manual (Postman):** before writing the automated tests I tested every endpoint by hand with
a 55-request Postman collection (`docs/testing/`): happy paths and error paths for create, get,
update, status change, search and filters, skill lookup and delete. I tracked each case
(expected vs actual status and response, pass / fail) in a test tracker; all 55 passed.
Doing this manually first helped me understand each response before automating it.
The UI flows (login, overview, search, filters, add, edit, delete, sign out) were tested in the browser.

## 9. Performance

- Search runs in the database with indexes; results are always paged (max 100 per page).
- Counts for the overview page are single `COUNT` / `GROUP BY` queries.
- **No N+1 queries on search:** skills are a lazy collection, so a page of 20 candidates first
  caused 1 + 20 queries. `@BatchSize(size = 100)` on the collection makes Hibernate load the skills
  of the whole page in one `IN (...)` query. A `JOIN FETCH` was not used because with paging it
  makes Hibernate page in memory instead of in the database.

**Performance testing with 25,000 candidates** (seed script in `docs/testing/`, about 102,000
skill rows), measured locally on my laptop with SQL logging and the app's access log:

| Check | Result |
|---|---|
| SQL queries per search page (20 rows) | **3** (page, count, skills) instead of **22** before the fix |
| Default search, newest first, after warm-up | **about 230–620 ms** |
| First request after startup | about 1.5 s (JVM warm-up, one time) |
| Sign-in (BCrypt check + token) after warm-up | about 80–120 ms |

These are single-user measurements on a development machine, not a load test. Next step would be
a load test (e.g. k6 or JMeter) with many concurrent users, and `EXPLAIN` on each filter query.

## 10. Scaling: much larger data and many more users

Steps in order, cheapest first. Each is taken only when measurements show it is needed.

1. **Measure first:** metrics and slow-query logs; check query plans (`EXPLAIN`) for each filter.
2. **Better paging:** replace page numbers with keyset (cursor) paging, so deep pages stay fast;
   avoid exact total counts on very large result sets.
3. **More application instances** behind a load balancer. Easy because the service is stateless (JWT).
4. **Database:** connection pool tuning, read replicas for search traffic, primary for writes.
5. **Caching** (e.g. Redis) for data read often and changed rarely: skill suggestions, overview counts.
6. **Search engine** (Elasticsearch / OpenSearch) when free-text search and many combined filters
   outgrow SQL; the database stays the source of truth and changes are synced to the index.
7. **Asynchronous work** through a message queue for bulk imports, resume parsing, notifications.
8. **Users and access:** recruiters in a database or company SSO, roles and per-team visibility;
   multiple organizations via a tenant id on every record.
9. **Splitting into services** (e.g. candidates, jobs, search) only when separate teams or very
   different load patterns justify it.

## 11. Trade-offs and known limitations

- Recruiters are configured, not stored in a database (fine for a demo, listed in scaling).
- Logout only removes the token in the browser; a token stays valid until it expires (8 h).
- Email is globally unique; real recruiting may need duplicate detection instead of rejection.
- DELETE is permanent; ARCHIVED status is the safe alternative.

## 12. Future work

- **Resume upload:** store files in object storage (e.g. S3), keep only metadata (file name, type,
  size, storage key) in a `candidate_resumes` table; allow only PDF/DOCX up to a size limit, scan
  uploads, and serve downloads through short-lived signed URLs so storage paths are never exposed.
  Later: parse resumes to pre-fill candidate fields and skills.
- **Profile picture:** same storage approach as resumes; accept only JPEG/PNG with a size limit,
  resize to a small thumbnail on upload, and keep only the image key on the candidate record.
- **End-to-end UI tests with Playwright:** automate the browser flows tested manually today
  (login, overview, search and filters, add, edit, delete, sign out) and run them in CI against
  the app loaded with the 25k seed data.

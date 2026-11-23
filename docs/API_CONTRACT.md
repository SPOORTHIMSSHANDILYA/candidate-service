# API Contract — Candidate Service

Base URL: `http://localhost:8081/api/v1`  ·  JSON in and out  ·  Times are UTC (ISO-8601)

## Conventions

- **Authentication:** every endpoint except `POST /auth/login` and `GET /actuator/health` needs
  `Authorization: Bearer <accessToken>` (token from login, valid 8 hours).
- **Errors** always use one shape:
  ```json
  { "error": "message", "fields": { "fieldName": "what is wrong" } }
  ```
  `fields` only appears for validation errors.
- **Request id:** every response has an `X-Request-Id` header (send your own to trace a request in the logs).
- **Optimistic locking:** updates must send the `version` from the last read; a stale version gets `409`.

| Status | Meaning |
|---|---|
| 200 / 201 / 204 | OK / created / deleted |
| 400 | invalid body or parameters |
| 401 | not signed in, wrong password, invalid or expired token |
| 404 | candidate or URL not found |
| 405 | wrong HTTP method |
| 409 | email already used, or version conflict |
| 500 | unexpected error (`"Internal server error"`, details only in the server log) |

---

## Authentication

### POST /auth/login
Sign in and get a token. (No token needed.)

request:
```json
{ "username": "priya.n", "password": "your-password" }
```
response `200 OK`:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-11-22T23:30:00Z",
  "username": "priya.n",
  "displayName": "Priya Nair"
}
```
errors:
- `400` blank username or password → `{ "error": "Invalid request body", "fields": { "password": "must not be blank" } }`
- `401` wrong username or password → `{ "error": "Invalid username or password" }`

### GET /auth/me
The signed-in recruiter.

response `200 OK`:
```json
{ "username": "priya.n", "displayName": "Priya Nair" }
```
errors:
- `401` no token → `{ "error": "Authentication required" }`
- `401` invalid or expired token → `{ "error": "Invalid or expired token" }`

---

## Candidates

### GET /candidates — search, filter, sort, page
All parameters are optional and combined with AND.

| Parameter | Example | Meaning |
|---|---|---|
| `q` | `ananya rao` | partial match on first, last or full name, email, title, company |
| `email` | `ananya@test.com` | exact email |
| `skills` | `java,aws` | candidates with these skills |
| `skillMatch` | `ALL` (default) / `ANY` | must have every skill / at least one |
| `city`, `country` | `Pune`, `India` | exact match (case-insensitive) |
| `minExperience`, `maxExperience` | `3`, `7` | years of experience, 0–60 |
| `openToWork` | `true` | open to work only |
| `maxNoticeDays` | `30` | notice period at most N days |
| `status` | `ACTIVE,INACTIVE` | default: everything except `ARCHIVED` |
| `page` | `0` | page number, from 0 |
| `size` | `20` | 1–100, default 20 |
| `sort` | `lastName,asc` | `createdAt` (default, desc), `updatedAt`, `lastName`, `firstName`, `yearsExperience` |

request:
```
GET /candidates?skills=java,aws&city=Pune&minExperience=3&maxExperience=7&page=0&size=20&sort=createdAt,desc
```
response `200 OK` (each item is a summary; full details come from `GET /candidates/{id}`):
```json
{
  "content": [
    {
      "id": 42,
      "candidateCode": "CAN-00042",
      "firstName": "Ananya",
      "lastName": "Rao",
      "email": "ananya@test.com",
      "currentTitle": "Java Developer",
      "city": "Pune",
      "country": "India",
      "yearsExperience": 5,
      "noticePeriodDays": 30,
      "openToWork": true,
      "skills": ["java", "aws"],
      "candidateStatus": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```
errors (`400`):
- `minExperience` > `maxExperience` → `{ "error": "minExperience must not be greater than maxExperience" }`
- `size` outside 1–100 → `{ "error": "size must be between 1 and 100" }`
- negative `page` → `{ "error": "page must be 0 or greater" }`
- sort field not allowed → `{ "error": "sort must be one of [...]" }`
- bad sort direction → `{ "error": "sort direction must be asc or desc" }`
- wrong type or value (e.g. `minExperience=abc`, `status=DELETED`) → `{ "error": "Invalid request body", "fields": { ... } }`

### GET /candidates/{id}
response `200 OK`:
```json
{
  "id": 42,
  "candidateCode": "CAN-00042",
  "firstName": "Ananya",
  "lastName": "Rao",
  "email": "ananya@test.com",
  "phone": "+91 98765 43210",
  "currentTitle": "Java Developer",
  "currentCompany": "Acme",
  "city": "Pune",
  "country": "India",
  "yearsExperience": 5,
  "noticePeriodDays": 30,
  "openToWork": true,
  "skills": ["aws", "java"],
  "candidateStatus": "ACTIVE",
  "version": 0,
  "createdAt": "2026-11-20T10:15:30Z",
  "updatedAt": "2026-11-20T10:15:30Z"
}
```
errors:
- `400` id is not a number (`/candidates/abc`) → `{ "error": "Invalid value for 'id'" }`
- `404` → `{ "error": "Candidate not found" }`

### POST /candidates
Required: `firstName`, `lastName`, `email`. Everything else optional.
Email is stored in lower case; skills are trimmed, lower-cased and de-duplicated.
`id`, `version`, `candidateStatus` and audit fields are set by the server (ignored if sent).

request:
```json
{
  "firstName": "Ananya",
  "lastName": "Rao",
  "email": "ananya@test.com",
  "phone": "+91 98765 43210",
  "currentTitle": "Java Developer",
  "currentCompany": "Acme",
  "city": "Pune",
  "country": "India",
  "yearsExperience": 5,
  "noticePeriodDays": 30,
  "openToWork": true,
  "skills": ["Java", "AWS"]
}
```
response `201 Created`, header `Location: /api/v1/candidates/42`, body = the full candidate (as in `GET /candidates/{id}`).

Validation rules: names ≤ 100 chars, valid email ≤ 254, phone digits/space/+/-/() 7–20 chars,
experience 0–60, notice 0–365 days, at most 30 skills of ≤ 50 chars.

errors:
- `400` invalid fields → `{ "error": "Invalid request body", "fields": { "email": "must be a well-formed email address", "yearsExperience": "must be less than or equal to 60" } }`
- `400` broken JSON or wrong type (e.g. `"yearsExperience": "abc"`) → `{ "error": "Invalid request body" }`
- `409` email already used (any letter case) → `{ "error": "Candidate with this email already exists" }`

### PUT /candidates/{id}
Same body as POST **plus `version`** from the last read (required). Replaces all editable fields and skills.

request:
```json
{
  "firstName": "Ananya",
  "lastName": "Rao",
  "email": "ananya@test.com",
  "currentTitle": "Senior Java Developer",
  "city": "Pune",
  "openToWork": true,
  "skills": ["java", "aws", "kafka"],
  "version": 0
}
```
response `200 OK`: the full candidate, with `version` increased by 1.

errors:
- `400` invalid fields → `{ "error": "Invalid request body", "fields": { ... } }`
- `400` no version → `{ "error": "version is required" }`
- `404` → `{ "error": "Candidate not found" }`
- `409` email used by another candidate → `{ "error": "Candidate with this email already exists" }`
- `409` stale version → `{ "error": "Candidate was modified by another user, reload and try again" }`

### PATCH /candidates/{id}/status
Change only the status: `ACTIVE`, `INACTIVE` or `ARCHIVED` (archived candidates are hidden from default search).

request:
```json
{ "status": "ARCHIVED", "version": 1 }
```
response `200 OK`: the full candidate, with `version` increased by 1.

errors:
- `400` missing version or status → `{ "error": "Invalid request body", "fields": { "version": "must not be null" } }`
- `400` unknown status value → `{ "error": "Invalid request body" }`
- `404` → `{ "error": "Candidate not found" }`
- `409` stale version → `{ "error": "Candidate was modified by another user, reload and try again" }`

### DELETE /candidates/{id}
Deletes the candidate and its skills permanently (use status `ARCHIVED` to keep the record).

response `204 No Content` (no body)

errors:
- `404` → `{ "error": "Candidate not found" }`

### GET /candidates/stats
Counts for the overview page.

response `200 OK`:
```json
{
  "total": 25003,
  "countsByStatus": { "ACTIVE": 20047, "INACTIVE": 3709, "ARCHIVED": 1247 },
  "addedInLast7Days": 241,
  "openToWork": 18002
}
```
Every status is always present (0 if none).

---

## Skills

### GET /skills?prefix=ja&limit=20
Skill suggestions for the filter dropdown: distinct, lower case, sorted. Both parameters optional;
`prefix` is case-insensitive, and `%` / `_` are matched literally.

response `200 OK`:
```json
["java", "javascript"]
```
errors:
- `400` `limit` outside 1–50 → `{ "error": "limit must be between 1 and 50" }`

---

## Health

### GET /actuator/health
No token needed.

response `200 OK`:
```json
{ "status": "UP" }
```

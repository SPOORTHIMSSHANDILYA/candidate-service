# AI Usage

**Tools:** Claude (chat, and as a coding agent with access to the project folder), IntelliJ IDEA.

I used AI as a pair programmer and reviewer. I made the design decisions, ran and tested
everything myself, and only kept code I understood and could explain.

## Where and how I used AI

| Area | How AI was used |
|---|---|
| Planning | Discussed approach, scope and trade-offs before coding (monolith vs services, database, ids). |
| Database schema | AI proposed tables and indexes; I reviewed, simplified and ran them in MySQL. |
| Backend code | I wrote the first CRUD endpoints with AI snippets; AI reviewed my code for quality and bugs; AI drafted the search Specifications, JWT login and request logging, which I reviewed. |
| Testing material | AI generated a 55-request Postman collection, a test tracker and a 25k-row seed script. |
| Automated tests | AI generated the unit and integration tests; I reviewed which cases matter, ran them (53 passing) and checked them against my manual Postman results. |
| Frontend | AI built most of the React UI (I am a backend developer); I tested every flow in the browser. |

## How I validated AI output

- **Compiled and ran** every change in IntelliJ against my local MySQL.
- **Ran the schema** in MySQL Workbench and checked the tables, constraints and indexes.
- **Manual API testing:** ran all 55 Postman requests myself (happy paths and error paths) and
  recorded expected vs actual result for each in a test tracker; all 55 passed. Where a result
  surprised me (e.g. a 409 on update), I worked out why before moving on.
- **Performance testing:** loaded 25,000 candidates, checked search, filters, paging and counts
  in the UI and API, and used SQL logging to count queries per request (3 per search page).
- **Browser testing** of the UI: create, edit, search, filters, login, logout.
- **Automated tests** that run with `mvnw test`.

## Changes I made to AI output (and bugs found)

- **Removed tables I did not need** (jobs, applications, users) to keep the scope to the brief.
- **Seed script failed in MySQL:** safe-update mode error, then a collation error caused by a
  session variable; fixed both before it ran.
- **Wrong auto-import:** `MAX_PAGE_SIZE` was imported from a Hibernate dialect class instead of my
  constants; replaced it.
- **Entity design:** replaced `@Data` on the JPA entity with getters and setters only for editable
  fields, so ids, versions and timestamps cannot be changed from outside.
- **MapStruct mapping errors** (unmapped properties) after changing the entity; fixed the mappings.
- **UI bug:** the "Add candidate" form kept the previous candidate's values; fixed and retested.
- **N+1 query problem in AI-generated search code:** each candidate's skills were loaded with a
  separate query, so one search page of 20 candidates ran 22 queries. It worked and passed all
  manual tests, so it was easy to miss. I compared the options (`JOIN FETCH`, entity graph, batch
  fetching) and chose `@BatchSize`, because fetch joins on a collection break database paging.
  I verified it with SQL logging on the 25k data: 22 queries before, 3 after.

## Where I did not follow AI recommendations

| AI suggested | I chose | Why |
|---|---|---|
| UUIDs stored as binary | BIGINT auto-increment ids | Easier to read and debug, smaller indexes; a display code `CAN-00001` covers readability. |
| Separate tables for jobs, applications and users | Candidates and skills only | The brief is about candidates; less code to maintain and explain. |
| HTTP Basic authentication | JWT | I use JWT at work; the password is checked once instead of on every request; stateless. |
| Moving recruiters into a database table | Keep configured recruiters | Login is not required by the brief; documented as the next step instead. |
| Automated Postman scripts | Manual Postman testing first | I wanted to understand each response myself before automating. |
| More dashboard charts and widgets | Four numbers and one status bar | Simpler; the brief values clear decisions over extra features. |

## What I learned

AI was fastest for boilerplate, test data and UI. It was most valuable as a reviewer of my own
code. It was the least reliable on environment-specific details (MySQL settings, imports, library
versions), which is where running and testing everything myself mattered most.

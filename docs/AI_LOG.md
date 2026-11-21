## 2026-11-20 — Milestone 0: database schema + config

### Schema (AI-assisted)
- **Asked:** Claude to design MySQL tables for candidate
- **AI produced:** 
- **I validated:** Executed the script on my own MySQL [version] via Workbench;
  checked tables/indexes created
- **Changed / rejected:** none yet

### DB configuration (written by me)
- Wrote the datasource config in `application.yml` myself, without AI.

Manual API testing
- AI generated a Postman collection (55 requests) and a test tracker.
- I tested all 55 cases manually in Postman; all pass.
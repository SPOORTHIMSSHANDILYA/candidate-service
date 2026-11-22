# Candidate CRM — frontend

A simple recruiter screen for `candidate-service`: search, filter, view, add, edit, change status and delete candidates.

**Stack:** React 19 + TypeScript + Vite + Ant Design 6.

## Run it

You need **Node.js 20+** (`node -v`) and the backend running on **http://localhost:8081**.

```bash
cd frontend
npm install        # first time only: downloads libraries into node_modules/
npm run dev        # starts the UI on http://localhost:5173
```

Open **http://localhost:5173**.

The dev server forwards every `/api/...` call to `http://localhost:8081` (see `vite.config.ts`),
so the browser talks to one address and no CORS setup is needed while developing.
If your backend uses another port, change it there.

Other commands:

| Command | What it does |
|---|---|
| `npm run build` | Type-checks and builds the production files into `dist/` |
| `npm run preview` | Serves the built `dist/` to check it |
| `npm run lint` | Checks the code for common mistakes |

## What is where

```
src/
├── main.tsx                 Starts React and shows <App/>
├── App.tsx                  Shell: theme, top bar, side menu
├── types.ts                 TypeScript shapes of the API data (mirror the Java DTOs)
├── api/
│   ├── client.ts            fetch() wrapper: JSON in/out, turns { error, fields } into ApiError
│   └── candidates.ts        One function per endpoint (searchCandidates, getCandidate, ...)
├── hooks/
│   └── useDebouncedValue.ts Waits until the user stops typing before searching
├── filters/
│   └── filterDefinitions.ts ALL filters in one list (add a filter = add one entry)
├── table/
│   └── candidateColumns.tsx ALL table columns in one list (add a column = add one entry)
├── utils/
│   ├── status.ts            "ACTIVE" -> "Active"
│   ├── storage.ts           Safe localStorage (saved views, chosen columns)
│   └── urlState.ts          Keeps the search in the address bar (bookmark / share a link)
├── pages/
│   └── CandidatesPage.tsx   The screen: holds the state and wires the pieces together
└── components/
    ├── FilterBar.tsx              Search box + filter buttons + "More filters" + "Clear all"
    ├── FilterChip.tsx             One Jira-style filter button with its small editor panel
    ├── SavedViews.tsx             "Views" menu: save / apply / delete a named search
    ├── ColumnSettings.tsx         "Columns" menu: show or hide table columns
    ├── CandidateTable.tsx         The list + "Showing 1–20 of N" footer; paging/sorting on the server
    ├── CandidateDetailsDrawer.tsx Read-only view + Edit / Change status / Delete
    ├── CandidateFormDrawer.tsx    One form for Add and Edit
    ├── SkillSelect.tsx            Skills multi-select with suggestions from GET /skills
    └── StatusTag.tsx              Coloured status label
```

## How it works (for a backend developer)

| React idea | Backend comparison |
|---|---|
| A **component** (a function returning JSX) | A small class that renders one part of the screen |
| **props** | Constructor/method parameters passed in from the parent |
| **state** (`useState`) | A field; changing it re-draws that part of the screen |
| **effect** (`useEffect`) | "Run this when X changes", e.g. call the API when filters change |
| `api/candidates.ts` | A typed client, like a Feign/RestTemplate wrapper |

**One search, step by step**

1. The user types in the search box → `searchText` state changes on every key.
2. `useDebouncedValue` waits 400 ms of no typing, then gives the final text.
3. `CandidatesPage` puts it into `filters.q` and goes back to page 1.
4. An effect sees `filters` changed → calls `searchCandidates(filters, paging)` → `GET /api/v1/candidates?q=...`.
5. The answer goes into `data` state → `CandidateTable` re-draws with the new rows.

Each filter button opens a small panel; the search runs when you press **Apply** in it.

**Filters, views, columns, address bar**

- Filters are defined in `filters/filterDefinitions.ts`. Pinned ones always show; the rest are under
  **+ More filters**, which has its own search box, so the bar stays tidy however many filters exist.
- **Views** saves the current search text, filters and sort under a name. Views and the chosen columns are
  stored in this browser (localStorage). With user accounts they would move to a backend table.
- The current search is written to the address bar (`?city=Pune&minExperience=3`), so a refresh keeps it
  and a link can be shared.

**Extra columns need 5 backend fields**

The optional columns Company, Country, Notice, Added and Updated show `—` until the backend's
`CandidateSummary` record also returns `currentCompany`, `country`, `noticePeriodDays`, `createdAt`, `updatedAt`.

**Saving with optimistic locking**

- The edit form keeps the `version` it loaded and sends it with PUT.
- If someone saved in between, the backend answers **409**; the form shows
  *"Someone else edited this candidate"* with a **Load latest** button instead of overwriting their change.
- **400** field errors from the backend are shown under the matching field;
  **409 duplicate email** is shown under the email field.

## Not included (on purpose)

- Login: added together with backend security (HTTP Basic).
- Dashboard numbers: need the `/candidates/stats` endpoint.

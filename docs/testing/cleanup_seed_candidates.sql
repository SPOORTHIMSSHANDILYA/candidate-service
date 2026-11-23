-- Removes ONLY the rows created by seed_candidates_25k.sql.
-- Candidates you created yourself (through the app or Postman) are kept.
-- Skills of the deleted candidates are removed automatically (ON DELETE CASCADE).

SET SQL_SAFE_UPDATES = 0;   -- Workbench's safe-update mode would block this DELETE (created_by is not a key)

DELETE FROM candidates
WHERE created_by IN ('anita.k', 'rahul.m', 'sara.j', 'vikram.p', 'meena.s');

SET SQL_SAFE_UPDATES = 1;   -- back to Workbench's default

ANALYZE TABLE candidates, candidate_skills;

SELECT COUNT(*) AS remaining_candidates FROM candidates;

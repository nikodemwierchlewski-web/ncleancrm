-- Selection is independent of dashboard pinning. Existing notes and steps stay intact.
ALTER TABLE memo_entry ADD COLUMN main_goal BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE memo_entry ADD CONSTRAINT memo_main_goal_eligible CHECK (NOT main_goal OR (kind='GOAL' AND NOT archived));
CREATE UNIQUE INDEX memo_single_main_goal ON memo_entry ((TRUE)) WHERE main_goal;

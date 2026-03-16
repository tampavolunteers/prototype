-- Add seeded flag to organizations and opportunities.
-- Existing rows are assumed to have been created via seed scripts,
-- so they default to TRUE. Future rows inserted outside the CLI default to FALSE.

ALTER TABLE organizations
    ADD COLUMN seeded BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE organizations
    ALTER COLUMN seeded SET DEFAULT FALSE;

ALTER TABLE opportunities
    ADD COLUMN seeded BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE opportunities
    ALTER COLUMN seeded SET DEFAULT FALSE;

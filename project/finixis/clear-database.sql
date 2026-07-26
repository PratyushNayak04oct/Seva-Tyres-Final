-- Seva Tyres — wipe all business data for a clean testing slate
-- Run against the sevatyres PostgreSQL database (pgAdmin / psql).
-- Schema and tables are preserved; only rows are removed.
-- Sale_Transaction_Item may not exist yet on older DBs — ignored safely.

TRUNCATE TABLE Alert_Log RESTART IDENTITY CASCADE;
TRUNCATE TABLE Alert_Config RESTART IDENTITY CASCADE;
TRUNCATE TABLE Generated_Report RESTART IDENTITY CASCADE;
TRUNCATE TABLE Sale_Transaction RESTART IDENTITY CASCADE;
TRUNCATE TABLE Transaction_Credit RESTART IDENTITY CASCADE;
TRUNCATE TABLE Transaction_Debit RESTART IDENTITY CASCADE;
TRUNCATE TABLE Inventory RESTART IDENTITY CASCADE;
TRUNCATE TABLE Customer RESTART IDENTITY CASCADE;

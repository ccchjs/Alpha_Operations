-- =========================================================
-- Airemore API Add-on Migration
-- Run this AFTER your existing schema.sql / migration.sql.
-- Adds: token storage for the mobile app, and client_uuid columns
-- so the app can safely retry a submit without creating duplicates.
-- Nothing existing is deleted or modified in a breaking way.
-- =========================================================

-- Bearer tokens issued to the Kotlin app on login.
CREATE TABLE IF NOT EXISTS api_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token CHAR(64) NOT NULL UNIQUE,
    device_label VARCHAR(150) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NULL DEFAULT NULL,
    revoked_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Idempotency keys: the app generates a UUID per record the moment the
-- staff member starts a new form (before saving). If a sync upload times
-- out and WorkManager retries it, the server recognizes the same
-- client_uuid and updates the existing row instead of inserting a dup.
-- NOTE: table name casing below matches your LIVE database exactly as
-- used by pm/save.php, repair/save.php and installation/save.php
-- (pm_* is lowercase; Repair_* and Installation_* are capitalized).
-- If your actual table names differ, adjust these three blocks first.

ALTER TABLE pm_forms
    ADD COLUMN IF NOT EXISTS client_uuid CHAR(36) NULL UNIQUE AFTER id;

ALTER TABLE Repair_forms
    ADD COLUMN IF NOT EXISTS client_uuid CHAR(36) NULL UNIQUE AFTER id;

ALTER TABLE Installation_forms
    ADD COLUMN IF NOT EXISTS client_uuid CHAR(36) NULL UNIQUE AFTER id;

-- Same idea for pm_units/pm_photos so a retried upload doesn't duplicate rows.
ALTER TABLE pm_units
    ADD COLUMN IF NOT EXISTS client_uuid CHAR(36) NULL AFTER id;

ALTER TABLE pm_photos
    ADD COLUMN IF NOT EXISTS client_uuid CHAR(36) NULL AFTER id;

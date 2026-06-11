-- ============================================================
-- data.sql — runs AFTER Hibernate creates all entity tables
-- ============================================================

-- ---------------------------------------------------------------
-- ShedLock table — used for distributed scheduler locking.
-- Only ONE instance runs @Scheduled jobs at a time.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

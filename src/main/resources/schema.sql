CREATE TABLE IF NOT EXISTS tb_user (
    user_id       TEXT PRIMARY KEY,
    user_pw       TEXT NOT NULL,
    is_del        TEXT DEFAULT 'N',
    refresh_token TEXT,
    time_create   DATETIME DEFAULT CURRENT_TIMESTAMP,
    time_update   DATETIME
);

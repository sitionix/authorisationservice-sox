CREATE TABLE IF NOT EXISTS global_roles (
    id          BIGINT PRIMARY KEY,
    description VARCHAR(64) NOT NULL UNIQUE
);

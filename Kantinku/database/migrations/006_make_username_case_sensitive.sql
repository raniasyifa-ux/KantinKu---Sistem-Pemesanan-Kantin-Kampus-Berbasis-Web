USE db_kantinku;

ALTER TABLE users
    DROP INDEX unique_username,
    MODIFY COLUMN username VARCHAR(50)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    ADD UNIQUE KEY unique_username (username);

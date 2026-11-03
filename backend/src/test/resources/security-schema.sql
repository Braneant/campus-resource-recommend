CREATE TABLE IF NOT EXISTS student_user (
    id BIGINT PRIMARY KEY, student_no VARCHAR(50), username VARCHAR(50), password VARCHAR(255),
    nickname VARCHAR(50), real_name VARCHAR(50), gender INT, college_id BIGINT, major_id BIGINT,
    grade VARCHAR(20), email VARCHAR(100), phone VARCHAR(20), avatar VARCHAR(255), status INT,
    create_time TIMESTAMP, update_time TIMESTAMP
);
CREATE TABLE IF NOT EXISTS admin (
    id BIGINT PRIMARY KEY, username VARCHAR(50), password VARCHAR(255), real_name VARCHAR(50),
    role INT, email VARCHAR(100), phone VARCHAR(20), status INT, last_login_time TIMESTAMP,
    create_time TIMESTAMP, update_time TIMESTAMP
);
CREATE TABLE IF NOT EXISTS password_reset_credential (
    student_id BIGINT PRIMARY KEY, token_hash CHAR(64) NOT NULL, issued_at TIMESTAMP(3) NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL, consumed_at TIMESTAMP(3)
);

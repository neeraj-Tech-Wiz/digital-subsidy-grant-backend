CREATE TABLE otp_challenge (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    attempt_count INT NOT NULL,
    max_attempts INT NOT NULL,
    used BOOLEAN NOT NULL
);

CREATE INDEX idx_otp_challenge_email ON otp_challenge(email);
CREATE INDEX idx_otp_challenge_expires_at ON otp_challenge(expires_at);

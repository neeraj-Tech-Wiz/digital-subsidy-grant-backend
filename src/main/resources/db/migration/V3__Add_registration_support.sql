CREATE TABLE IF NOT EXISTS dummy_v3 (id INT); -- Ensuring V3 executes successfully exactly.

ALTER TABLE users 
ADD COLUMN email_verified BOOLEAN DEFAULT TRUE;

ALTER TABLE otp_challenge 
ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE otp_challenge 
ADD COLUMN registration_name VARCHAR(255);

ALTER TABLE otp_challenge 
ADD COLUMN registration_password VARCHAR(255);

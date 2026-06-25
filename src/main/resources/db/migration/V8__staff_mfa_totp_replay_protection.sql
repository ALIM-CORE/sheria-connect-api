ALTER TABLE staff_mfa_credentials
    ADD COLUMN last_used_totp_counter BIGINT;

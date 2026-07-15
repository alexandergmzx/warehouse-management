-- Development/demo credentials only. This location is excluded from preprod.
-- picker02 / 2468 (numeric password: the HandheldPi PIN pad sends the PIN as
-- the account password after a badge scan supplies the username; the badge QR
-- payload OP:picker02 is a device-side convention the WMS never sees).
-- Password hash is a precomputed Argon2id PHC string (Spring Security v5.8
-- defaults, ADR 0005). Regenerate with the application PasswordEncoder if the
-- demo credentials change; never store production secrets here.
INSERT INTO app_user (username, password_hash, role)
VALUES
    ('picker02', '$argon2id$v=19$m=16384,t=2,p=1$s5nHmJmYrffdEwCnt1Jb1g$AS9VyOfwQNjWkw4BphE6xyMBjtAJ08px0iLmWgib34Y', 'PICKER');

-- Loopback development HHT so a desktop dev client and the physical HHT-PI-01
-- can hold sessions simultaneously (and demonstrate DEVICE_ASSIGNMENT_CONFLICT).
INSERT INTO device (device_code, description)
VALUES ('HHT-DEV-01', 'Developer loopback HHT (dev fixture)');

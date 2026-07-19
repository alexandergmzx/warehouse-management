-- Development/demo credentials only. This location is excluded from preprod.
-- wcs01 / wcs01pass: the dev stand-in for agv-fleet-controller (scripts/wcs-
-- standin/) authenticates as this WCS-role user to confirm MFC missions
-- (ADR 0011). Password hash is a precomputed Argon2id PHC string generated
-- with the same Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
-- defaults as the other dev fixtures (ADR 0005); regenerate with the
-- application PasswordEncoder if the demo credentials change.
INSERT INTO app_user (username, password_hash, role)
VALUES
    ('wcs01', '$argon2id$v=19$m=16384,t=2,p=1$lnzS67bHJaicgxO/uTaerg$jrGUvvumbIGch7HsTXGn7/tnJZ88wZxoCOJWfYoZi8c', 'WCS');

INSERT INTO device (device_code, description)
VALUES ('AGV-FC-01', 'agv-fleet-controller dev stand-in (dev fixture)');

-- The two fixed MFC handover points TRANSPORT missions reference
-- (wms.mfc.transport.source-location / .destination-location,
-- docs/configuration-matrix.md). Reserved pick-sequence range (9xxxx) so
-- they never sort into the picking FIFO alongside real picking locations;
-- no stock is ever assigned here.
INSERT INTO location (code, qr_value, pick_sequence)
VALUES
    ('MFC-90-01', 'LOC:MFC-90-01', 90001),
    ('MFC-90-02', 'LOC:MFC-90-02', 90002);

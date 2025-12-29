-- User
INSERT INTO user (id, username, password, role) VALUES (UUID_TO_BIN(UUID()), 'valid-user', '', 'ROLE_USER')
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO user (id, username, password, role) VALUES (UUID_TO_BIN(UUID()), 'second-user', '', 'ROLE_USER')
ON DUPLICATE KEY UPDATE id=id;

-- Tag
DELETE FROM tag;

INSERT INTO tag (id, name, color, user_id)
SELECT UUID_TO_BIN(UUID()), 'tag-1', 'white', id
FROM user
WHERE username = 'valid-user';

INSERT INTO tag (id, name, color, user_id)
SELECT UUID_TO_BIN(UUID()), 'tag-2', 'black', id
FROM user
WHERE username = 'valid-user';

INSERT INTO tag (id, name, color, user_id)
SELECT UUID_TO_BIN(UUID()), 'tag-3', 'white', id
FROM user
WHERE username = 'second-user';
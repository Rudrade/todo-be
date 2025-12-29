-- User
INSERT INTO user (id, username, password, role) VALUES (UUID_TO_BIN(UUID()), 'valid-user', '', 'ROLE_USER')
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO user (id, username, password, role) VALUES (UUID_TO_BIN(UUID()), 'second-user', '', 'ROLE_USER')
ON DUPLICATE KEY UPDATE id=id;

-- User Lists
INSERT INTO user_list (id, name, color, user_id)
SELECT UUID_TO_BIN(UUID()), 'list-1', 'red', id
FROM user
WHERE username = 'valid-user';

INSERT INTO user_list (id, name, color, user_id)
SELECT UUID_TO_BIN(UUID()), 'list-2', 'blue', id
FROM user
WHERE username = 'valid-user';

INSERT INTO user_list (id, name, color, user_id)
SELECT UUID_TO_BIN(UUID()), 'list-3', 'green', id
FROM user
WHERE username = 'second-user';


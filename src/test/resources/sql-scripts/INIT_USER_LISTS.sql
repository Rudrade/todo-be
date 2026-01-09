-- User
INSERT INTO user (id, username, password, role, email, is_active) VALUES (UUID_TO_BIN(UUID()), 'valid-user', '', 'ROLE_USER', 'test@mail.com', true)
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO user (id, username, password, role, email, is_active) VALUES (UUID_TO_BIN(UUID()), 'second-user', '', 'ROLE_USER', 'test-2@mail.com', true)
ON DUPLICATE KEY UPDATE id=id;

-- User Lists
UPDATE task SET user_list_id = NULL;
DELETE FROM user_list;

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


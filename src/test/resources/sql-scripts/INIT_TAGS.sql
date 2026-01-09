-- User
INSERT INTO user (id, username, password, role, email, is_active, contains_image) VALUES (UUID_TO_BIN(UUID()), 'valid-user', '', 'ROLE_USER', 'test@mail.com', true, false)
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO user (id, username, password, role, email, is_active, contains_image) VALUES (UUID_TO_BIN(UUID()), 'second-user', '', 'ROLE_USER', 'test-2@mail.com', true, false)
ON DUPLICATE KEY UPDATE id=id;

-- Tag
DELETE FROM tag_task;
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
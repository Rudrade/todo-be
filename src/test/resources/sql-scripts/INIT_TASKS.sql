-- User
INSERT INTO user (id, username, password, role) VALUES (UUID_TO_BIN(UUID()), 'valid-user', '', 'ROLE_USER')
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO user (id, username, password, role) VALUES (UUID_TO_BIN(UUID()), 'second-user', '', 'ROLE_USER')
ON DUPLICATE KEY UPDATE id=id;

-- Due Today
INSERT INTO task (id, title, description, due_date, user_id) 
SELECT UUID_TO_BIN(UUID()), 'title today', 'description today', CURRENT_DATE, id
FROM user
WHERE username = 'valid-user';

INSERT INTO task (id, title, description, due_date, user_id) 
SELECT UUID_TO_BIN(UUID()), 'title today 2', 'description today 2', CURRENT_DATE, id
FROM user
WHERE username = 'valid-user';

INSERT INTO task (id, title, description, due_date, user_id) 
SELECT UUID_TO_BIN(UUID()), 'title today invalid', 'description today invalid', CURRENT_DATE, id
FROM user
WHERE username = 'second-user';

-- Due Upcoming
INSERT INTO task (id, title, description, due_date, user_id) 
SELECT UUID_TO_BIN(UUID()), 'title future', 'description future', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), id
FROM user
WHERE username = 'valid-user';

INSERT INTO task (id, title, description, due_date, user_id) 
SELECT UUID_TO_BIN(UUID()), 'title future 2', 'description future', DATE_ADD(CURRENT_DATE, INTERVAL 1 YEAR), id
FROM user
WHERE username = 'valid-user';

INSERT INTO task (id, title, description, due_date, user_id) 
SELECT UUID_TO_BIN(UUID()), 'title future', 'description future', DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), id
FROM user
WHERE username = 'second-user';
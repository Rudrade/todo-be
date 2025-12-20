DELETE FROM user_list;
DELETE FROM user;

INSERT INTO user(id, username) VALUES (UUID_TO_BIN(UUID()), "valid-user");
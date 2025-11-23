INSERT IGNORE INTO pokemon_game_db.users (username, password, email)
VALUES (
    'Ash',
    '$2a$12$a01lqhNyMwck0C78Sp9YguQIVuL/dg2kNiH2uUo6kTiZ9xPotqnPy', -- 'pikachu'
    'ash.ketchum@kanto.com'
);

SET @user_role_id = (SELECT id FROM pokemon_game_db.roles WHERE name = 'ROLE_USER');

SET @ash_user_id = (SELECT id FROM pokemon_game_db.users WHERE username = 'Ash');

INSERT IGNORE INTO pokemon_game_db.user_roles (user_id, role_id)
VALUES (@ash_user_id, @user_role_id);
-- Creación de tablas de entidades principales

-- Tabla de Roles
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

-- Tabla de Usuarios
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(60) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);

-- Tabla de Lobbies de Juego
CREATE TABLE game_lobbies (
    id CHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    is_public BOOLEAN NOT NULL
);

-- Tabla de Jugadores
CREATE TABLE players (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    game_lobby_id CHAR(36) NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (game_lobby_id) REFERENCES game_lobbies(id)
);

-- Tablas de datos estáticos
CREATE TABLE static_type_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE static_ability_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1024) NOT NULL
);

CREATE TABLE static_move_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    power INT NOT NULL,
    accuracy INT NOT NULL,
    pp INT NOT NULL,
    priority INT NOT NULL,
    damage_class VARCHAR(255) NOT NULL,
    static_type_id BIGINT NOT NULL,
    FOREIGN KEY (static_type_id) REFERENCES static_type_data(id)
);

CREATE TABLE static_pokemon_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    base_hp INT NOT NULL,
    base_attack INT NOT NULL,
    base_defense INT NOT NULL,
    base_special_attack INT NOT NULL,
    base_special_defense INT NOT NULL,
    base_speed INT NOT NULL
);

-- Creación de tablas de unión (Join Tables)

-- Unión User <-> Role
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Uniones de StaticTypeData (relaciones de daño)
CREATE TABLE type_double_damage_from (
    static_type_data_id BIGINT NOT NULL,
    double_damage_from_id BIGINT NOT NULL,
    PRIMARY KEY (static_type_data_id, double_damage_from_id),
    FOREIGN KEY (static_type_data_id) REFERENCES static_type_data(id),
    FOREIGN KEY (double_damage_from_id) REFERENCES static_type_data(id)
);

CREATE TABLE type_half_damage_from (
    static_type_data_id BIGINT NOT NULL,
    half_damage_from_id BIGINT NOT NULL,
    PRIMARY KEY (static_type_data_id, half_damage_from_id),
    FOREIGN KEY (static_type_data_id) REFERENCES static_type_data(id),
    FOREIGN KEY (half_damage_from_id) REFERENCES static_type_data(id)
);

CREATE TABLE type_no_damage_from (
    static_type_data_id BIGINT NOT NULL,
    no_damage_from_id BIGINT NOT NULL,
    PRIMARY KEY (static_type_data_id, no_damage_from_id),
    FOREIGN KEY (static_type_data_id) REFERENCES static_type_data(id),
    FOREIGN KEY (no_damage_from_id) REFERENCES static_type_data(id)
);

-- Uniones de StaticPokemonData
CREATE TABLE pokemon_types (
    static_pokemon_data_id BIGINT NOT NULL,
    types_id BIGINT NOT NULL,
    PRIMARY KEY (static_pokemon_data_id, types_id),
    FOREIGN KEY (static_pokemon_data_id) REFERENCES static_pokemon_data(id),
    FOREIGN KEY (types_id) REFERENCES static_type_data(id)
);

CREATE TABLE pokemon_moves (
    static_pokemon_data_id BIGINT NOT NULL,
    moves_id BIGINT NOT NULL,
    PRIMARY KEY (static_pokemon_data_id, moves_id),
    FOREIGN KEY (static_pokemon_data_id) REFERENCES static_pokemon_data(id),
    FOREIGN KEY (moves_id) REFERENCES static_move_data(id)
);

CREATE TABLE pokemon_abilities (
    static_pokemon_data_id BIGINT NOT NULL,
    abilities_id BIGINT NOT NULL,
    PRIMARY KEY (static_pokemon_data_id, abilities_id),
    FOREIGN KEY (static_pokemon_data_id) REFERENCES static_pokemon_data(id),
    FOREIGN KEY (abilities_id) REFERENCES static_ability_data(id)
);
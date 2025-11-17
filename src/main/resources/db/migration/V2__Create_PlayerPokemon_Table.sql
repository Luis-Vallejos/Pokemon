CREATE TABLE player_pokemons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    static_pokemon_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    level INT NOT NULL DEFAULT 100,
    current_hp INT NOT NULL,
    status_condition VARCHAR(50) NOT NULL DEFAULT 'NONE',
    slot_position INT NOT NULL,
    
    FOREIGN KEY (static_pokemon_id) REFERENCES static_pokemon_data(id),
    FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE TABLE player_pokemon_moves (
    player_pokemon_id BIGINT NOT NULL,
    static_move_id BIGINT NOT NULL,
    PRIMARY KEY (player_pokemon_id, static_move_id),
    FOREIGN KEY (player_pokemon_id) REFERENCES player_pokemons(id),
    FOREIGN KEY (static_move_id) REFERENCES static_move_data(id)
);
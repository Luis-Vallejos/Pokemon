-- 1. Si existía una tabla antigua de relación muchos a muchos con este nombre, bórrala.
-- (Esta era la tabla creada en V2 para la lista simple de movimientos)
DROP TABLE IF EXISTS player_pokemon_moves;

-- 2. Crear la tabla DEFINITIVA para las instancias de movimientos con PPs
CREATE TABLE player_pokemon_moves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_pokemon_id BIGINT NOT NULL,
    static_move_id BIGINT NOT NULL,
    current_pp INT NOT NULL,
    max_pp INT NOT NULL,
    
    FOREIGN KEY (player_pokemon_id) REFERENCES player_pokemons(id),
    FOREIGN KEY (static_move_id) REFERENCES static_move_data(id)
);
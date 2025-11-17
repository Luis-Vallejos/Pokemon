package com.pokemon.game.repository;

import com.pokemon.game.model.PlayerPokemon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Luis
 */
@Repository
public interface PlayerPokemonRepository extends JpaRepository<PlayerPokemon, Long> {

}

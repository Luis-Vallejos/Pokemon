package com.pokemon.game.repository;

import com.pokemon.game.model.StaticPokemonData;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Luis
 */
@Repository
public interface StaticPokemonDataRepository extends JpaRepository<StaticPokemonData, Long> {

    Optional<StaticPokemonData> findByName(String name);
}

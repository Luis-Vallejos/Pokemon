package com.pokemon.game.repository;

import com.pokemon.game.model.StaticAbilityData;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Luis
 */
@Repository
public interface StaticAbilityDataRepository extends JpaRepository<StaticAbilityData, Long> {

    Optional<StaticAbilityData> findByName(String name);
}

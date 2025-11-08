package com.pokemon.game.repository;

import com.pokemon.game.model.StaticTypeData;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Luis
 */
@Repository
public interface StaticTypeDataRepository extends JpaRepository<StaticTypeData, Long> {

    Optional<StaticTypeData> findByName(String name);
}

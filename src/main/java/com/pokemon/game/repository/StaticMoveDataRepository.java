package com.pokemon.game.repository;

import com.pokemon.game.model.StaticMoveData;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Luis
 */
@Repository
public interface StaticMoveDataRepository extends JpaRepository<StaticMoveData, Long> {

    Optional<StaticMoveData> findByName(String name);
}

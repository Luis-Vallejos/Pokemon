package com.pokemon.game.repository;

import com.pokemon.game.model.Player;
import com.pokemon.game.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * Luis
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByUser(User user);
}

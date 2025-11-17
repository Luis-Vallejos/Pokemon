package com.pokemon.game.repository;

import com.pokemon.game.model.GameLobby;
import com.pokemon.game.util.Enums;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * Luis
 */
@Repository
public interface GameLobbyRepository extends JpaRepository<GameLobby, UUID> {

    List<GameLobby> findByIsPublicTrueAndStatus(Enums.GameStatus status);
}

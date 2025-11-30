package com.pokemon.game.service;

import com.pokemon.game.model.Player;
import java.util.List;
import java.util.UUID;

/**
 *
 * Luis
 */
public interface IBattleStateManagerService {

    BattleService createBattle(UUID lobbyId, List<Player> players);

    BattleService getBattle(UUID lobbyId);

    void removeBattle(UUID lobbyId);

    boolean battleExists(UUID lobbyId);
}

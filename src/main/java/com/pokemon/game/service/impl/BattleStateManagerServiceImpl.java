package com.pokemon.game.service.impl;

import com.pokemon.game.model.Player;
import com.pokemon.game.repository.PlayerPokemonRepository;
import com.pokemon.game.repository.StaticMoveDataRepository;
import com.pokemon.game.service.BattleService;
import com.pokemon.game.service.IBattleStateManagerService;
import com.pokemon.game.service.IDamageCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Luis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BattleStateManagerServiceImpl implements IBattleStateManagerService {

    private final IDamageCalculatorService damageCalculatorService;
    private final StaticMoveDataRepository moveRepository;
    private final PlayerPokemonRepository playerPokemonRepository;

    private final Map<UUID, BattleService> activeBattles = new ConcurrentHashMap<>();

    @Override
    public BattleService createBattle(UUID lobbyId, List<Player> players) {
        if (activeBattles.containsKey(lobbyId)) {
            log.warn("Intento de crear batalla duplicada para lobby {}", lobbyId);
            return activeBattles.get(lobbyId);
        }

        log.info("Creando nueva instancia de BattleService para Lobby: {}", lobbyId);

        BattleService newBattle = new BattleService(
                lobbyId,
                players,
                damageCalculatorService,
                moveRepository,
                playerPokemonRepository
        );

        activeBattles.put(lobbyId, newBattle);
        return newBattle;
    }

    @Override
    public BattleService getBattle(UUID lobbyId) {
        return activeBattles.get(lobbyId);
    }

    @Override
    public void removeBattle(UUID lobbyId) {
        if (activeBattles.remove(lobbyId) != null) {
            log.info("Batalla del lobby {} finalizada y eliminada de memoria.", lobbyId);
        } else {
            log.warn("Intento de eliminar batalla inexistente: {}", lobbyId);
        }
    }

    @Override
    public boolean battleExists(UUID lobbyId) {
        return activeBattles.containsKey(lobbyId);
    }
}

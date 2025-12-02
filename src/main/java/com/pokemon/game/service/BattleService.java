package com.pokemon.game.service;

import com.pokemon.game.dto.payload.BattleUpdatePayload;
import com.pokemon.game.model.Player;
import com.pokemon.game.model.PlayerPokemon;
import com.pokemon.game.model.PokemonMove;
import com.pokemon.game.model.StaticMoveData;
import com.pokemon.game.repository.PlayerPokemonRepository;
import com.pokemon.game.repository.StaticMoveDataRepository;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Luis
 */
@Getter
public class BattleService {

    private final UUID lobbyId;
    private final Map<Long, PlayerPokemon> activePokemon;
    private final Map<Long, Player> playersMap;

    private Long currentTurnPlayerId;
    private boolean isFinished;
    private Long winnerId;

    private final IDamageCalculatorService damageCalculatorService;
    private final StaticMoveDataRepository moveRepository;
    private final PlayerPokemonRepository playerPokemonRepository;

    public BattleService(UUID lobbyId,
            List<Player> players,
            IDamageCalculatorService damageCalculatorService,
            StaticMoveDataRepository moveRepository,
            PlayerPokemonRepository playerPokemonRepository) {
        this.lobbyId = lobbyId;
        this.damageCalculatorService = damageCalculatorService;
        this.moveRepository = moveRepository;
        this.playerPokemonRepository = playerPokemonRepository;

        this.activePokemon = new ConcurrentHashMap<>();
        this.playersMap = new ConcurrentHashMap<>();
        this.isFinished = false;

        initializeBattle(players);
    }

    private void initializeBattle(List<Player> players) {
        for (Player player : players) {
            playersMap.put(player.getId(), player);

            PlayerPokemon firstPokemon = player.getTeam().stream()
                    .filter(p -> p.getCurrentHp() > 0)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("El jugador " + player.getUser().getUsername() + " no tiene Pokémon vivos."));

            activePokemon.put(player.getId(), firstPokemon);
        }

        this.currentTurnPlayerId = players.get(0).getId();
    }

    public synchronized BattleUpdatePayload executeTurn(Long actingPlayerId, String moveName) {
        if (isFinished) {
            throw new IllegalStateException("La batalla ya ha terminado.");
        }

        if (!actingPlayerId.equals(currentTurnPlayerId)) {
            throw new IllegalStateException("No es el turno del jugador con ID: " + actingPlayerId);
        }

        PlayerPokemon attacker = activePokemon.get(actingPlayerId);
        Long opponentId = getOpponentId(actingPlayerId);
        PlayerPokemon defender = activePokemon.get(opponentId);

        PokemonMove selectedMoveInstance = attacker.getMoves().stream()
                .filter(m -> m.getStaticMoveData().getName().equalsIgnoreCase(moveName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El Pokémon activo no conoce el movimiento: " + moveName));

        if (selectedMoveInstance.getCurrentPp() <= 0) {
            throw new IllegalArgumentException("No quedan PP para el movimiento: " + moveName);
        }

        selectedMoveInstance.setCurrentPp(selectedMoveInstance.getCurrentPp() - 1);

        StaticMoveData staticData = selectedMoveInstance.getStaticMoveData();

        int damage = damageCalculatorService.calculateDamage(attacker, defender, staticData);

        int oldHp = defender.getCurrentHp();
        int newHp = Math.max(0, oldHp - damage);
        defender.setCurrentHp(newHp);
        playerPokemonRepository.save(defender);
        playerPokemonRepository.save(attacker);

        String message = String.format("¡%s usó %s (PP: %d/%d) y causó %d de daño!",
                attacker.getBasePokemon().getName(),
                staticData.getName(),
                selectedMoveInstance.getCurrentPp(),
                selectedMoveInstance.getMaxPp(),
                damage);

        if (newHp == 0) {
            message += " ¡" + defender.getBasePokemon().getName() + " se debilitó!";
            checkWinCondition(actingPlayerId);
        }

        if (!isFinished) {
            this.currentTurnPlayerId = opponentId;
        }

        return new BattleUpdatePayload(
                playersMap.get(actingPlayerId).getUser().getUsername(),
                staticData.getName(),
                damage,
                message,
                defender.getId(),
                newHp,
                isFinished ? null : currentTurnPlayerId,
                isFinished,
                winnerId
        );
    }

    private Long getOpponentId(Long currentPlayerId) {
        return playersMap.keySet().stream()
                .filter(id -> !id.equals(currentPlayerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontró oponente"));
    }

    private void checkWinCondition(Long potentialWinnerId) {
        Long opponentId = getOpponentId(potentialWinnerId);
        Player opponent = playersMap.get(opponentId);

        boolean hasAlivePokemon = opponent.getTeam().stream()
                .anyMatch(p -> p.getCurrentHp() > 0);

        if (!hasAlivePokemon) {
            this.isFinished = true;
            this.winnerId = potentialWinnerId;
        }
    }
}

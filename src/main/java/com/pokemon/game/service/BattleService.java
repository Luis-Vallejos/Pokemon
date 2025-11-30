package com.pokemon.game.service;

import com.pokemon.game.dto.BattleTurnResultDTO;
import com.pokemon.game.model.Player;
import com.pokemon.game.model.PlayerPokemon;
import com.pokemon.game.model.StaticMoveData;
import com.pokemon.game.repository.StaticMoveDataRepository;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class BattleService {

    private final UUID lobbyId;
    private final Map<Long, PlayerPokemon> activePokemon;
    private int turnNumber;
    private boolean isFinished;
    private Long winnerId;

    private final IDamageCalculatorService damageCalculatorService;
    private final StaticMoveDataRepository moveRepository; // <--- ESTO FALTABA

    public BattleService(UUID lobbyId, List<Player> players,
            IDamageCalculatorService damageCalculatorService,
            StaticMoveDataRepository moveRepository) {
        this.lobbyId = lobbyId;
        this.damageCalculatorService = damageCalculatorService;
        this.moveRepository = moveRepository;
        this.activePokemon = new HashMap<>();
        this.turnNumber = 1;
        this.isFinished = false;

        initializeBattle(players);
    }

    private void initializeBattle(List<Player> players) {
        for (Player player : players) {
            PlayerPokemon firstPokemon = player.getTeam().stream()
                    .filter(p -> p.getCurrentHp() > 0)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("El jugador " + player.getUser().getUsername() + " no tiene Pokémon vivos."));
            activePokemon.put(player.getId(), firstPokemon);
        }
    }

    public BattleTurnResultDTO resolveTurn(Map<Long, String> playerMoves) {
        List<String> log = new ArrayList<>();
        log.add("--- Turno " + turnNumber + " ---");

        Long player1Id = playerMoves.keySet().stream().findFirst().orElseThrow();
        Long player2Id = playerMoves.keySet().stream().skip(1).findFirst().orElseThrow();

        PlayerPokemon p1Pokemon = activePokemon.get(player1Id);
        PlayerPokemon p2Pokemon = activePokemon.get(player2Id);

        StaticMoveData p1Move = moveRepository.findByName(playerMoves.get(player1Id))
                .orElseThrow(() -> new IllegalArgumentException("Movimiento inválido: " + playerMoves.get(player1Id)));
        StaticMoveData p2Move = moveRepository.findByName(playerMoves.get(player2Id))
                .orElseThrow(() -> new IllegalArgumentException("Movimiento inválido: " + playerMoves.get(player2Id)));

        boolean p1GoesFirst = checkSpeed(p1Pokemon, p2Pokemon, p1Move, p2Move);

        PlayerPokemon firstAttacker = p1GoesFirst ? p1Pokemon : p2Pokemon;
        PlayerPokemon secondAttacker = p1GoesFirst ? p2Pokemon : p1Pokemon;
        StaticMoveData firstMove = p1GoesFirst ? p1Move : p2Move;
        StaticMoveData secondMove = p1GoesFirst ? p2Move : p1Move;

        executeAttack(firstAttacker, secondAttacker, firstMove, log);

        if (secondAttacker.getCurrentHp() > 0) {
            executeAttack(secondAttacker, firstAttacker, secondMove, log);
        } else {
            log.add(secondAttacker.getBasePokemon().getName() + " se debilitó!");
            checkWinCondition(firstAttacker.getPlayer().getId());
        }

        if (firstAttacker.getCurrentHp() <= 0) {
            log.add(firstAttacker.getBasePokemon().getName() + " se debilitó!");
            checkWinCondition(secondAttacker.getPlayer().getId());
        }

        if (!isFinished) {
            turnNumber++;
        }

        return new BattleTurnResultDTO(log, true, isFinished, winnerId);
    }

    private void executeAttack(PlayerPokemon attacker, PlayerPokemon defender, StaticMoveData move, List<String> log) {
        log.add(attacker.getBasePokemon().getName() + " usó " + move.getName() + "!");

        // Chequeo de precisión
        if (Math.random() * 100 > move.getAccuracy()) {
            log.add("¡El ataque falló!");
            return;
        }

        int damage = damageCalculatorService.calculateDamage(attacker, defender, move);
        int newHp = Math.max(0, defender.getCurrentHp() - damage);
        defender.setCurrentHp(newHp);
        log.add("¡Hizo " + damage + " de daño!");
    }

    private boolean checkSpeed(PlayerPokemon p1, PlayerPokemon p2, StaticMoveData m1, StaticMoveData m2) {
        if (m1.getPriority() != m2.getPriority()) {
            return m1.getPriority() > m2.getPriority();
        }
        if (p1.getBasePokemon().getBaseSpeed() != p2.getBasePokemon().getBaseSpeed()) {
            return p1.getBasePokemon().getBaseSpeed() > p2.getBasePokemon().getBaseSpeed();
        }
        return Math.random() > 0.5;
    }

    private void checkWinCondition(Long possibleWinnerId) {
        this.isFinished = true;
        this.winnerId = possibleWinnerId;
    }
}

package com.pokemon.game.controller;

import com.pokemon.game.dto.BattleActionDTO;
import com.pokemon.game.dto.payload.BattleUpdatePayload;
import com.pokemon.game.model.Player;
import com.pokemon.game.model.User;
import com.pokemon.game.repository.PlayerRepository;
import com.pokemon.game.repository.UserRepository;
import com.pokemon.game.service.BattleService;
import com.pokemon.game.service.IBattleStateManagerService;
import com.pokemon.game.service.IGameLobbyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 *
 * Luis
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class BattleController {

    private final IBattleStateManagerService battleStateManager;
    private final IGameLobbyService gameLobbyService;
    private final SimpMessagingTemplate messagingTemplate;

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;

    @MessageMapping("/battle/{lobbyId}/action")
    public void handleBattleAction(
            @DestinationVariable UUID lobbyId,
            @Payload BattleActionDTO action,
            Principal principal
    ) {
        String username = principal.getName();
        log.info("Acción recibida de {}: {} en lobby {}", username, action.moveName(), lobbyId);

        if (!battleStateManager.battleExists(lobbyId)) {
            sendError(username, "La batalla no existe o ha terminado.");
            return;
        }

        BattleService battle = battleStateManager.getBattle(lobbyId);

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            Player player = playerRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Jugador no encontrado"));

            BattleUpdatePayload updatePayload = battle.executeTurn(player.getId(), action.moveName());

            log.info("Acción válida. Daño: {}. Siguiente turno: ID {}", updatePayload.damageDealt(), updatePayload.nextTurnPlayerId());

            for (Player battlePlayer : battle.getPlayersMap().values()) {
                String playerUsername = battlePlayer.getUser().getUsername();
                messagingTemplate.convertAndSendToUser(
                        playerUsername,
                        "/queue/battle-update",
                        updatePayload
                );
            }

            if (updatePayload.matchFinished()) {
                log.info("Batalla {} finalizada. Ganador ID: {}", lobbyId, updatePayload.winnerId());

                gameLobbyService.finishGame(lobbyId);

                for (Player battlePlayer : battle.getPlayersMap().values()) {
                    String resultMessage = battlePlayer.getId().equals(updatePayload.winnerId()) ? "VICTORIA" : "DERROTA";
                    messagingTemplate.convertAndSendToUser(
                            battlePlayer.getUser().getUsername(),
                            "/queue/game-result",
                            resultMessage
                    );
                }

                battleStateManager.removeBattle(lobbyId);
            }

        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Acción rechazada para {}: {}", username, e.getMessage());
            sendError(username, "Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en batalla", e);
            sendError(username, "Error interno del servidor.");
        }
    }

    private void sendError(String username, String errorMessage) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/errors",
                errorMessage
        );
    }
}

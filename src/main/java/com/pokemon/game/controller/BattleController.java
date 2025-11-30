package com.pokemon.game.controller;

import com.pokemon.game.dto.BattleActionDTO;
import com.pokemon.game.dto.BattleTurnResultDTO;
import com.pokemon.game.model.Player;
import com.pokemon.game.model.User;
import com.pokemon.game.repository.PlayerRepository;
import com.pokemon.game.repository.UserRepository;
import com.pokemon.game.service.BattleService;
import com.pokemon.game.service.IBattleStateManagerService;
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
            log.error("La batalla para el lobby {} no existe.", lobbyId);
            return;
        }
        BattleService battle = battleStateManager.getBattle(lobbyId);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Player player = playerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado"));

        BattleTurnResultDTO turnResult = battle.registerPlayerMove(player.getId(), action.moveName());

        if (turnResult != null) {
            log.info("Turno resuelto para lobby {}. Transmitiendo resultados.", lobbyId);
            messagingTemplate.convertAndSend("/topic/battle/" + lobbyId, turnResult);

            if (turnResult.matchFinished()) {
                log.info("Batalla {} finalizada. Ganador: {}", lobbyId, turnResult.winnerId());
                battleStateManager.removeBattle(lobbyId);
            }
        } else {
            log.info("Esperando acción del oponente en lobby {}", lobbyId);
        }
    }
}

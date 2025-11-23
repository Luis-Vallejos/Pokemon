package com.pokemon.game.service.impl;

import com.pokemon.game.model.GameLobby;
import com.pokemon.game.model.Player;
import com.pokemon.game.model.User;
import com.pokemon.game.repository.GameLobbyRepository;
import com.pokemon.game.service.IMatchmakingService;
import com.pokemon.game.service.IPlayerService;
import com.pokemon.game.util.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * Luis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchmakingServiceImpl implements IMatchmakingService {

    private final Queue<User> publicMatchmakingQueue = new ConcurrentLinkedQueue<>();
    private static final int REQUIRED_PLAYERS = 2;

    private final SimpMessagingTemplate messagingTemplate;
    private final IPlayerService playerService;
    private final GameLobbyRepository gameLobbyRepository;

    @Override
    public void addToPublicQueue(User user) {
        if (publicMatchmakingQueue.stream().anyMatch(u -> u.getId().equals(user.getId()))) {
            log.warn("El usuario {} ya está en la cola de matchmaking.", user.getUsername());
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/match-found",
                    "STATUS: Ya estás buscando partida."
            );
            return;
        }

        log.info("Añadiendo usuario {} a la cola de matchmaking. Tamaño actual: {}", user.getUsername(), publicMatchmakingQueue.size() + 1);
        publicMatchmakingQueue.add(user);

        if (publicMatchmakingQueue.size() >= REQUIRED_PLAYERS) {
            log.info("Jugadores encontrados. Intentando crear partida.");
            try {
                processMatchmakingQueue();
            } catch (Exception e) {
                log.error("Error al procesar la cola de matchmaking:", e);
            }
        }
    }

    @Override
    public void removeFromPublicQueue(User user) {
        boolean removed = publicMatchmakingQueue.removeIf(u -> u.getId().equals(user.getId()));
        if (removed) {
            log.info("Usuario {} eliminado de la cola de matchmaking. Tamaño restante: {}", user.getUsername(), publicMatchmakingQueue.size());
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/match-found",
                    "STATUS: Búsqueda de partida cancelada."
            );
        }
    }

    @Transactional
    private void processMatchmakingQueue() {
        if (publicMatchmakingQueue.size() < REQUIRED_PLAYERS) {
            return;
        }

        User user1 = publicMatchmakingQueue.poll();
        User user2 = publicMatchmakingQueue.poll();

        if (user1 == null || user2 == null) {
            log.error("Error lógico: la cola se vació inesperadamente durante la extracción.");
            if (user1 != null) {
                publicMatchmakingQueue.add(user1);
            }
            return;
        }

        log.info("Emparejados: {} y {}", user1.getUsername(), user2.getUsername());

        Player player1 = playerService.findOrCreatePlayerForUser(user1);
        Player player2 = playerService.findOrCreatePlayerForUser(user2);

        GameLobby newLobby = GameLobby.builder()
                .status(Enums.GameStatus.WAITING)
                .isPublic(true)
                .build();

        newLobby.addPlayer(player1);
        newLobby.addPlayer(player2);
        newLobby = gameLobbyRepository.save(newLobby);
        UUID lobbyId = newLobby.getId();
        log.info("Lobby de partida pública creado: {}", lobbyId);

        String messagePayload = "MATCH_FOUND:" + lobbyId.toString();

        messagingTemplate.convertAndSendToUser(
                user1.getUsername(),
                "/queue/match-found",
                messagePayload
        );
        log.info("Notificación de partida enviada a {} en /queue/match-found. Lobby ID: {}", user1.getUsername(), lobbyId);

        messagingTemplate.convertAndSendToUser(
                user2.getUsername(),
                "/queue/match-found",
                messagePayload
        );
        log.info("Notificación de partida enviada a {} en /queue/match-found. Lobby ID: {}", user2.getUsername(), lobbyId);
    }
}

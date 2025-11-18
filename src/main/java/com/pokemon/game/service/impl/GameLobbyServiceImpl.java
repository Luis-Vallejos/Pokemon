package com.pokemon.game.service.impl;

import com.pokemon.game.dto.GameLobbyDTO;
import com.pokemon.game.model.GameLobby;
import com.pokemon.game.model.Player;
import com.pokemon.game.model.User;
import com.pokemon.game.repository.GameLobbyRepository;
import com.pokemon.game.service.IGameLobbyService;
import com.pokemon.game.service.IPlayerService;
import com.pokemon.game.util.Enums;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * Luis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameLobbyServiceImpl implements IGameLobbyService {

    private final GameLobbyRepository gameLobbyRepository;
    private final IPlayerService playerService;

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public GameLobbyDTO createAndBroadcastPublicLobby() {
        User currentUser = playerService.getCurrentAuthenticatedUser();
        Player player = playerService.findOrCreatePlayerForUser(currentUser);

        GameLobby newLobby = GameLobby.builder()
                .status(Enums.GameStatus.WAITING)
                .isPublic(true)
                .build();

        newLobby.addPlayer(player);
        newLobby = gameLobbyRepository.save(newLobby);
        log.info("Lobby creado: {}", newLobby.getId());

        GameLobbyDTO lobbyDTO = GameLobbyDTO.fromEntity(newLobby);

        messagingTemplate.convertAndSend("/topic/lobby", lobbyDTO);

        return lobbyDTO;
    }

    @Override
    @Transactional
    public GameLobbyDTO joinLobby(UUID lobbyId) {
        User currentUser = playerService.getCurrentAuthenticatedUser();
        Player joiningPlayer = playerService.findOrCreatePlayerForUser(currentUser);
        String joiningUsername = currentUser.getUsername();

        GameLobby lobby = gameLobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new IllegalArgumentException("Lobby no encontrado con ID: " + lobbyId));

        if (lobby.getStatus() != Enums.GameStatus.WAITING) {
            throw new IllegalStateException("No se puede unir: el lobby ya está en juego o ha terminado.");
        }

        lobby.addPlayer(joiningPlayer);
        GameLobby updatedLobby = gameLobbyRepository.save(lobby);
        log.info("Jugador {} se ha unido al lobby {}", joiningUsername, updatedLobby.getId());

        GameLobbyDTO updatedLobbyDTO = GameLobbyDTO.fromEntity(updatedLobby);

        String topicDestination = "/topic/lobby/" + lobbyId.toString();
        messagingTemplate.convertAndSend(topicDestination, updatedLobbyDTO);
        log.info("Lobby {} actualizado y difundido a: {}", lobbyId, topicDestination);

        String welcomeMessage = String.format("Bienvenido al lobby %s, %s. Disfruta la partida.", lobbyId.toString(), joiningUsername);

        messagingTemplate.convertAndSendToUser(
                joiningUsername,
                "/queue/join-response",
                welcomeMessage
        );
        log.info("Mensaje privado enviado a {} en /queue/join-response", joiningUsername);

        return updatedLobbyDTO;
    }
}

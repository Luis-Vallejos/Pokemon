package com.pokemon.game.service.impl;

import com.pokemon.game.dto.GameLobbyDTO;
import com.pokemon.game.dto.TeamSetupDTO;
import com.pokemon.game.model.GameLobby;
import com.pokemon.game.model.Player;
import com.pokemon.game.model.User;
import com.pokemon.game.repository.GameLobbyRepository;
import com.pokemon.game.service.IGameLobbyService;
import com.pokemon.game.service.IPlayerService;
import com.pokemon.game.service.ITeamService;
import com.pokemon.game.util.Enums;
import java.util.UUID;
import java.util.Map;
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
    private final ITeamService teamService;

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
        log.info("Lobby PÚBLICO creado: {}", newLobby.getId());

        GameLobbyDTO lobbyDTO = GameLobbyDTO.fromEntity(newLobby);
        messagingTemplate.convertAndSend("/topic/lobby", lobbyDTO);

        return lobbyDTO;
    }

    @Override
    @Transactional
    public GameLobbyDTO createPrivateLobby() {
        User currentUser = playerService.getCurrentAuthenticatedUser();
        Player player = playerService.findOrCreatePlayerForUser(currentUser);

        GameLobby newLobby = GameLobby.builder()
                .status(Enums.GameStatus.WAITING)
                .isPublic(false)
                .build();

        newLobby.addPlayer(player);
        newLobby = gameLobbyRepository.save(newLobby);
        log.info("Lobby PRIVADO creado: {}", newLobby.getId());

        GameLobbyDTO lobbyDTO = GameLobbyDTO.fromEntity(newLobby);

        messagingTemplate.convertAndSendToUser(
                currentUser.getUsername(),
                "/queue/lobby-created",
                lobbyDTO
        );

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

        boolean alreadyInLobby = lobby.getPlayers().stream()
                .anyMatch(p -> p.getUser().getUsername().equals(joiningUsername));

        if (alreadyInLobby) {
            return GameLobbyDTO.fromEntity(lobby);
        }

        if (lobby.getStatus() != Enums.GameStatus.WAITING) {
            throw new IllegalStateException("No se puede unir: el lobby ya está en juego o ha terminado.");
        }

        if (lobby.getPlayers().size() >= 2) {
            throw new IllegalStateException("El lobby está lleno.");
        }

        lobby.addPlayer(joiningPlayer);
        GameLobby updatedLobby = gameLobbyRepository.save(lobby);
        log.info("Jugador {} se ha unido al lobby {}", joiningUsername, updatedLobby.getId());

        GameLobbyDTO updatedLobbyDTO = GameLobbyDTO.fromEntity(updatedLobby);

        String topicDestination = "/topic/lobby/" + lobbyId.toString();
        messagingTemplate.convertAndSend(topicDestination, updatedLobbyDTO);

        String welcomeMessage = String.format("Bienvenido al lobby %s, %s.", lobbyId.toString(), joiningUsername);
        messagingTemplate.convertAndSendToUser(
                joiningUsername,
                "/queue/join-response",
                welcomeMessage
        );

        return updatedLobbyDTO;
    }

    @Override
    @Transactional
    public void processTeamSelection(UUID lobbyId, TeamSetupDTO teamSetup) {
        User currentUser = playerService.getCurrentAuthenticatedUser();
        String username = currentUser.getUsername();

        log.info("Procesando selección de equipo para usuario: {} en lobby: {}", username, lobbyId);

        GameLobby lobby = gameLobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new IllegalArgumentException("Lobby no encontrado"));

        boolean isPlayerInLobby = lobby.getPlayers().stream()
                .anyMatch(p -> p.getUser().getUsername().equals(username));

        if (!isPlayerInLobby) {
            throw new SecurityException("El jugador no pertenece a este lobby.");
        }

        teamService.createPlayerTeam(teamSetup);

        Map<String, Object> readyMessage = Map.of(
                "type", "PLAYER_READY",
                "username", username,
                "lobbyId", lobbyId
        );

        String gameTopic = "/topic/game/" + lobbyId.toString();
        messagingTemplate.convertAndSend(gameTopic, readyMessage);

        log.info("Equipo guardado y notificación PLAYER_READY enviada a {}", gameTopic);
    }
}

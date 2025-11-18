package com.pokemon.game.service.impl;

import com.pokemon.game.dto.GameLobbyDTO;
import com.pokemon.game.model.GameLobby;
import com.pokemon.game.model.Player;
import com.pokemon.game.model.User;
import com.pokemon.game.repository.GameLobbyRepository;
import com.pokemon.game.service.IGameLobbyService;
import com.pokemon.game.service.IPlayerService;
import com.pokemon.game.util.Enums;
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
        log.info("Lobby {} difundido a /topic/lobby", lobbyDTO.id());

        return lobbyDTO;
    }
}

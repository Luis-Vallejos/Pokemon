package com.pokemon.game.controller;

import com.pokemon.game.dto.GameLobbyDTO;
import com.pokemon.game.dto.TeamSetupDTO;
import com.pokemon.game.model.User;
import com.pokemon.game.service.IGameLobbyService;
import com.pokemon.game.service.IMatchmakingService;
import com.pokemon.game.service.IPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 *
 * Luis
 */
@Controller
@RequiredArgsConstructor
public class LobbyController {

    private final IGameLobbyService lobbyService;
    private final IMatchmakingService matchmakingService;
    private final IPlayerService playerService;

    @MessageMapping("/lobby.create")
    public GameLobbyDTO createPublicLobby(@AuthenticationPrincipal UserDetails principal) {
        return lobbyService.createAndBroadcastPublicLobby();
    }

    @MessageMapping("/lobby/create")
    public void createPrivateLobby(Principal principal) {
        lobbyService.createPrivateLobby();
    }

    @MessageMapping("/lobby/join/{lobbyId}")
    public void joinPrivateLobby(@DestinationVariable UUID lobbyId, Principal principal) {
        lobbyService.joinLobby(lobbyId);
    }

    @MessageMapping("/lobby/{lobbyId}/select-team")
    public void selectTeam(@DestinationVariable UUID lobbyId,
            @Payload TeamSetupDTO teamSetup,
            Principal principal) {
        lobbyService.processTeamSelection(lobbyId, teamSetup);
    }

    @MessageMapping("/matchmaking/find")
    public void findMatch(Principal principal) {
        User user = playerService.getCurrentAuthenticatedUser();
        matchmakingService.addToPublicQueue(user);
    }

    @MessageMapping("/matchmaking/cancel")
    public void cancelMatch(Principal principal) {
        User user = playerService.getCurrentAuthenticatedUser();
        matchmakingService.removeFromPublicQueue(user);
    }
}

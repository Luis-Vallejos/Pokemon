package com.pokemon.game.controller;

import com.pokemon.game.dto.GameLobbyDTO;
import com.pokemon.game.model.User;
import com.pokemon.game.request.JoinLobbyRequest;
import com.pokemon.game.service.IGameLobbyService;
import com.pokemon.game.service.IMatchmakingService;
import com.pokemon.game.service.IPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.security.Principal;

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

    @MessageMapping("/lobby.join")
    public GameLobbyDTO joinLobby(@Payload JoinLobbyRequest request, @AuthenticationPrincipal UserDetails principal) {
        return lobbyService.joinLobby(request.lobbyId());
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

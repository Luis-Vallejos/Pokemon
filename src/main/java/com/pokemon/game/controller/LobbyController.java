package com.pokemon.game.controller;

import com.pokemon.game.dto.GameLobbyDTO;
import com.pokemon.game.request.JoinLobbyRequest;
import com.pokemon.game.service.IGameLobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

/**
 *
 * Luis
 */
@Controller
@RequiredArgsConstructor
public class LobbyController {

    private final IGameLobbyService lobbyService;

    @MessageMapping("/lobby.create")
    public GameLobbyDTO createPublicLobby(@AuthenticationPrincipal UserDetails principal) {
        return lobbyService.createAndBroadcastPublicLobby();
    }

    @MessageMapping("/lobby.join")
    public GameLobbyDTO joinLobby(@Payload JoinLobbyRequest request, @AuthenticationPrincipal UserDetails principal) {
        return lobbyService.joinLobby(request.lobbyId());
    }
}

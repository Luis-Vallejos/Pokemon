package com.pokemon.game.request;

import java.util.UUID;

/**
 *
 * @author Luis
 */
public record JoinLobbyRequest(
        UUID lobbyId
) {
}
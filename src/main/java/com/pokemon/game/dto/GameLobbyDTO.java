package com.pokemon.game.dto;

import com.pokemon.game.model.GameLobby;
import com.pokemon.game.util.Enums;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Luis
 */
public record GameLobbyDTO(
        UUID id,
        Enums.GameStatus status,
        boolean isPublic,
        int currentPlayerCount,
        List<String> playerNames) {

    public static GameLobbyDTO fromEntity(GameLobby lobby) {
        List<String> names = lobby.getPlayers().stream()
                .map(player -> player.getUser().getUsername())
                .collect(Collectors.toList());

        return new GameLobbyDTO(
                lobby.getId(),
                lobby.getStatus(),
                lobby.isPublic(),
                lobby.getPlayers().size(),
                names
        );
    }
}

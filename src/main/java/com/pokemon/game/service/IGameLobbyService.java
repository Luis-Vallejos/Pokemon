package com.pokemon.game.service;

import com.pokemon.game.dto.GameLobbyDTO;
import java.util.UUID;

/**
 *
 * Luis
 */
public interface IGameLobbyService {

    GameLobbyDTO createAndBroadcastPublicLobby();

    GameLobbyDTO joinLobby(UUID lobbyId);
}

package com.pokemon.game.service;

import com.pokemon.game.dto.GameLobbyDTO;

/**
 *
 * Luis
 */
public interface IGameLobbyService {

    GameLobbyDTO createAndBroadcastPublicLobby();
}

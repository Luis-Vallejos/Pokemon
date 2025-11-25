package com.pokemon.game.service;

import com.pokemon.game.dto.GameLobbyDTO;
import com.pokemon.game.dto.TeamSetupDTO;
import java.util.UUID;

/**
 *
 * Luis
 */
public interface IGameLobbyService {

    GameLobbyDTO createAndBroadcastPublicLobby();

    GameLobbyDTO createPrivateLobby();

    GameLobbyDTO joinLobby(UUID lobbyId);

    void processTeamSelection(UUID lobbyId, TeamSetupDTO teamSetup);
}

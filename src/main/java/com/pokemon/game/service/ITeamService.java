package com.pokemon.game.service;

import com.pokemon.game.dto.PlayerPokemonDTO;
import com.pokemon.game.dto.TeamSetupDTO;
import java.util.List;

public interface ITeamService {

    List<PlayerPokemonDTO> createPlayerTeam(TeamSetupDTO request);
}

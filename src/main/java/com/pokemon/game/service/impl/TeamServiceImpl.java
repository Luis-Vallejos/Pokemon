package com.pokemon.game.service.impl;

import com.pokemon.game.model.Player;
import com.pokemon.game.model.PlayerPokemon;
import com.pokemon.game.model.StaticPokemonData;
import com.pokemon.game.repository.PlayerPokemonRepository;
import com.pokemon.game.repository.StaticPokemonDataRepository;
import com.pokemon.game.dto.PlayerPokemonDTO;
import com.pokemon.game.dto.TeamSetupDTO;
import com.pokemon.game.service.IPlayerService;
import com.pokemon.game.service.ITeamService;
import com.pokemon.game.util.Enums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements ITeamService {

    private final IPlayerService playerService;
    private final PlayerPokemonRepository playerPokemonRepository;
    private final StaticPokemonDataRepository staticPokemonDataRepository;

    @Override
    @Transactional
    public List<PlayerPokemonDTO> createPlayerTeam(TeamSetupDTO request) {
        Player currentPlayer = playerService.getCurrentPlayer();

        if (request.pokemonNames() == null || request.pokemonNames().size() != 6) {
            throw new IllegalArgumentException("Se requiere un equipo de exactamente 6 Pokémon.");
        }

        List<PlayerPokemon> newTeam = new ArrayList<>();
        int slot = 1;

        for (String pokemonName : request.pokemonNames()) {
            StaticPokemonData staticData = staticPokemonDataRepository.findByName(pokemonName)
                    .orElseThrow(() -> new IllegalArgumentException("Pokémon no encontrado: " + pokemonName));

            int maxHp = staticData.getBaseHp();

            PlayerPokemon newInstance = PlayerPokemon.builder()
                    .player(currentPlayer)
                    .basePokemon(staticData)
                    .level(100)
                    .currentHp(maxHp)
                    .statusCondition(Enums.StatusCondition.NONE)
                    .slot(slot++)
                    .build();

            newTeam.add(newInstance);
        }

        playerPokemonRepository.saveAll(newTeam);

        currentPlayer.getTeam().clear();
        currentPlayer.getTeam().addAll(newTeam);

        return newTeam.stream()
                .map(PlayerPokemonDTO::fromEntity)
                .collect(Collectors.toList());
    }
}

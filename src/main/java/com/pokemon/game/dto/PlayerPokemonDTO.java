package com.pokemon.game.dto;

import com.pokemon.game.model.PlayerPokemon;
import com.pokemon.game.util.Enums;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Luis
 */
public record PlayerPokemonDTO(
        Long id,
        String basePokemonName,
        int level,
        int currentHp,
        int maxHp,
        Enums.StatusCondition status,
        int slot,
        Set<String> moves) {

    public static PlayerPokemonDTO fromEntity(PlayerPokemon entity) {
        int maxHp = entity.getBasePokemon().getBaseHp();

        return new PlayerPokemonDTO(
                entity.getId(),
                entity.getBasePokemon().getName(),
                entity.getLevel(),
                entity.getCurrentHp(),
                maxHp,
                entity.getStatusCondition(),
                entity.getSlot(),
                entity.getMoves().stream()
                        .map(pokemonMove -> pokemonMove.getStaticMoveData().getName())
                        .collect(Collectors.toSet())
        );
    }
}

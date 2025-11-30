package com.pokemon.game.service;

import com.pokemon.game.model.PlayerPokemon;
import com.pokemon.game.model.StaticMoveData;

/**
 * 
 * Luis
 */
public interface IDamageCalculatorService {

    int calculateDamage(PlayerPokemon attacker, PlayerPokemon defender, StaticMoveData move);
}

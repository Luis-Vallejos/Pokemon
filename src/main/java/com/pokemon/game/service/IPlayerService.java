package com.pokemon.game.service;

import com.pokemon.game.model.Player;
import com.pokemon.game.model.User;

/**
 *
 * Luis
 */
public interface IPlayerService {

    User getCurrentAuthenticatedUser();

    Player getCurrentPlayer();

    Player findOrCreatePlayerForUser(User user);
}

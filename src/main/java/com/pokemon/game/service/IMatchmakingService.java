package com.pokemon.game.service;

import com.pokemon.game.model.User;

/**
 *
 * Luis
 */
public interface IMatchmakingService {

    void addToPublicQueue(User user);

    void removeFromPublicQueue(User user);
}

package com.pokemon.game.request;

/**
 *
 * @author Luis
 */
public record RegisterRequest(
        String username,
        String email,
        String password) {

}

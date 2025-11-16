package com.pokemon.game.request;

public record RegisterRequest(
        String username,
        String email,
        String password) {

}

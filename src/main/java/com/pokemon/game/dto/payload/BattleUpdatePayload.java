package com.pokemon.game.dto.payload;

/**
 *
 * Luis
 */
public record BattleUpdatePayload(
        String playerName,      // Nombre del jugador que hizo la acción
        String moveName,        // Movimiento usado
        int damageDealt,        // Daño realizado
        String message,         // Mensaje narrativo (ej: "¡Es súper efectivo!")
        Long targetPokemonId,   // ID del pokemon que recibió daño
        int targetNewHp,        // Nueva vida del objetivo
        Long nextTurnPlayerId,  // ID del jugador que le toca ahora
        boolean matchFinished,  // ¿Terminó la partida?
        Long winnerId           // ID del ganador (si terminó)
        ) {

}

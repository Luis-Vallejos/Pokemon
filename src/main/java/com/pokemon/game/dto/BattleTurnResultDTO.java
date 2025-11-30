package com.pokemon.game.dto;

import java.util.List;

/**
 *
 * @author Luis
 */
public record BattleTurnResultDTO(
        List<String> battleLog, // Mensajes: "Pikachu usó Rayo", "Es súper efectivo"
        boolean turnFinished,   // Si el turno concluyó correctamente
        boolean matchFinished,  // Si alguien ganó
        Long winnerId           // ID del ganador (si lo hay)
        ) {

}

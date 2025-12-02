package com.pokemon.game.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_pokemon_moves", schema = "pokemon_game_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PokemonMove {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "current_pp", nullable = false)
    private int currentPp;

    @Column(name = "max_pp", nullable = false)
    private int maxPp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_pokemon_id", nullable = false)
    private PlayerPokemon playerPokemon;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "static_move_id", nullable = false)
    private StaticMoveData staticMoveData;
}

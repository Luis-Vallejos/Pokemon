package com.pokemon.game.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.pokemon.game.util.Enums.StatusCondition;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * Luis
 */
@Entity
@Table(name = "player_pokemons", schema = "pokemon_game_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerPokemon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Builder.Default
    @Column(nullable = false)
    private int level = 100;

    @Column(name = "current_hp", nullable = false)
    private int currentHp;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status_condition", nullable = false)
    private StatusCondition statusCondition = StatusCondition.NONE;

    @Column(name = "slot_position", nullable = false)
    private int slot;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "static_pokemon_id", nullable = false)
    private StaticPokemonData basePokemon;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Builder.Default
    @OneToMany(
            mappedBy = "playerPokemon",
            cascade = CascadeType.ALL, 
            fetch = FetchType.EAGER,
            orphanRemoval = true 
    )
    private Set<PokemonMove> moves = new HashSet<>();


    public void addMove(PokemonMove move) {
        this.moves.add(move);
        move.setPlayerPokemon(this);
    }
}

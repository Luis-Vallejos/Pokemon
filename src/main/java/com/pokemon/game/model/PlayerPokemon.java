package com.pokemon.game.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.pokemon.game.util.Enums.StatusCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author Luis
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

    // Conexiones
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "static_pokemon_id", nullable = false)
    private StaticPokemonData basePokemon;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "player_pokemon_moves",
            joinColumns = @JoinColumn(name = "player_pokemon_id"),
            inverseJoinColumns = @JoinColumn(name = "static_move_id")
    )
    private Set<StaticMoveData> moves = new HashSet<>();
}

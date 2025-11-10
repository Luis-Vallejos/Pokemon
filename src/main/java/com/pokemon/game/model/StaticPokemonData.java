package com.pokemon.game.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author Luis
 */
@Entity
@Table(name = "static_pokemon_data", schema = "pokemon_game_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StaticPokemonData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "baseHp", nullable = false)
    private int baseHp;

    @Column(name = "baseAttack", nullable = false)
    private int baseAttack;

    @Column(name = "baseDefense", nullable = false)
    private int baseDefense;

    @Column(name = "baseSpecialAttack", nullable = false)
    private int baseSpecialAttack;

    @Column(name = "baseSpecialDefense", nullable = false)
    private int baseSpecialDefense;

    @Column(name = "baseSpeed", nullable = false)
    private int baseSpeed;

    // Conexiones
    @Builder.Default
    @ManyToMany
    @JoinTable(name = "pokemon_types")
    private Set<StaticTypeData> types = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "pokemon_moves")
    private Set<StaticMoveData> moves = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "pokemon_abilities")
    private Set<StaticAbilityData> abilities = new HashSet<>();
}

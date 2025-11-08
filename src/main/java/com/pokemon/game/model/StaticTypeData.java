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
@Table(name = "static_type_data", schema = "pokemon_game_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StaticTypeData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    // Conexioes
    @Builder.Default
    @ManyToMany
    @JoinTable(name = "type_double_damage_from")
    private Set<StaticTypeData> doubleDamageFrom = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "type_half_damage_from")
    private Set<StaticTypeData> halfDamageFrom = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "type_no_damage_from")
    private Set<StaticTypeData> noDamageFrom = new HashSet<>();
}

package com.pokemon.game.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
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
@Table(name = "static_move_data", schema = "pokemon_game_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StaticMoveData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "power", nullable = false)
    private int power;

    @Column(name = "accuracy", nullable = false)
    private int accuracy;

    @Column(name = "pp", nullable = false)
    private int pp;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "damage_class", nullable = false)
    private String damageClass;

    // Conexiones
    @ManyToOne
    @JoinColumn(name = "static_type_id", nullable = false)
    private StaticTypeData type;
}

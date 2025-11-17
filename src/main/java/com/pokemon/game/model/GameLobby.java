package com.pokemon.game.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.pokemon.game.util.Enums;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
@Table(name = "game_lobbies", schema = "pokemon_game_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameLobby {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.GameStatus status;

    @Column(nullable = false)
    private boolean isPublic;

    @JsonManagedReference
    @OneToMany(
            mappedBy = "gameLobby",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Player> players = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public void addPlayer(Player player) {
        players.add(player);
        player.setGameLobby(this);
    }

    public void removePlayer(Player player) {
        players.remove(player);
        player.setGameLobby(null);
    }
}

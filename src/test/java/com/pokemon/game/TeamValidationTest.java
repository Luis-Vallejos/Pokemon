package com.pokemon.game;

import com.pokemon.game.dto.TeamSetupDTO;
import com.pokemon.game.model.Player;
import com.pokemon.game.model.StaticPokemonData;
import com.pokemon.game.model.User;
import com.pokemon.game.repository.PlayerPokemonRepository;
import com.pokemon.game.repository.StaticPokemonDataRepository;
import com.pokemon.game.service.IPlayerService;
import com.pokemon.game.service.impl.TeamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamValidationTest {

    @Mock
    private IPlayerService playerService;
    @Mock
    private PlayerPokemonRepository playerPokemonRepository;
    @Mock
    private StaticPokemonDataRepository staticPokemonDataRepository;

    @InjectMocks
    private TeamServiceImpl teamService;

    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        mockPlayer = Player.builder()
                .id(1L)
                .user(User.builder().username("Ash").build())
                .build();
    }

    @Test
    @DisplayName("Debe fallar (Excepción) si la lista es NULL")
    void testCreateTeam_NullList_ThrowsException() {
        when(playerService.getCurrentPlayer()).thenReturn(mockPlayer);
        TeamSetupDTO invalidDto = new TeamSetupDTO(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            teamService.createPlayerTeam(invalidDto);
        });

        assertEquals("Se requiere un equipo de exactamente 6 Pokémon.", exception.getMessage());
    }

    @Test
    @DisplayName("Debe fallar si envían MENOS de 6 Pokémon")
    void testCreateTeam_TooFew_ThrowsException() {
        when(playerService.getCurrentPlayer()).thenReturn(mockPlayer);
        TeamSetupDTO invalidDto = new TeamSetupDTO(Arrays.asList("Pikachu", "Charizard"));

        assertThrows(IllegalArgumentException.class, () -> {
            teamService.createPlayerTeam(invalidDto);
        });
    }

    @Test
    @DisplayName("Debe fallar si envían MÁS de 6 Pokémon")
    void testCreateTeam_TooMany_ThrowsException() {
        when(playerService.getCurrentPlayer()).thenReturn(mockPlayer);
        TeamSetupDTO invalidDto = new TeamSetupDTO(Arrays.asList(
                "1", "2", "3", "4", "5", "6", "7"
        ));

        assertThrows(IllegalArgumentException.class, () -> {
            teamService.createPlayerTeam(invalidDto);
        });
    }

    @Test
    @DisplayName("ÉXITO: Debe guardar si son EXACTAMENTE 6 Pokémon válidos")
    void testCreateTeam_ValidTeam_Success() {
        when(playerService.getCurrentPlayer()).thenReturn(mockPlayer);

        List<String> validTeam = Arrays.asList("Pikachu", "Bulbasaur", "Charmander", "Squirtle", "Pidgey", "Rattata");
        TeamSetupDTO validDto = new TeamSetupDTO(validTeam);

        StaticPokemonData mockData = StaticPokemonData.builder().baseHp(100).name("Dummy").build();
        when(staticPokemonDataRepository.findByName(anyString())).thenReturn(Optional.of(mockData));

        teamService.createPlayerTeam(validDto);

        verify(playerPokemonRepository, times(1)).saveAll(anyList());
    }
}

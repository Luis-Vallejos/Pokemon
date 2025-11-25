package com.pokemon.game;

import com.pokemon.game.dto.GameLobbyDTO;
import com.pokemon.game.dto.TeamSetupDTO;
import com.pokemon.game.model.GameLobby;
import com.pokemon.game.model.Player;
import com.pokemon.game.model.User;
import com.pokemon.game.repository.GameLobbyRepository;
import com.pokemon.game.service.IPlayerService;
import com.pokemon.game.service.ITeamService;
import com.pokemon.game.service.impl.GameLobbyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PrivateLobbyServiceTest {

    @Mock
    private GameLobbyRepository gameLobbyRepository;
    @Mock
    private IPlayerService playerService;
    @Mock
    private ITeamService teamService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GameLobbyServiceImpl gameLobbyService;

    private User mockUser;
    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().username("TrainerRed").build();
        mockPlayer = Player.builder().id(1L).user(mockUser).build();
    }

    @Test
    @DisplayName("Crear Lobby Privado: Debe tener isPublic=false y notificar al creador")
    void testCreatePrivateLobby() {
        when(playerService.getCurrentAuthenticatedUser()).thenReturn(mockUser);
        when(playerService.findOrCreatePlayerForUser(mockUser)).thenReturn(mockPlayer);
        when(gameLobbyRepository.save(any(GameLobby.class))).thenAnswer(i -> {
            GameLobby l = i.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        GameLobbyDTO result = gameLobbyService.createPrivateLobby();

        assertFalse(result.isPublic(), "El lobby debe ser PRIVADO (isPublic=false)");

        verify(messagingTemplate).convertAndSendToUser(
                eq("TrainerRed"),
                eq("/queue/lobby-created"),
                any(GameLobbyDTO.class)
        );
    }

    @Test
    @DisplayName("Procesar Equipo: Delega validación a TeamService y notifica PLAYER_READY")
    void testProcessTeamSelection() {
        UUID lobbyId = UUID.randomUUID();
        GameLobby lobby = GameLobby.builder()
                .id(lobbyId)
                .players(new ArrayList<>(List.of(mockPlayer)))
                .build();

        TeamSetupDTO teamDto = new TeamSetupDTO(List.of("Mewtwo"));

        when(playerService.getCurrentAuthenticatedUser()).thenReturn(mockUser);
        when(gameLobbyRepository.findById(lobbyId)).thenReturn(Optional.of(lobby));

        gameLobbyService.processTeamSelection(lobbyId, teamDto);

        verify(teamService).createPlayerTeam(teamDto);

        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);

        verify(messagingTemplate).convertAndSend(destCaptor.capture(), payloadCaptor.capture());

        assertEquals("/topic/game/" + lobbyId.toString(), destCaptor.getValue());
        assertEquals("PLAYER_READY", payloadCaptor.getValue().get("type"));
    }
}

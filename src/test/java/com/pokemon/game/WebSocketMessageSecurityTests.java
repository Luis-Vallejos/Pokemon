package com.pokemon.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.game.PokemonApplication;
import com.pokemon.game.request.JoinLobbyRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.messaging.MessageDeliveryException; // Importación necesaria

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Luis
 */
@SpringBootTest(classes = PokemonApplication.class)
public class WebSocketMessageSecurityTests {

    @Autowired
    private MessageChannel clientInboundChannel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Message<byte[]> createStompMessage(StompCommand command, String destination, Authentication principal, Object payload) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);

        if (principal != null) {
            accessor.setUser(principal);
        }

        try {
            byte[] messagePayload = payload != null ? objectMapper.writeValueAsBytes(payload) : new byte[0];
            return MessageBuilder.withPayload(messagePayload)
                    .setHeaders(accessor)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error al crear mensaje STOMP", e);
        }
    }

    @Test
    @WithMockUser(username = "AshKetchum", roles = {"USER"})
    void authenticatedUserCanSendToAppDestination() {
        System.out.println("TEST: Enviando mensaje a /app/** como usuario autenticado...");

        Authentication principal = SecurityContextHolder.getContext().getAuthentication();

        Message<byte[]> message = createStompMessage(
                StompCommand.SEND,
                "/app/lobby.create",
                principal,
                null
        );

        assertDoesNotThrow(() -> clientInboundChannel.send(message),
                "Un usuario autenticado debe poder enviar mensajes a /app/**");

        System.out.println("RESULTADO: ✅ Éxito. El usuario autenticado pudo enviar el mensaje a /app/**.");
    }

    @Test
    void unauthenticatedUserCannotSendToAppDestination() {
        System.out.println("TEST: Enviando mensaje a /app/** como usuario NO autenticado...");

        JoinLobbyRequest mockRequest = new JoinLobbyRequest(UUID.randomUUID());

        Message<byte[]> message = createStompMessage(
                StompCommand.SEND,
                "/app/lobby.join",
                null,
                mockRequest
        );

        MessageDeliveryException exception = assertThrows(MessageDeliveryException.class,
                () -> clientInboundChannel.send(message),
                "Se esperaba MessageDeliveryException al enviar a /app/** sin autenticar.");

        assertTrue(exception.getCause() instanceof AccessDeniedException,
                "La causa de la MessageDeliveryException debe ser AccessDeniedException.");

        System.out.println("RESULTADO: ✅ Éxito. El mensaje fue rechazado (AccessDeniedException) para el usuario no autenticado.");
    }

    @Test
    @WithMockUser(username = "Misty", roles = {"USER"})
    void authenticatedUserCanSubscribeToTopicDestination() {
        System.out.println("TEST: Suscribiéndose a /topic/** como usuario autenticado...");

        Authentication principal = SecurityContextHolder.getContext().getAuthentication();

        Message<byte[]> message = createStompMessage(
                StompCommand.SUBSCRIBE,
                "/topic/lobby",
                principal,
                null
        );

        assertDoesNotThrow(() -> clientInboundChannel.send(message),
                "Un usuario autenticado debe poder suscribirse a /topic/**");

        System.out.println("RESULTADO: ✅ Éxito. El usuario autenticado pudo suscribirse a /topic/**.");
    }

    @Test
    void unauthenticatedUserCannotSubscribeToUserQueueDestination() {
        System.out.println("TEST: Suscribiéndose a /user/queue/** como usuario NO autenticado...");

        Message<byte[]> message = createStompMessage(
                StompCommand.SUBSCRIBE,
                "/user/queue/join-response",
                null,
                null
        );

        MessageDeliveryException exception = assertThrows(MessageDeliveryException.class,
                () -> clientInboundChannel.send(message),
                "Se esperaba MessageDeliveryException al suscribirse a /user/queue/** sin autenticar.");

        assertTrue(exception.getCause() instanceof AccessDeniedException,
                "La causa de la MessageDeliveryException debe ser AccessDeniedException.");

        System.out.println("RESULTADO: ✅ Éxito. La suscripción a la cola privada fue rechazada (AccessDeniedException).");
    }
}

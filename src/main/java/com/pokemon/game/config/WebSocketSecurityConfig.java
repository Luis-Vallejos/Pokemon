package com.pokemon.game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.messaging.Message;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

/**
 *
 * * @author Luis
 */
@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        return messages
                .simpDestMatchers("/app/**").authenticated()
                .simpSubscribeDestMatchers("/topic/**").authenticated()
                .simpSubscribeDestMatchers("/user/queue/**").authenticated()
                .anyMessage().permitAll()
                .build();
    }
}

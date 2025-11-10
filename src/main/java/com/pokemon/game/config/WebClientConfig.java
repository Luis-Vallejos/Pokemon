package com.pokemon.game.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${pokeapi.base-uri}")
    private String pokeApiBaseUri;

    @Bean
    public WebClient pokeApiWebClient(WebClient.Builder builder) {
        return builder.baseUrl(pokeApiBaseUri).build();
    }
}

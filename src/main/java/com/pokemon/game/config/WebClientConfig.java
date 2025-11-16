package com.pokemon.game.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 *
 * @author Luis
 */
@Configuration
public class WebClientConfig {

    @Value("${pokeapi.base-uri}")
    private String pokeApiBaseUri;

    // Logger para este WebClient
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    public WebClient pokeApiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(pokeApiBaseUri)
                .filter(logRequest())
                .filter(logResponse()) // Usamos la versión de DEBUG TOTAL
                .build();
    }

    // Este método registra la solicitud (qué estamos enviando)
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info(">>> API REQUEST: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info(">>> Header: {}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }

    // --- MODO DEBUG TOTAL ---
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("<<< API RESPONSE: Status {}", clientResponse.statusCode());

            // Clonamos la respuesta para leer el body
            return clientResponse.bodyToMono(String.class)
                    // Usamos .flatMap() para asegurar que el body se procese
                    .flatMap(body -> {
                        // ¡ESTE ES EL LOG QUE NECESITAMOS VER!
                        log.info("<<< API BODY: {}", body);

                        // Devolvemos la respuesta original con el body "recargado"
                        return Mono.just(clientResponse.mutate().body(body).build());
                    })
                    // Agregamos un .onErrorResume por si el body está vacío
                    .onErrorResume(e -> {
                        log.error("<<< No se pudo leer el body de la respuesta: {}", e.getMessage());
                        return Mono.just(clientResponse); // Devuelve la respuesta original sin body
                    });
        });
    }
}

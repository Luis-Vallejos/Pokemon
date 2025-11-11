package com.pokemon.game.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
                // AÑADIMOS ESTOS FILTROS DE LOGGING
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    // Este método registra la solicitud (qué estamos enviando)
    // CORREGIDO: Eliminada la parte del "body" que causaba el error
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info(">>> API REQUEST: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info(">>> Header: {}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }

    // Este método registra la respuesta (qué nos está devolviendo)
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("<<< API RESPONSE: Status {}", clientResponse.statusCode());
            // Para ver el cuerpo del error, necesitamos clonarlo
            return clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.info("<<< API BODY: {}", body); // ¡AQUÍ ESTARÁ EL ERROR!
                        // Devolvemos la respuesta original para que la aplicación continúe
                        return Mono.just(clientResponse.mutate().body(body).build());
                    });
        });
    }
}

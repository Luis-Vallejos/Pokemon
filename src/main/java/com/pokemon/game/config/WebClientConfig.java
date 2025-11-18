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

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    public WebClient pokeApiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(pokeApiBaseUri)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info(">>> API REQUEST: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info(">>> Header: {}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("<<< API RESPONSE: Status {}", clientResponse.statusCode());

            return clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.info("<<< API BODY: {}", body);

                        return Mono.just(clientResponse.mutate().body(body).build());
                    })
                    .onErrorResume(e -> {
                        log.error("<<< No se pudo leer el body de la respuesta: {}", e.getMessage());
                        return Mono.just(clientResponse);
                    });
        });
    }
}

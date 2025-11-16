package com.pokemon.game.config;

import com.pokemon.game.repository.StaticPokemonDataRepository;
import com.pokemon.game.service.PokeApiIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 *
 * @author Luis
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataIngestionRunner implements CommandLineRunner {

    private final PokeApiIngestionService ingestionService;
    private final StaticPokemonDataRepository pokemonRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Verificando la base de datos estática...");

        if (pokemonRepository.count() > 0) {
            log.info("La base de datos estática ya está poblada. Saltando ingesta.");
            return;
        }

        log.warn("Base de datos estática vacía. Iniciando ingesta de datos de PokeAPI...");
        long startTime = System.currentTimeMillis();

        try {
            ingestionService.ingestTypes()
                    .then(ingestionService.ingestAbilities())
                    .then(ingestionService.ingestMoves())
                    .then(ingestionService.ingestPokemon())
                    .then(ingestionService.ingestTypeDamageRelations())
                    .doOnSuccess(v -> {
                        long endTime = System.currentTimeMillis();
                        log.info("¡Ingesta de datos completada exitosamente en {} ms!", (endTime - startTime));
                    })
                    .doOnError(e -> {
                        log.error("¡FALLÓ la ingesta de datos (dentro del flujo reactivo)!", e);
                    })
                    .block();
        } catch (Exception e) {
            log.error("¡FALLÓ la ingesta de datos con una excepción bloqueante!", e);
        }
    }
}

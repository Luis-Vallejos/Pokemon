package com.pokemon.game.config;

import com.pokemon.game.repository.StaticAbilityDataRepository;
import com.pokemon.game.repository.StaticMoveDataRepository;
import com.pokemon.game.repository.StaticPokemonDataRepository;
import com.pokemon.game.repository.StaticTypeDataRepository;
import com.pokemon.game.service.PokeApiIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 *
 * @author Luis
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataIngestionRunner implements CommandLineRunner {

    private final PokeApiIngestionService ingestionService;

    // Inyectar TODOS los repositorios estáticos
    private final StaticPokemonDataRepository pokemonRepository;
    private final StaticTypeDataRepository typeRepository;
    private final StaticAbilityDataRepository abilityRepository;
    private final StaticMoveDataRepository moveRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando verificación e ingesta de datos estáticos...");
        long startTime = System.currentTimeMillis();

        // Cadena de ingesta reactiva
        Mono<Void> ingestionChain = Mono.empty();

        // Tarea 1: Tipos
        if (typeRepository.count() == 0) {
            log.warn("Base de datos de Tipos vacía. Iniciando ingesta de Tipos...");
            ingestionChain = ingestionChain.then(ingestionService.ingestTypes());
        } else {
            log.info("Tipos ya existen. Cargando caché de Tipos desde la BD.");
            // Si ya existen, necesitamos cargar el caché para los siguientes pasos
            ingestionChain = ingestionChain.then(ingestionService.loadTypeCache());
        }

        // Tarea 2: Habilidades
        if (abilityRepository.count() == 0) {
            log.warn("Base de datos de Habilidades vacía. Iniciando ingesta de Habilidades...");
            ingestionChain = ingestionChain.then(ingestionService.ingestAbilities());
        } else {
            log.info("Habilidades ya existen. Cargando caché de Habilidades desde la BD.");
            ingestionChain = ingestionChain.then(ingestionService.loadAbilityCache());
        }

        // Tarea 3: Movimientos (Depende de Tipos)
        if (moveRepository.count() == 0) {
            log.warn("Base de datos de Movimientos vacía. Iniciando ingesta de Movimientos...");
            ingestionChain = ingestionChain.then(ingestionService.ingestMoves());
        } else {
            log.info("Movimientos ya existen. Cargando caché de Movimientos desde la BD.");
            ingestionChain = ingestionChain.then(ingestionService.loadMoveCache());
        }

        // Tarea 4: Pokémon (Depende de Tipos, Habilidades, Movimientos)
        if (pokemonRepository.count() == 0) {
            log.warn("Base de datos de Pokémon vacía. Iniciando ingesta de Pokémon...");
            ingestionChain = ingestionChain.then(ingestionService.ingestPokemon());
        } else {
            log.info("Pokémon ya existe. Saltando ingesta de Pokémon.");
        }

        // Tarea 5: Relaciones de Tipos (Depende de Tipos)
        // Esta tarea actualiza los tipos existentes, por lo que la ejecutamos
        // si los tipos existen (ya sea de la ingesta o de la carga de caché).
        if (typeRepository.count() > 0) {
            log.info("Actualizando relaciones de daño de Tipos...");
            ingestionChain = ingestionChain.then(ingestionService.ingestTypeDamageRelations());
        } else {
            log.warn("Saltando relaciones de daño de Tipos (los tipos no están cargados).");
        }

        // Ejecutar la cadena
        try {
            ingestionChain
                    .doOnSuccess(v -> {
                        long endTime = System.currentTimeMillis();
                        log.info("¡Ingesta/Verificación de datos completada exitosamente en {} ms!", (endTime - startTime));
                    })
                    .doOnError(e -> {
                        log.error("¡FALLÓ la ingesta de datos (dentro del flujo reactivo)!", e);
                    })
                    .block();
        } catch (Exception e) {
            log.error("¡FALLÓ la ingesta de datos con una excepción bloqueante!", e);
            log.error("Excepción: ", e);
        }
    }
}

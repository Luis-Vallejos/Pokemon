package com.pokemon.game.service;

import com.pokemon.game.model.StaticAbilityData;
import com.pokemon.game.model.StaticMoveData;
import com.pokemon.game.model.StaticPokemonData;
import com.pokemon.game.model.StaticTypeData;
import com.pokemon.game.repository.StaticAbilityDataRepository;
import com.pokemon.game.repository.StaticMoveDataRepository;
import com.pokemon.game.repository.StaticPokemonDataRepository;
import com.pokemon.game.repository.StaticTypeDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PokeApiIngestionService {

    private final WebClient pokeApiWebClient;

    private final StaticPokemonDataRepository pokemonRepository;
    private final StaticMoveDataRepository moveRepository;
    private final StaticTypeDataRepository typeRepository;
    private final StaticAbilityDataRepository abilityRepository;

    // Cachés en memoria para construir las relaciones de la base de datos
    private final Map<String, StaticTypeData> typeCache = new ConcurrentHashMap<>();
    private final Map<String, StaticMoveData> moveCache = new ConcurrentHashMap<>();
    private final Map<String, StaticAbilityData> abilityCache = new ConcurrentHashMap<>();

    // --- Clases DTO Internas (Wrapper de consulta y DTOs de Respuesta) ---
    // (Estas clases modelan la respuesta JSON de la API v1beta2)
    private static class GqlQuery {

        public String query;

        public GqlQuery(String query) {
            this.query = query;
        }
    }

    // --- DTOs para Tipos (Types) ---
    // Nombres de campo coinciden con la API v1beta2 (sin prefijo)
    private static class GqlTypeResponse {

        public GqlTypeData data;
    }

    private static class GqlTypeData {

        public List<GqlType> type;
    } // SIN PREFIJO

    public static class GqlType {

        public String name;
        public List<GqlTypeDamage> typedamagerelations; // SIN PREFIJO
    }

    public static class GqlTypeDamage {

        public int damage_factor; // La API v1beta2 usa int (200, 50, 0)
        public GqlTypeName type; // SIN PREFIJO
    }

    public static class GqlTypeName {

        public String name;
    }

    // --- DTOs para Habilidades (Abilities) ---
    private static class GqlAbilityResponse {

        public GqlAbilityData data;
    }

    private static class GqlAbilityData {

        public List<GqlAbility> ability;
    } // SIN PREFIJO

    public static class GqlAbility {

        public String name;
        public List<GqlAbilityEffect> abilityeffecttexts; // SIN PREFIJO
    }

    public static class GqlAbilityEffect {

        public String effect;
    }

    // --- DTOs para Movimientos (Moves) ---
    private static class GqlMoveResponse {

        public GqlMoveData data;
    }

    private static class GqlMoveData {

        public List<GqlMove> move;
    } // SIN PREFIJO

    public static class GqlMove {

        public String name;
        public Integer power;
        public Integer accuracy;
        public Integer pp;
        public Integer priority;
        public GqlTypeName movedamageclass; // SIN PREFIJO
        public GqlTypeName type; // SIN PREFIJO
    }

    // --- DTOs para Pokémon ---
    private static class GqlPokemonResponse {

        public GqlPokemonData data;
    }

    private static class GqlPokemonData {

        public List<GqlPokemon> pokemon;
    } // SIN PREFIJO

    public static class GqlPokemon {

        public String name;
        public List<GqlPokemonStat> pokemonstats; // SIN PREFIJO
        public List<GqlPokemonType> pokemontypes; // SIN PREFIJO
        public List<GqlPokemonAbility> pokemonabilities; // SIN PREFIJO
        public List<GqlPokemonMove> pokemonmoves; // SIN PREFIJO
    }

    public static class GqlPokemonStat {

        public int base_stat;
        public GqlStatName stat; // SIN PREFIJO
    }

    public static class GqlStatName {

        public String name;
    }

    public static class GqlPokemonType {

        public GqlTypeName type;
    } // SIN PREFIJO

    public static class GqlPokemonAbility {

        public GqlTypeName ability;
    } // SIN PREFIJO

    public static class GqlPokemonMove {

        public GqlTypeName move;
    } // SIN PREFIJO

    /**
     * Ejecuta una consulta GraphQL genérica.
     */
    private <T> Mono<T> executeGqlQuery(String query, Class<T> responseClass) {
        return pokeApiWebClient.post()
                .bodyValue(new GqlQuery(query))
                .retrieve()
                .bodyToMono(responseClass)
                .doOnError(e -> log.error("Error al ejecutar consulta GraphQL: {}", e.getMessage()));
    }

    /**
     * Tarea 1: Descarga y guarda los 18 tipos de Pokémon.
     */
    @Transactional
    public Mono<Void> ingestTypes() {
        log.info("Iniciando ingesta de Tipos...");
        // Consulta GraphQL actualizada para v1beta2 (sin prefijos)
        String query = """
            query {
              type {
                name
              }
            }
        """;
        return executeGqlQuery(query, GqlTypeResponse.class)
                .flatMapMany(response -> {
                    // CORRECCIÓN: Comprobar si la respuesta o los datos son nulos
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Tipos) nula o inválida.");
                        return Flux.empty();
                    }
                    // CORRECCIÓN: Comprobar si la lista de tipos es nula
                    List<GqlType> types = response.data.type;
                    if (types == null) {
                        log.warn("La API de Tipos devolvió datos, pero la lista de tipos era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(types); // Actualizado
                })
                .map(gqlType -> StaticTypeData.builder().name(gqlType.name).build())
                .collectList()
                .doOnNext(typeRepository::saveAll)
                .doOnNext(savedTypes -> {
                    log.info("Guardados {} tipos en la BD.", savedTypes.size());
                    savedTypes.forEach(type -> typeCache.put(type.getName(), type));
                })
                .then();
    }

    /**
     * Tarea 2: Descarga y guarda todas las habilidades.
     */
    @Transactional
    public Mono<Void> ingestAbilities() {
        log.info("Iniciando ingesta de Habilidades...");
        // Consulta GraphQL actualizada para v1beta2 (sin prefijos)
        // language_id 9 es Inglés
        String query = """
            query {
              ability(limit: 370) {
                name
                abilityeffecttexts(where: {language_id: {_eq: 9}}) {
                  effect
                }
              }
            }
        """;
        return executeGqlQuery(query, GqlAbilityResponse.class)
                .flatMapMany(response -> {
                    // CORRECCIÓN: Comprobar si la respuesta o los datos son nulos
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Habilidades) nula o inválida.");
                        return Flux.empty();
                    }
                    // CORRECCIÓN: Comprobar si la lista de habilidades es nula
                    List<GqlAbility> abilities = response.data.ability;
                    if (abilities == null) {
                        log.warn("La API de Habilidades devolvió datos, pero la lista de habilidades era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(abilities); // Actualizado
                })
                .map(gqlAbility -> {
                    // CORRECCIÓN: Comprobar si la lista de efectos es nula o está vacía
                    String effect = (gqlAbility.abilityeffecttexts == null || gqlAbility.abilityeffecttexts.isEmpty())
                            ? "No effect text." : gqlAbility.abilityeffecttexts.get(0).effect;

                    return StaticAbilityData.builder()
                            .name(gqlAbility.name)
                            .description(effect)
                            .build();
                })
                .collectList()
                .doOnNext(abilityRepository::saveAll)
                .doOnNext(savedAbilities -> {
                    log.info("Guardadas {} habilidades en la BD.", savedAbilities.size());
                    savedAbilities.forEach(ability -> abilityCache.put(ability.getName(), ability));
                })
                .then();
    }

    /**
     * Tarea 3: Descarga y guarda todos los movimientos.
     */
    @Transactional
    public Mono<Void> ingestMoves() {
        log.info("Iniciando ingesta de Movimientos...");
        // Consulta GraphQL actualizada para v1beta2 (sin prefijos)
        String query = """
            query {
              move(limit: 950) {
                name
                power
                accuracy
                pp
                priority
                movedamageclass { name }
                type { name }
              }
            }
        """;
        return executeGqlQuery(query, GqlMoveResponse.class)
                .flatMapMany(response -> {
                    // CORRECCIÓN: Comprobar si la respuesta o los datos son nulos
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Movimientos) nula o inválida.");
                        return Flux.empty();
                    }
                    // CORRECCIÓN: Comprobar si la lista de movimientos es nula
                    List<GqlMove> moves = response.data.move;
                    if (moves == null) {
                        log.warn("La API de Movimientos devolvió datos, pero la lista de movimientos era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(moves); // Actualizado
                })
                .map(gqlMove -> StaticMoveData.builder()
                .name(gqlMove.name)
                .power(Objects.requireNonNullElse(gqlMove.power, 0))
                .accuracy(Objects.requireNonNullElse(gqlMove.accuracy, 0))
                .pp(Objects.requireNonNullElse(gqlMove.pp, 0))
                .priority(Objects.requireNonNullElse(gqlMove.priority, 0))
                // CORRECCIÓN: Comprobar si los objetos anidados son nulos
                .damageClass(gqlMove.movedamageclass != null ? gqlMove.movedamageclass.name : "unknown")
                .type(typeCache.get(gqlMove.type != null ? gqlMove.type.name : null))
                .build())
                .collectList()
                .doOnNext(moveRepository::saveAll)
                .doOnNext(savedMoves -> {
                    log.info("Guardados {} movimientos en la BD.", savedMoves.size());
                    savedMoves.forEach(move -> moveCache.put(move.getName(), move));
                })
                .then();
    }

    /**
     * Tarea 4: Descarga y guarda los primeros 151 Pokémon (Gen 1).
     */
    @Transactional
    public Mono<Void> ingestPokemon() {
        log.info("Iniciando ingesta de Pokémon (1-151)...");
        // Consulta GraphQL actualizada para v1beta2 (sin prefijos)
        String query = """
            query {
              pokemon(limit: 151, order_by: {id: asc}) {
                name
                pokemonstats {
                  base_stat
                  stat { name }
                }
                pokemontypes {
                  type { name }
                }
                pokemonabilities {
                  ability { name }
                }
                pokemonmoves(limit: 50) {
                  move { name }
                }
              }
            }
        """;
        return executeGqlQuery(query, GqlPokemonResponse.class)
                .flatMapMany(response -> {
                    // CORRECCIÓN: Comprobar si la respuesta o los datos son nulos
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Pokemon) nula o inválida.");
                        return Flux.empty();
                    }
                    // CORRECCIÓN: Comprobar si la lista de pokémon es nula
                    List<GqlPokemon> pokemonList = response.data.pokemon;
                    if (pokemonList == null) {
                        log.warn("La API de Pokémon devolvió datos, pero la lista de pokémon era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(pokemonList); // Actualizado
                })
                .map(gqlPokemon -> {
                    // CORRECCIÓN: Comprobar si las listas internas son nulas antes de hacer stream()
                    Map<String, Integer> stats = (gqlPokemon.pokemonstats == null) ? Collections.emptyMap()
                            : gqlPokemon.pokemonstats.stream()
                                    .filter(s -> s.stat != null) // Asegura que el stat interno no sea nulo
                                    .collect(Collectors.toMap(s -> s.stat.name, s -> s.base_stat));

                    Set<StaticTypeData> types = (gqlPokemon.pokemontypes == null) ? Collections.emptySet()
                            : gqlPokemon.pokemontypes.stream()
                                    .filter(t -> t.type != null) // Asegura que el tipo interno no sea nulo
                                    .map(t -> typeCache.get(t.type.name))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet());

                    Set<StaticAbilityData> abilities = (gqlPokemon.pokemonabilities == null) ? Collections.emptySet()
                            : gqlPokemon.pokemonabilities.stream()
                                    .filter(a -> a.ability != null) // Asegura que la habilidad interna no sea nula
                                    .map(a -> abilityCache.get(a.ability.name))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet());

                    Set<StaticMoveData> moves = (gqlPokemon.pokemonmoves == null) ? Collections.emptySet()
                            : gqlPokemon.pokemonmoves.stream()
                                    .filter(m -> m.move != null) // Asegura que el movimiento interno no sea nulo
                                    .map(m -> moveCache.get(m.move.name))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet());

                    // Construye la entidad JPA
                    return StaticPokemonData.builder()
                            .name(gqlPokemon.name)
                            .baseHp(stats.getOrDefault("hp", 0))
                            .baseAttack(stats.getOrDefault("attack", 0))
                            .baseDefense(stats.getOrDefault("defense", 0))
                            .baseSpecialAttack(stats.getOrDefault("special-attack", 0))
                            .baseSpecialDefense(stats.getOrDefault("special-defense", 0))
                            .baseSpeed(stats.getOrDefault("speed", 0))
                            .types(types)
                            .abilities(abilities)
                            .moves(moves)
                            .build();
                })
                .collectList()
                .doOnNext(pokemonRepository::saveAll)
                .doOnNext(savedPokemon -> log.info("Guardados {} Pokémon en la BD.", savedPokemon.size()))
                .then();
    }

    /**
     * Tarea 5: Actualiza las relaciones de daño de los tipos.
     */
    @Transactional
    public Mono<Void> ingestTypeDamageRelations() {
        log.info("Iniciando ingesta de Relaciones de Daño de Tipos...");
        // Consulta GraphQL actualizada para v1beta2 (sin prefijos)
        String query = """
            query {
              type {
                name
                typedamagerelations {
                  damage_factor
                  type { name }
                }
              }
            }
        """;
        return executeGqlQuery(query, GqlTypeResponse.class)
                .flatMapMany(response -> {
                    // CORRECCIÓN: Comprobar si la respuesta o los datos son nulos
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Relaciones de Tipos) nula o inválida.");
                        return Flux.empty();
                    }
                    // CORRECCIÓN: Comprobar si la lista de tipos es nula
                    List<GqlType> types = response.data.type;
                    if (types == null) {
                        log.warn("La API de Relaciones de Tipos devolvió datos, pero la lista de tipos era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(types); // Actualizado
                })
                .map(gqlType -> {
                    StaticTypeData type = typeCache.get(gqlType.name);
                    if (type == null) {
                        return null;
                    }

                    // CORRECCIÓN: Comprobar si la lista de relaciones es nula
                    if (gqlType.typedamagerelations == null) {
                        return type; // No hay relaciones para este tipo
                    }

                    for (GqlTypeDamage relation : gqlType.typedamagerelations) { // Actualizado
                        // CORRECCIÓN: Comprobar si la relación interna o el tipo son nulos
                        if (relation == null || relation.type == null) {
                            continue;
                        }
                        StaticTypeData otherType = typeCache.get(relation.type.name);
                        if (otherType == null) {
                            continue;
                        }

                        // La API v1beta2 devuelve 'damage_factor' como int: 200, 50, 0
                        if (relation.damage_factor == 200) { // Double damage FROM
                            type.getDoubleDamageFrom().add(otherType);
                        } else if (relation.damage_factor == 50) { // Half damage FROM
                            type.getHalfDamageFrom().add(otherType);
                        } else if (relation.damage_factor == 0) { // No damage FROM
                            type.getNoDamageFrom().add(otherType);
                        }
                    }
                    return type;
                })
                .filter(Objects::nonNull)
                .collectList()
                .doOnNext(typeRepository::saveAll)
                .doOnNext(savedTypes -> log.info("Actualizadas las relaciones de daño para {} tipos.", savedTypes.size()))
                .then();
    }
}

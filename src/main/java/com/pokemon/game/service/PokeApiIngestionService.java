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
import org.springframework.http.MediaType; // Importado
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

    private final Map<String, StaticTypeData> typeCache = new ConcurrentHashMap<>();
    private final Map<String, StaticMoveData> moveCache = new ConcurrentHashMap<>();
    private final Map<String, StaticAbilityData> abilityCache = new ConcurrentHashMap<>();

    // --- DTOs Internos ---
    // CORRECCIÓN: Usar el payload completo que espera el servidor GraphQL
    private static class GqlPayload {

        public String query;
        public Object variables = null;
        public Object operationName = null;

        public GqlPayload(String query) {
            this.query = query;
        }
    }

    // --- DTOs para Tipos (Types) ---
    private static class GqlTypeDefinitionResponse {

        public GqlTypeDefinitionData data;
    }

    private static class GqlTypeDefinitionData {

        public List<GqlTypeDefinition> pokemontype;
    }

    public static class GqlTypeDefinition {

        public String name;
        public List<GqlTypeDamage> type_efficacies;
    }

    public static class GqlTypeDamage {

        public int damage_factor;
        public GqlTypeName target_type;
    }

    public static class GqlTypeName {

        public String name;
    }

    // --- DTOs para Habilidades (Abilities) ---
    private static class GqlAbilityDefinitionResponse {

        public GqlAbilityDefinitionData data;
    }

    private static class GqlAbilityDefinitionData {

        public List<GqlAbilityDefinition> pokemonability;
    }

    public static class GqlAbilityDefinition {

        public String name;
        public List<GqlAbilityEffect> effect_entries;
    }

    public static class GqlAbilityEffect {

        public String effect;
    }

    // --- DTOs para Movimientos (Moves) ---
    private static class GqlMoveDefinitionResponse {

        public GqlMoveDefinitionData data;
    }

    private static class GqlMoveDefinitionData {

        public List<GqlMoveDefinition> pokemonmove;
    }

    public static class GqlMoveDefinition {

        public String name;
        public Integer power;
        public Integer accuracy;
        public Integer pp;
        public Integer priority;
        public GqlTypeName move_damage_class;
        public GqlTypeName type;
    }

    // --- DTOs para Pokémon ---
    private static class GqlPokemonspeciesResponse {

        public GqlPokemonspeciesData data;
    }

    private static class GqlPokemonspeciesData {

        public List<GqlPokemonspecies> pokemonspecies;
    }

    public static class GqlPokemonspecies {

        public String name;
        public List<GqlPokemonInstance> pokemons;
    }

    // --- DTOs para Instancias de Pokémon (Links) ---
    public static class GqlPokemonInstance {

        public List<GqlPokemonStat> stats;
        public List<GqlPokemonTypeLink> types;
        public List<GqlAbilityLink> abilities;
        public List<GqlMoveLink> moves;
    }

    public static class GqlPokemonStat {

        public int base_stat;
        public GqlStatName stat;
    }

    public static class GqlStatName {

        public String name;
    }

    public static class GqlPokemonTypeLink {

        public GqlTypeName type;
    }

    public static class GqlAbilityLink {

        public GqlTypeName ability;
    }

    public static class GqlMoveLink {

        public GqlTypeName move;
    }

    /**
     * Ejecuta una consulta GraphQL genérica.
     */
    private <T> Mono<T> executeGqlQuery(String query, Class<T> responseClass) {
        // CORRECCIÓN: Usar el GqlPayload completo
        return pokeApiWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GqlPayload(query)) // <-- CORREGIDO
                .retrieve()
                .bodyToMono(responseClass)
                .doOnError(e -> log.error("Error al ejecutar consulta GraphQL: {}", e.getMessage()));
    }

    /**
     * Tarea 1: Descarga y guarda los 18 tipos de Pokémon.
     */
    @Transactional
    public Mono<Void> ingestTypes() {
        log.info("Iniciando ingesta de Tipos (v1beta2)...");
        String query = """
            query {
              pokemontype {
                name
              }
            }
        """;
        return executeGqlQuery(query, GqlTypeDefinitionResponse.class)
                .flatMapMany(response -> {
                    // ESTA ES LA LÍNEA QUE ESTÁ FALLANDO
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Tipos) nula o inválida.");
                        return Flux.empty();
                    }
                    List<GqlTypeDefinition> types = response.data.pokemontype;
                    if (types == null) {
                        log.warn("La API de Tipos devolvió datos, pero la lista de tipos era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(types);
                })
                .map(gqlType -> StaticTypeData.builder().name(gqlType.name).build())
                .collectList()
                .doOnNext(typeRepository::saveAll)
                .doOnNext(savedTypes -> {
                    log.info("Guardados {} tipos en la BD.", savedTypes.size());
                    typeCache.clear();
                    savedTypes.forEach(type -> typeCache.put(type.getName(), type));
                })
                .then();
    }

    /**
     * Tarea 2: Descarga y guarda todas las habilidades.
     */
    @Transactional
    public Mono<Void> ingestAbilities() {
        log.info("Iniciando ingesta de Habilidades (v1beta2)...");
        String query = """
            query {
              pokemonability(limit: 370) {
                name
                effect_entries(where: {language_id: {_eq: 9}}) {
                  effect
                }
              }
            }
        """;
        return executeGqlQuery(query, GqlAbilityDefinitionResponse.class)
                .flatMapMany(response -> {
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Habilidades) nula o inválida.");
                        return Flux.empty();
                    }
                    List<GqlAbilityDefinition> abilities = response.data.pokemonability;
                    if (abilities == null) {
                        log.warn("La API de Habilidades devolvió datos, pero la lista de habilidades era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(abilities);
                })
                .map(gqlAbility -> {
                    String effect = (gqlAbility.effect_entries == null || gqlAbility.effect_entries.isEmpty())
                            ? "No effect text." : gqlAbility.effect_entries.get(0).effect;

                    return StaticAbilityData.builder()
                            .name(gqlAbility.name)
                            .description(effect)
                            .build();
                })
                .collectList()
                .doOnNext(abilityRepository::saveAll)
                .doOnNext(savedAbilities -> {
                    log.info("Guardadas {} habilidades en la BD.", savedAbilities.size());
                    abilityCache.clear();
                    savedAbilities.forEach(ability -> abilityCache.put(ability.getName(), ability));
                })
                .then();
    }

    /**
     * Tarea 3: Descarga y guarda todos los movimientos.
     */
    @Transactional
    public Mono<Void> ingestMoves() {
        log.info("Iniciando ingesta de Movimientos (v1beta2)...");
        String query = """
            query {
              pokemonmove(limit: 950) {
                name
                power
                accuracy
                pp
                priority
                move_damage_class { name }
                type { name }
              }
            }
        """;
        return executeGqlQuery(query, GqlMoveDefinitionResponse.class)
                .flatMapMany(response -> {
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Movimientos) nula o inválida.");
                        return Flux.empty();
                    }
                    List<GqlMoveDefinition> moves = response.data.pokemonmove;
                    if (moves == null) {
                        log.warn("La API de Movimientos devolvió datos, pero la lista de movimientos era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(moves);
                })
                .map(gqlMove -> {
                    StaticTypeData moveType = typeCache.get(gqlMove.type != null ? gqlMove.type.name : null);
                    if (moveType == null) {
                        log.warn("No se encontró el tipo '{}' en caché para el movimiento '{}'.", gqlMove.type.name, gqlMove.name);
                        return null;
                    }

                    return StaticMoveData.builder()
                            .name(gqlMove.name)
                            .power(Objects.requireNonNullElse(gqlMove.power, 0))
                            .accuracy(Objects.requireNonNullElse(gqlMove.accuracy, 0))
                            .pp(Objects.requireNonNullElse(gqlMove.pp, 0))
                            .priority(Objects.requireNonNullElse(gqlMove.priority, 0))
                            .damageClass(gqlMove.move_damage_class != null ? gqlMove.move_damage_class.name : "unknown")
                            .type(moveType)
                            .build();
                })
                .filter(Objects::nonNull)
                .collectList()
                .doOnNext(moveRepository::saveAll)
                .doOnNext(savedMoves -> {
                    log.info("Guardados {} movimientos en la BD.", savedMoves.size());
                    moveCache.clear();
                    savedMoves.forEach(move -> moveCache.put(move.getName(), move));
                })
                .then();
    }

    /**
     * Tarea 4: Descarga y guarda los primeros 151 Pokémon (Gen 1).
     */
    @Transactional
    public Mono<Void> ingestPokemon() {
        log.info("Iniciando ingesta de Pokémon (1-151) (v1beta2)...");
        String query = """
            query {
              pokemonspecies(limit: 151, order_by: {id: asc}) {
                name
                pokemons(limit: 1) {
                  stats {
                    base_stat
                    stat { name }
                  }
                  types {
                    type { name }
                  }
                  abilities {
                    ability { name }
                  }
                  moves(limit: 50) {
                    move { name }
                  }
                }
              }
            }
        """;
        return executeGqlQuery(query, GqlPokemonspeciesResponse.class)
                .flatMapMany(response -> {
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Pokemon) nula o inválida.");
                        return Flux.empty();
                    }
                    List<GqlPokemonspecies> pokemonList = response.data.pokemonspecies;
                    if (pokemonList == null) {
                        log.warn("La API de Pokémon devolvió datos, pero la lista de pokémon era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(pokemonList);
                })
                .map(gqlSpecies -> {
                    if (gqlSpecies.pokemons == null || gqlSpecies.pokemons.isEmpty()) {
                        log.warn("Species {} no tiene instancia 'pokemon', saltando.", gqlSpecies.name);
                        return null;
                    }

                    GqlPokemonInstance instance = gqlSpecies.pokemons.get(0);

                    Map<String, Integer> stats = (instance.stats == null) ? Collections.emptyMap()
                            : instance.stats.stream()
                                    .filter(s -> s.stat != null)
                                    .collect(Collectors.toMap(s -> s.stat.name, s -> s.base_stat));

                    Set<StaticTypeData> types = (instance.types == null) ? Collections.emptySet()
                            : instance.types.stream()
                                    .filter(t -> t.type != null && typeCache.containsKey(t.type.name))
                                    .map(t -> typeCache.get(t.type.name))
                                    .collect(Collectors.toSet());

                    Set<StaticAbilityData> abilities = (instance.abilities == null) ? Collections.emptySet()
                            : instance.abilities.stream()
                                    .filter(a -> a.ability != null && abilityCache.containsKey(a.ability.name))
                                    .map(a -> abilityCache.get(a.ability.name))
                                    .collect(Collectors.toSet());

                    Set<StaticMoveData> moves = (instance.moves == null) ? Collections.emptySet()
                            : instance.moves.stream()
                                    .filter(m -> m.move != null && moveCache.containsKey(m.move.name))
                                    .map(m -> moveCache.get(m.move.name))
                                    .collect(Collectors.toSet());

                    return StaticPokemonData.builder()
                            .name(gqlSpecies.name)
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
                .filter(Objects::nonNull)
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
        log.info("Iniciando ingesta de Relaciones de Daño de Tipos (v1beta2)...");

        String query = """
            query {
              pokemontype {
                name
                type_efficacies {
                  damage_factor
                  target_type { name }
                }
              }
            }
        """;

        return executeGqlQuery(query, GqlTypeDefinitionResponse.class)
                .map(response -> {
                    if (response == null || response.data == null || response.data.pokemontype == null) {
                        log.error("Respuesta de API (Relaciones de Tipos) nula o inválida.");
                        return Collections.<GqlTypeDefinition>emptyList();
                    }
                    return response.data.pokemontype;
                })
                .flatMap(allTypeDefs -> {
                    for (GqlTypeDefinition attackingTypeGql : allTypeDefs) {
                        StaticTypeData attackingType = typeCache.get(attackingTypeGql.name);
                        if (attackingType == null || attackingTypeGql.type_efficacies == null) {
                            continue;
                        }

                        for (GqlTypeDamage efficacy : attackingTypeGql.type_efficacies) {
                            if (efficacy == null || efficacy.target_type == null) {
                                continue;
                            }

                            StaticTypeData targetType = typeCache.get(efficacy.target_type.name);
                            if (targetType == null) {
                                continue;
                            }

                            if (efficacy.damage_factor == 200) {
                                targetType.getDoubleDamageFrom().add(attackingType);
                            } else if (efficacy.damage_factor == 50) {
                                targetType.getHalfDamageFrom().add(attackingType);
                            } else if (efficacy.damage_factor == 0) {
                                targetType.getNoDamageFrom().add(attackingType);
                            }
                        }
                    }

                    return Flux.fromIterable(typeCache.values())
                            .collectList()
                            .doOnNext(typeRepository::saveAll)
                            .doOnNext(savedTypes -> log.info("Actualizadas las relaciones de daño (invertidas) para {} tipos.", savedTypes.size()));
                })
                .then();
    }
}

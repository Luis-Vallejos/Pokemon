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
import org.springframework.http.MediaType;
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

    // Caches internos
    private final Map<String, StaticTypeData> typeCache = new ConcurrentHashMap<>();
    private final Map<String, StaticMoveData> moveCache = new ConcurrentHashMap<>();
    private final Map<String, StaticAbilityData> abilityCache = new ConcurrentHashMap<>();

    // --- DTOs Internos ---
    public static class GqlPayload {

        public String query;
        public Object variables = null;
        public Object operationName = null;

        public GqlPayload(String query) {
            this.query = query;
        }
    }

    // --- DTOs para Nombres (reutilizable) ---
    public static class GqlNameWrapper {

        public String name;
    }

    // --- DTOs para Tipos (Types) - (Actualizado a v2) ---
    public static class GqlTypeDefinitionResponse {

        public GqlTypeDefinitionData data;
    }

    public static class GqlTypeDefinitionData {

        public List<GqlTypeDefinition> pokemon_v2_type; // <-- CORREGIDO
    }

    public static class GqlTypeDefinition {

        public String name;
        public List<GqlTypeDamage> type_efficacies; // <-- (pokemon_v2_typeefficacy)
    }

    public static class GqlTypeDamage {

        public int damage_factor;
        public GqlNameWrapper damage_type; // <-- (pokemon_v2_type)
    }

    // --- DTOs para Habilidades (Abilities) - (Actualizado a v2) ---
    public static class GqlAbilityDefinitionResponse {

        public GqlAbilityDefinitionData data;
    }

    public static class GqlAbilityDefinitionData {

        public List<GqlAbilityDefinition> pokemon_v2_ability; // <-- CORREGIDO
    }

    public static class GqlAbilityDefinition {

        public String name;
        public List<GqlAbilityEffect> ability_effects; // <-- (pokemon_v2_abilityeffecttext)
    }

    public static class GqlAbilityEffect {

        public String effect;
    }

    // --- DTOs para Movimientos (Moves) - (Actualizado a v2) ---
    public static class GqlMoveDefinitionResponse {

        public GqlMoveDefinitionData data;
    }

    public static class GqlMoveDefinitionData {

        public List<GqlMoveDefinition> pokemon_v2_move; // <-- CORREGIDO
    }

    public static class GqlMoveDefinition {

        public String name;
        public Integer power;
        public Integer accuracy;
        public Integer pp;
        public Integer priority;
        public GqlNameWrapper move_damage_class; // <-- (pokemon_v2_movedamageclass)
        public GqlNameWrapper type; // <-- (pokemon_v2_type)
    }

    // --- DTOs para Pokémon - (Actualizado a v2) ---
    public static class GqlPokemonspeciesResponse {

        public GqlPokemonspeciesData data;
    }

    public static class GqlPokemonspeciesData {

        public List<GqlPokemonspecies> pokemon_v2_pokemonspecies; // <-- CORREGIDO
    }

    public static class GqlPokemonspecies {

        public String name;
        public List<GqlPokemonInstance> pokemons; // <-- (pokemon_v2_pokemon)
    }

    // --- DTOs para Instancias de Pokémon (Links) - (Actualizado a v2) ---
    public static class GqlPokemonInstance {

        public List<GqlPokemonStat> stats; // <-- (pokemon_v2_pokemonstat)
        public List<GqlPokemonTypeLink> types; // <-- (pokemon_v2_pokemontype)
        public List<GqlAbilityLink> abilities; // <-- (pokemon_v2_pokemonability)
        public List<GqlMoveLink> moves; // <-- (pokemon_v2_pokemonmove)
    }

    public static class GqlPokemonStat {

        public int base_stat;
        public GqlNameWrapper stat; // <-- (pokemon_v2_stat)
    }

    public static class GqlPokemonTypeLink {

        public GqlNameWrapper type; // <-- (pokemon_v2_type)
    }

    public static class GqlAbilityLink {

        public GqlNameWrapper ability; // <-- (pokemon_v2_ability)
    }

    public static class GqlMoveLink {

        public GqlNameWrapper move; // <-- (pokemon_v2_move)
    }

    // --- (INICIO) MÉTODOS PARA CARGAR CACHÉ ---
    @Transactional(readOnly = true)
    public Mono<Void> loadTypeCache() {
        return Mono.fromRunnable(() -> {
            if (typeCache.isEmpty()) {
                log.info("Cargando TypeCache desde la BD...");
                typeCache.clear();
                typeRepository.findAll().forEach(type -> typeCache.put(type.getName(), type));
                log.info("TypeCache cargado con {} entradas.", typeCache.size());
            } else {
                log.info("TypeCache ya estaba cargado.");
            }
        });
    }

    @Transactional(readOnly = true)
    public Mono<Void> loadAbilityCache() {
        return Mono.fromRunnable(() -> {
            if (abilityCache.isEmpty()) {
                log.info("Cargando AbilityCache desde la BD...");
                abilityCache.clear();
                abilityRepository.findAll().forEach(ability -> abilityCache.put(ability.getName(), ability));
                log.info("AbilityCache cargado con {} entradas.", abilityCache.size());
            } else {
                log.info("AbilityCache ya estaba cargado.");
            }
        });
    }

    @Transactional(readOnly = true)
    public Mono<Void> loadMoveCache() {
        return Mono.fromRunnable(() -> {
            if (moveCache.isEmpty()) {
                log.info("Cargando MoveCache desde la BD...");
                moveCache.clear();
                moveRepository.findAll().forEach(move -> moveCache.put(move.getName(), move));
                log.info("MoveCache cargado con {} entradas.", moveCache.size());
            } else {
                log.info("MoveCache ya estaba cargado.");
            }
        });
    }
    // --- (FIN) MÉTODOS PARA CARGAR CACHÉ ---

    private <T> Mono<T> executeGqlQuery(String query, Class<T> responseClass) {
        return pokeApiWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GqlPayload(query))
                .retrieve()
                .bodyToMono(responseClass)
                .doOnSuccess(response -> {
                    if (response != null) {
                        log.info("Deserialización exitosa para {}.", responseClass.getSimpleName());
                    } else {
                        log.warn("La deserialización resultó en un objeto nulo para {}", responseClass.getSimpleName());
                    }
                })
                .doOnError(e -> log.error("Error al ejecutar consulta GraphQL o deserializar: {}", e.getMessage()));
    }

    @Transactional
    public Mono<Void> ingestTypes() {
        log.info("Iniciando ingesta de Tipos (esquema v2)...");
        // CORREGIDO: Query para v2
        String query = """
            query {
              pokemon_v2_type(limit: 18) {
                name
              }
            }
        """;
        return executeGqlQuery(query, GqlTypeDefinitionResponse.class)
                .flatMapMany(response -> {
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Tipos) nula o inválida.");
                        return Flux.empty();
                    }
                    List<GqlTypeDefinition> types = response.data.pokemon_v2_type; // <-- CORREGIDO
                    if (types == null) {
                        log.warn("La API de Tipos devolvió datos, pero la lista de tipos era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(types);
                })
                .map(gqlType -> StaticTypeData.builder().name(gqlType.name).build())
                .filter(Objects::nonNull)
                .collectList()
                .doOnNext(typeRepository::saveAll)
                .doOnNext(savedTypes -> {
                    log.info("Guardados {} tipos en la BD.", savedTypes.size());
                    typeCache.clear();
                    savedTypes.forEach(type -> typeCache.put(type.getName(), type));
                })
                .then();
    }

    @Transactional
    public Mono<Void> ingestAbilities() {
        log.info("Iniciando ingesta de Habilidades (esquema v2)...");
        // CORREGIDO: Query para v2
        String query = """
            query {
              pokemon_v2_ability(limit: 370) {
                name
                ability_effects: pokemon_v2_abilityeffecttexts(where: {language_id: {_eq: 9}}) {
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
                    List<GqlAbilityDefinition> abilities = response.data.pokemon_v2_ability; // <-- CORREGIDO
                    if (abilities == null) {
                        log.warn("La API de Habilidades devolvió datos, pero la lista de habilidades era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(abilities);
                })
                .map(gqlAbility -> {
                    String effect = (gqlAbility.ability_effects == null || gqlAbility.ability_effects.isEmpty())
                            ? "No effect text." : gqlAbility.ability_effects.get(0).effect;

                    return StaticAbilityData.builder()
                            .name(gqlAbility.name)
                            .description(effect)
                            .build();
                })
                .filter(Objects::nonNull)
                .collectList()
                .doOnNext(abilityRepository::saveAll)
                .doOnNext(savedAbilities -> {
                    log.info("Guardadas {} habilidades en la BD.", savedAbilities.size());
                    abilityCache.clear();
                    savedAbilities.forEach(ability -> abilityCache.put(ability.getName(), ability));
                })
                .then();
    }

    @Transactional
    public Mono<Void> ingestMoves() {
        log.info("Iniciando ingesta de Movimientos (esquema v2)...");
        // CORREGIDO: Query para v2
        String query = """
            query {
              pokemon_v2_move(limit: 950) {
                name
                power
                accuracy
                pp
                priority
                move_damage_class: pokemon_v2_movedamageclass {
                  name
                }
                type: pokemon_v2_type {
                  name
                }
              }
            }
        """;
        return executeGqlQuery(query, GqlMoveDefinitionResponse.class)
                .flatMapMany(response -> {
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Movimientos) nula o inválida.");
                        return Flux.empty();
                    }
                    List<GqlMoveDefinition> moves = response.data.pokemon_v2_move; // <-- CORREGIDO
                    if (moves == null) {
                        log.warn("La API de Movimientos devolvió datos, pero la lista de movimientos era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(moves);
                })
                .map(gqlMove -> {
                    StaticTypeData moveType = typeCache.get(gqlMove.type != null ? gqlMove.type.name : null);

                    if (moveType == null) {
                        log.warn("No se encontró el tipo '{}' en caché para el movimiento '{}'.",
                                (gqlMove.type != null ? gqlMove.type.name : "null"),
                                gqlMove.name);
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

    @Transactional
    public Mono<Void> ingestPokemon() {
        log.info("Iniciando ingesta de Pokémon (1-151) (esquema v2)...");
        // CORREGIDO: Query para v2
        String query = """
            query {
              pokemon_v2_pokemonspecies(limit: 151, order_by: {id: asc}) {
                name
                pokemons: pokemon_v2_pokemons(limit: 1) {
                  stats: pokemon_v2_pokemonstats {
                    base_stat
                    stat: pokemon_v2_stat {
                      name
                    }
                  }
                  types: pokemon_v2_pokemontypes {
                    type: pokemon_v2_type {
                      name
                    }
                  }
                  abilities: pokemon_v2_pokemonabilities {
                    ability: pokemon_v2_ability {
                      name
                    }
                  }
                  moves: pokemon_v2_pokemonmoves(limit: 50) {
                    move: pokemon_v2_move {
                      name
                    }
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
                    List<GqlPokemonspecies> pokemonList = response.data.pokemon_v2_pokemonspecies; // <-- CORREGIDO
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

    @Transactional
    public Mono<Void> ingestTypeDamageRelations() {
        log.info("Iniciando ingesta de Relaciones de Daño de Tipos (esquema v2)...");
        // CORREGIDO: Query para v2
        String query = """
            query {
              pokemon_v2_type(limit: 18) {
                name
                type_efficacies: pokemon_v2_typeefficacies {
                  damage_factor
                  damage_type: pokemon_v2_type {
                    name
                  }
                }
              }
            }
        """;

        return executeGqlQuery(query, GqlTypeDefinitionResponse.class)
                .map(response -> {
                    if (response == null || response.data == null || response.data.pokemon_v2_type == null) {
                        log.error("Respuesta de API (Relaciones de Tipos) nula o inválida.");
                        return Collections.<GqlTypeDefinition>emptyList();
                    }
                    return response.data.pokemon_v2_type;
                })
                .flatMap(allTypeDefs -> {
                    for (GqlTypeDefinition attackingTypeGql : allTypeDefs) {
                        StaticTypeData attackingType = typeCache.get(attackingTypeGql.name);

                        if (attackingType == null || attackingTypeGql.type_efficacies == null) {
                            continue;
                        }

                        for (GqlTypeDamage efficacy : attackingTypeGql.type_efficacies) {
                            if (efficacy == null || efficacy.damage_type == null) {
                                continue;
                            }

                            StaticTypeData targetType = typeCache.get(efficacy.damage_type.name);
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

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

    // --- DTOs para Tipos (Types) ---
    // CORREGIDO: Nombres de campos para v1beta2 (anidado)
    public static class GqlTypeDefinitionResponse {

        public GqlTypeDefinitionData data;
    }

    public static class GqlTypeDefinitionData {

        public List<GqlTypeDefinition> pokemontype; // <--- Raíz v1beta2
    }

    public static class GqlTypeDefinition {

        public GqlNameWrapper pokemon_v2_type; // <--- Objeto anidado
        public List<GqlTypeDamage> pokemon_v2_typeefficacies;
    }

    public static class GqlTypeDamage {

        public int damage_factor;
        public GqlTypeDamageTarget pokemon_v2_typetarget;
    }

    // Nivel extra de anidamiento para la relación
    public static class GqlTypeDamageTarget {

        public GqlNameWrapper pokemon_v2_type;
    }

    // --- DTOs para Habilidades (Abilities) ---
    // CORREGIDO: Nombres de campos para v1beta2 (anidado)
    public static class GqlAbilityDefinitionResponse {

        public GqlAbilityDefinitionData data;
    }

    public static class GqlAbilityDefinitionData {

        public List<GqlAbilityDefinition> pokemonability; // <--- Raíz v1beta2
    }

    public static class GqlAbilityDefinition {

        public GqlNameWrapper pokemon_v2_ability; // <--- Objeto anidado
        public List<GqlAbilityEffect> pokemon_v2_abilityeffecttexts;
    }

    public static class GqlAbilityEffect {

        public String effect;
    }

    // --- DTOs para Movimientos (Moves) ---
    // CORREGIDO: Nombres de campos para v1beta2 (anidado)
    public static class GqlMoveDefinitionResponse {

        public GqlMoveDefinitionData data;
    }

    public static class GqlMoveDefinitionData {

        public List<GqlMoveDefinition> pokemonmove; // <--- Raíz v1beta2
    }

    public static class GqlMoveDefinition {

        public GqlMoveDetails pokemon_v2_move; // <--- Objeto anidado
        public GqlNameWrapper pokemon_v2_movedamageclass;
        public GqlNameWrapper pokemon_v2_type;
    }

    public static class GqlMoveDetails {

        public String name;
        public Integer power;
        public Integer accuracy;
        public Integer pp;
        public Integer priority;
    }

    // --- DTOs para Pokémon ---
    // CORREGIDO: Nombres de campos para v1beta2 (anidado)
    public static class GqlPokemonspeciesResponse {

        public GqlPokemonspeciesData data;
    }

    public static class GqlPokemonspeciesData {

        public List<GqlPokemonspecies> pokemonspecies; // <--- Raíz v1beta2
    }

    public static class GqlPokemonspecies {

        public String name; // 'name' está en la raíz de species, pero el resto no
        public List<GqlPokemonInstance> pokemon_v2_pokemons;
    }

    // --- DTOs para Instancias de Pokémon (Links) ---
    // CORREGIDO: Nombres de campos para v1beta2 (anidado)
    public static class GqlPokemonInstance {

        public List<GqlPokemonStat> pokemon_v2_pokemonstats;
        public List<GqlPokemonTypeLink> pokemon_v2_pokemontypes;
        public List<GqlAbilityLink> pokemon_v2_pokemonabilities;
        public List<GqlMoveLink> pokemon_v2_pokemonmoves;
    }

    public static class GqlPokemonStat {

        public int base_stat;
        public GqlStatNameWrapper pokemon_v2_stat;
    }

    public static class GqlStatNameWrapper {
        // public GqlNameWrapper pokemon_v2_stat; // v1beta2 es absurdamente anidado

        public String name; // El stat 'name' SÍ está aquí
    }

    public static class GqlPokemonTypeLink {

        public GqlNameWrapper pokemon_v2_type;
    }

    public static class GqlAbilityLink {

        public GqlNameWrapper pokemon_v2_ability;
    }

    public static class GqlMoveLink {

        public GqlNameWrapper pokemon_v2_move;
    }

    /**
     * Ejecuta una consulta GraphQL genérica.
     */
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

    /**
     * Tarea 1: Descarga y guarda los 18 tipos de Pokémon.
     */
    @Transactional
    public Mono<Void> ingestTypes() {
        log.info("Iniciando ingesta de Tipos (v1beta2)..."); // <-- LOG CORREGIDO
        // CORREGIDO: Query para v1beta2 (anidado)
        String query = """
            query {
              pokemontype {
                pokemon_v2_type {
                  name
                }
              }
            }
        """;
        return executeGqlQuery(query, GqlTypeDefinitionResponse.class)
                .flatMapMany(response -> {
                    if (response == null || response.data == null) {
                        log.error("Respuesta de API (Tipos) nula o inválida.");
                        return Flux.empty();
                    }
                    List<GqlTypeDefinition> types = response.data.pokemontype; // <--- Raíz v1beta2
                    if (types == null) {
                        log.warn("La API de Tipos devolvió datos, pero la lista de tipos era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(types);
                })
                // CORREGIDO: Acceder al nombre anidado
                .map(gqlType -> {
                    if (gqlType.pokemon_v2_type == null) {
                        log.warn("Se encontró un 'pokemontype' sin 'pokemon_v2_type' anidado.");
                        return null;
                    }
                    return StaticTypeData.builder().name(gqlType.pokemon_v2_type.name).build();
                })
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

    /**
     * Tarea 2: Descarga y guarda todas las habilidades.
     */
    @Transactional
    public Mono<Void> ingestAbilities() {
        log.info("Iniciando ingesta de Habilidades (v1beta2)..."); // <-- LOG CORREGIDO
        // CORREGIDO: Query para v1beta2 (anidado)
        String query = """
            query {
              pokemonability(limit: 370) {
                pokemon_v2_ability {
                  name
                }
                pokemon_v2_abilityeffecttexts(where: {language_id: {_eq: 9}}) {
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
                    List<GqlAbilityDefinition> abilities = response.data.pokemonability; // <--- Raíz v1beta2
                    if (abilities == null) {
                        log.warn("La API de Habilidades devolvió datos, pero la lista de habilidades era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(abilities);
                })
                .map(gqlAbility -> {
                    // CORREGIDO: Acceder a campos anidados
                    String effect = (gqlAbility.pokemon_v2_abilityeffecttexts == null || gqlAbility.pokemon_v2_abilityeffecttexts.isEmpty())
                            ? "No effect text." : gqlAbility.pokemon_v2_abilityeffecttexts.get(0).effect;

                    if (gqlAbility.pokemon_v2_ability == null) {
                        log.warn("Se encontró un 'pokemonability' sin 'pokemon_v2_ability' anidado.");
                        return null;
                    }
                    String name = gqlAbility.pokemon_v2_ability.name;

                    return StaticAbilityData.builder()
                            .name(name)
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

    /**
     * Tarea 3: Descarga y guarda todos los movimientos.
     */
    @Transactional
    public Mono<Void> ingestMoves() {
        log.info("Iniciando ingesta de Movimientos (v1beta2)..."); // <-- LOG CORREGIDO
        // CORREGIDO: Query para v1beta2 (anidado)
        String query = """
            query {
              pokemonmove(limit: 950) {
                pokemon_v2_move {
                  name
                  power
                  accuracy
                  pp
                  priority
                }
                pokemon_v2_movedamageclass {
                  name
                }
                pokemon_v2_type {
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
                    List<GqlMoveDefinition> moves = response.data.pokemonmove; // <--- Raíz v1beta2
                    if (moves == null) {
                        log.warn("La API de Movimientos devolvió datos, pero la lista de movimientos era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(moves);
                })
                .map(gqlMove -> {
                    // CORREGIDO: Acceder a campos anidados
                    StaticTypeData moveType = typeCache.get(gqlMove.pokemon_v2_type != null ? gqlMove.pokemon_v2_type.name : null);

                    if (gqlMove.pokemon_v2_move == null) {
                        log.warn("Se encontró un 'pokemonmove' sin 'pokemon_v2_move' anidado.");
                        return null;
                    }

                    if (moveType == null) {
                        log.warn("No se encontró el tipo '{}' en caché para el movimiento '{}'.",
                                (gqlMove.pokemon_v2_type != null ? gqlMove.pokemon_v2_type.name : "null"),
                                gqlMove.pokemon_v2_move.name);
                        return null;
                    }

                    GqlMoveDetails details = gqlMove.pokemon_v2_move;

                    return StaticMoveData.builder()
                            .name(details.name)
                            .power(Objects.requireNonNullElse(details.power, 0))
                            .accuracy(Objects.requireNonNullElse(details.accuracy, 0))
                            .pp(Objects.requireNonNullElse(details.pp, 0))
                            .priority(Objects.requireNonNullElse(details.priority, 0))
                            .damageClass(gqlMove.pokemon_v2_movedamageclass != null ? gqlMove.pokemon_v2_movedamageclass.name : "unknown")
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
        log.info("Iniciando ingesta de Pokémon (1-151) (v1beta2)..."); // <-- LOG CORREGIDO
        // CORREGIDO: Query para v1beta2 (anidado)
        String query = """
            query {
              pokemonspecies(limit: 151, order_by: {id: asc}) {
                name
                pokemon_v2_pokemons(limit: 1) {
                  pokemon_v2_pokemonstats {
                    base_stat
                    pokemon_v2_stat {
                      name
                    }
                  }
                  pokemon_v2_pokemontypes {
                    pokemon_v2_type {
                      name
                    }
                  }
                  pokemon_v2_pokemonabilities {
                    pokemon_v2_ability {
                      name
                    }
                  }
                  pokemon_v2_pokemonmoves(limit: 50) {
                    pokemon_v2_move {
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
                    List<GqlPokemonspecies> pokemonList = response.data.pokemonspecies; // <--- Raíz v1beta2
                    if (pokemonList == null) {
                        log.warn("La API de Pokémon devolvió datos, pero la lista de pokémon era nula.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(pokemonList);
                })
                .map(gqlSpecies -> {
                    if (gqlSpecies.pokemon_v2_pokemons == null || gqlSpecies.pokemon_v2_pokemons.isEmpty()) {
                        log.warn("Species {} no tiene instancia 'pokemon', saltando.", gqlSpecies.name);
                        return null;
                    }

                    GqlPokemonInstance instance = gqlSpecies.pokemon_v2_pokemons.get(0);

                    // CORREGIDO: Acceder a campos anidados
                    Map<String, Integer> stats = (instance.pokemon_v2_pokemonstats == null) ? Collections.emptyMap()
                            : instance.pokemon_v2_pokemonstats.stream()
                                    .filter(s -> s.pokemon_v2_stat != null)
                                    .collect(Collectors.toMap(s -> s.pokemon_v2_stat.name, s -> s.base_stat));

                    Set<StaticTypeData> types = (instance.pokemon_v2_pokemontypes == null) ? Collections.emptySet()
                            : instance.pokemon_v2_pokemontypes.stream()
                                    .filter(t -> t.pokemon_v2_type != null && typeCache.containsKey(t.pokemon_v2_type.name))
                                    .map(t -> typeCache.get(t.pokemon_v2_type.name))
                                    .collect(Collectors.toSet());

                    Set<StaticAbilityData> abilities = (instance.pokemon_v2_pokemonabilities == null) ? Collections.emptySet()
                            : instance.pokemon_v2_pokemonabilities.stream()
                                    .filter(a -> a.pokemon_v2_ability != null && abilityCache.containsKey(a.pokemon_v2_ability.name))
                                    .map(a -> abilityCache.get(a.pokemon_v2_ability.name))
                                    .collect(Collectors.toSet());

                    Set<StaticMoveData> moves = (instance.pokemon_v2_pokemonmoves == null) ? Collections.emptySet()
                            : instance.pokemon_v2_pokemonmoves.stream()
                                    .filter(m -> m.pokemon_v2_move != null && moveCache.containsKey(m.pokemon_v2_move.name))
                                    .map(m -> moveCache.get(m.pokemon_v2_move.name))
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
        log.info("Iniciando ingesta de Relaciones de Daño de Tipos (v1beta2)..."); // <-- LOG CORREGIDO
        // CORREGIDO: Query para v1beta2 (anidado)
        String query = """
            query {
              pokemontype {
                pokemon_v2_type {
                  name
                }
                pokemon_v2_typeefficacies {
                  damage_factor
                  pokemon_v2_typetarget {
                    pokemon_v2_type {
                      name
                    }
                  }
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
                        // CORREGIDO: Acceder a campos anidados
                        if (attackingTypeGql.pokemon_v2_type == null) {
                            continue; // Saltar si no hay tipo
                        }
                        StaticTypeData attackingType = typeCache.get(attackingTypeGql.pokemon_v2_type.name);

                        if (attackingType == null || attackingTypeGql.pokemon_v2_typeefficacies == null) {
                            continue;
                        }

                        for (GqlTypeDamage efficacy : attackingTypeGql.pokemon_v2_typeefficacies) {
                            if (efficacy == null || efficacy.pokemon_v2_typetarget == null || efficacy.pokemon_v2_typetarget.pokemon_v2_type == null) {
                                continue;
                            }

                            StaticTypeData targetType = typeCache.get(efficacy.pokemon_v2_typetarget.pokemon_v2_type.name);
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

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

    public static class GqlPayload {

        public String query;
        public Object variables = null;
        public Object operationName = null;

        public GqlPayload(String query) {
            this.query = query;
        }
    }

    public static class GqlNameWrapper {

        public String name;
    }

    public static class GqlTypeDefinitionResponse {

        public GqlTypeDefinitionData data;
    }

    public static class GqlTypeDefinitionData {

        public List<GqlTypeDefinition> type;
    }

    public static class GqlTypeDefinition {

        public String name;
        public List<GqlTypeDamage> typeefficacies;
    }

    public static class GqlTypeDamage {

        public int damage_factor;
        public GqlNameWrapper damage_type;
    }

    public static class GqlAbilityDefinitionResponse {

        public GqlAbilityDefinitionData data;
    }

    public static class GqlAbilityDefinitionData {

        public List<GqlAbilityDefinition> ability;
    }

    public static class GqlAbilityDefinition {

        public String name;
        public List<GqlAbilityEffect> ability_effects;
    }

    public static class GqlAbilityEffect {

        public String effect;
    }

    public static class GqlMoveDefinitionResponse {

        public GqlMoveDefinitionData data;
    }

    public static class GqlMoveDefinitionData {

        public List<GqlMoveDefinition> move;
    }

    public static class GqlMoveDefinition {

        public String name;
        public Integer power;
        public Integer accuracy;
        public Integer pp;
        public Integer priority;
        public GqlNameWrapper move_damage_class;
        public GqlNameWrapper type;
    }

    public static class GqlPokemonspeciesResponse {

        public GqlPokemonspeciesData data;
    }

    public static class GqlPokemonspeciesData {

        public List<GqlPokemonspecies> pokemonspecies;
    }

    public static class GqlPokemonspecies {

        public String name;
        public List<GqlPokemonInstance> pokemons;
    }

    public static class GqlPokemonInstance {

        public List<GqlPokemonStat> stats;
        public List<GqlPokemonTypeLink> types;
        public List<GqlAbilityLink> abilities;
        public List<GqlMoveLink> moves;
    }

    public static class GqlPokemonStat {

        public int base_stat;
        public GqlNameWrapper stat;
    }

    public static class GqlPokemonTypeLink {

        public GqlNameWrapper type;
    }

    public static class GqlAbilityLink {

        public GqlNameWrapper ability;
    }

    public static class GqlMoveLink {

        public GqlNameWrapper move;
    }

    @Transactional(readOnly = true)
    public Mono<Void> loadTypeCache() {
        return Mono.fromRunnable(() -> {
            if (typeCache.isEmpty()) {
                log.info("Cargando TypeCache desde la BD...");
                typeCache.clear();
                typeRepository.findAll().forEach(type -> typeCache.put(type.getName(), type));
                log.info("TypeCache cargado con {} entradas.", typeCache.size());
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
            }
        });
    }

    private <T> Mono<T> executeGqlQuery(String query, Class<T> responseClass) {
        return pokeApiWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GqlPayload(query))
                .retrieve()
                .bodyToMono(responseClass)
                .doOnError(e -> log.error("Error GQL: {}", e.getMessage()));
    }

    @Transactional
    public Mono<Void> ingestTypes() {
        log.info("Iniciando ingesta de Tipos (esquema v1beta2)...");
        String query = """
            query {
              type(limit: 18) {
                name
              }
            }
        """;
        return executeGqlQuery(query, GqlTypeDefinitionResponse.class)
                .flatMapMany(response -> {
                    if (response == null || response.data == null || response.data.type == null) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(response.data.type);
                })
                .map(gqlType -> StaticTypeData.builder().name(gqlType.name).build())
                .collectList()
                .doOnNext(typeRepository::saveAll)
                .doOnNext(saved -> {
                    log.info("Guardados {} tipos.", saved.size());
                    typeCache.clear();
                    saved.forEach(type -> typeCache.put(type.getName(), type));
                })
                .then();
    }

    @Transactional
    public Mono<Void> ingestAbilities() {
        log.info("Iniciando ingesta de Habilidades (esquema v1beta2)...");
        String query = """
            query {
              ability(limit: 370) {
                name
                ability_effects: abilityeffecttexts(where: {language_id: {_eq: 9}}) {
                  effect
                }
              }
            }
        """;
        return executeGqlQuery(query, GqlAbilityDefinitionResponse.class)
                .flatMapMany(response -> {
                    if (response == null || response.data == null || response.data.ability == null) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(response.data.ability);
                })
                .map(gqlAbility -> {
                    String effect = (gqlAbility.ability_effects == null || gqlAbility.ability_effects.isEmpty())
                            ? "No effect text." : gqlAbility.ability_effects.get(0).effect;
                    if (effect.length() > 60000) {
                        effect = effect.substring(0, 60000);
                    }

                    return StaticAbilityData.builder()
                            .name(gqlAbility.name)
                            .description(effect)
                            .build();
                })
                .collectList()
                .doOnNext(abilityRepository::saveAll)
                .doOnNext(saved -> {
                    log.info("Guardadas {} habilidades.", saved.size());
                    abilityCache.clear();
                    saved.forEach(a -> abilityCache.put(a.getName(), a));
                })
                .then();
    }

    @Transactional
    public Mono<Void> ingestMoves() {
        log.info("Iniciando ingesta de Movimientos (esquema v1beta2)...");
        String query = """
            query {
              move(limit: 950) {
                name
                power
                accuracy
                pp
                priority
                move_damage_class: movedamageclass {
                  name
                }
                type: type {
                  name
                }
              }
            }
        """;
        return executeGqlQuery(query, GqlMoveDefinitionResponse.class)
                .flatMapMany(response -> {
                    if (response == null || response.data == null || response.data.move == null) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(response.data.move);
                })
                .handle((gqlMove, sink) -> {
                    StaticTypeData moveType = typeCache.get(gqlMove.type != null ? gqlMove.type.name : "");

                    if (moveType == null) {
                        log.warn("Saltando movimiento '{}': Tipo '{}' no encontrado en DB.", gqlMove.name, (gqlMove.type != null ? gqlMove.type.name : "null"));
                        return;
                    }

                    StaticMoveData moveData = StaticMoveData.builder()
                            .name(gqlMove.name)
                            .power(Objects.requireNonNullElse(gqlMove.power, 0))
                            .accuracy(Objects.requireNonNullElse(gqlMove.accuracy, 0))
                            .pp(Objects.requireNonNullElse(gqlMove.pp, 0))
                            .priority(Objects.requireNonNullElse(gqlMove.priority, 0))
                            .damageClass(gqlMove.move_damage_class != null ? gqlMove.move_damage_class.name : "unknown")
                            .type(moveType)
                            .build();

                    sink.next(moveData);
                })
                .cast(StaticMoveData.class)
                .collectList()
                .doOnNext(moveRepository::saveAll)
                .doOnNext(saved -> {
                    log.info("Guardados {} movimientos.", saved.size());
                    moveCache.clear();
                    saved.forEach(m -> moveCache.put(m.getName(), m));
                })
                .then();
    }

    @Transactional
    public Mono<Void> ingestPokemon() {
        log.info("Iniciando ingesta de Pokémon (1-151) (esquema v1beta2)...");
        String query = """
            query {
              pokemonspecies(limit: 151, order_by: {id: asc}) {
                name
                pokemons: pokemon(limit: 1) {
                  stats: pokemonstats {
                    base_stat
                    stat: stat {
                      name
                    }
                  }
                  types: pokemontypes {
                    type: type {
                      name
                    }
                  }
                  abilities: pokemonabilities {
                    ability: ability {
                      name
                    }
                  }
                  moves: pokemonmoves(limit: 50) {
                    move: move {
                      name
                    }
                  }
                }
              }
            }
        """;
        return executeGqlQuery(query, GqlPokemonspeciesResponse.class)
                .flatMapMany(response -> {
                    if (response == null || response.data == null || response.data.pokemonspecies == null) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(response.data.pokemonspecies);
                })
                .handle((gqlSpecies, sink) -> {
                    if (gqlSpecies.pokemons == null || gqlSpecies.pokemons.isEmpty()) {
                        return;
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

                    StaticPokemonData pokemon = StaticPokemonData.builder()
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

                    sink.next(pokemon);
                })
                .cast(StaticPokemonData.class)
                .collectList()
                .doOnNext(pokemonRepository::saveAll)
                .doOnNext(saved -> log.info("Guardados {} Pokémon.", saved.size()))
                .then();
    }

    @Transactional
    public Mono<Void> ingestTypeDamageRelations() {
        log.info("Iniciando ingesta de Relaciones de Daño de Tipos (esquema v1beta2)...");
        String query = """
            query {
              type(limit: 18) {
                name
                typeefficacies: typeefficacies {
                  damage_factor
                  damage_type: type {
                    name
                  }
                }
              }
            }
        """;

        return executeGqlQuery(query, GqlTypeDefinitionResponse.class)
                .map(response -> {
                    if (response == null || response.data == null || response.data.type == null) {
                        return Collections.<GqlTypeDefinition>emptyList();
                    }
                    return response.data.type;
                })
                .flatMap(allTypeDefs -> {
                    for (GqlTypeDefinition attackingTypeGql : allTypeDefs) {
                        StaticTypeData attackingType = typeCache.get(attackingTypeGql.name);
                        if (attackingType == null || attackingTypeGql.typeefficacies == null) {
                            continue;
                        }

                        for (GqlTypeDamage efficacy : attackingTypeGql.typeefficacies) {
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
                            .doOnNext(typeRepository::saveAll);
                })
                .then();
    }
}

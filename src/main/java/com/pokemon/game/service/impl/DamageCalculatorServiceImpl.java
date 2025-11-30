package com.pokemon.game.service.impl;

import com.pokemon.game.model.PlayerPokemon;
import com.pokemon.game.model.StaticMoveData;
import com.pokemon.game.model.StaticTypeData;
import com.pokemon.game.repository.StaticMoveDataRepository;
import com.pokemon.game.repository.StaticTypeDataRepository;
import com.pokemon.game.service.IDamageCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class DamageCalculatorServiceImpl implements IDamageCalculatorService {

    private final StaticMoveDataRepository moveRepository;
    private final StaticTypeDataRepository typeRepository;

    @Override
    public int calculateDamage(PlayerPokemon attacker, PlayerPokemon defender, StaticMoveData move) {

        boolean isSpecial = "special".equalsIgnoreCase(move.getDamageClass());

        double attackStat = isSpecial ? attacker.getBasePokemon().getBaseSpecialAttack() : attacker.getBasePokemon().getBaseAttack();
        double defenseStat = isSpecial ? defender.getBasePokemon().getBaseSpecialDefense() : defender.getBasePokemon().getBaseDefense();

        int level = attacker.getLevel();
        int power = move.getPower();

        double baseDamage = ((((2.0 * level / 5.0 + 2.0) * attackStat * power / defenseStat) / 50.0) + 2.0);
        double typeMultiplier = getTypeEffectiveness(move.getType(), defender.getBasePokemon().getTypes());

        double stabMultiplier = 1.0;
        if (attacker.getBasePokemon().getTypes().contains(move.getType())) {
            stabMultiplier = 1.5;
        }

        double randomMultiplier = 0.85 + (Math.random() * 0.15);

        double totalDamage = baseDamage * typeMultiplier * stabMultiplier * randomMultiplier;

        if (typeMultiplier == 0.0) {
            return 0;
        }
        return (int) Math.max(1, totalDamage);
    }

    private double getTypeEffectiveness(StaticTypeData moveType, Set<StaticTypeData> defenderTypes) {
        double multiplier = 1.0;

        for (StaticTypeData defenderType : defenderTypes) {
            if (defenderType.getDoubleDamageFrom().contains(moveType)) {
                multiplier *= 2.0;
            } else if (defenderType.getHalfDamageFrom().contains(moveType)) {
                multiplier *= 0.5;
            } else if (defenderType.getNoDamageFrom().contains(moveType)) {
                multiplier *= 0.0;
            }
        }
        return multiplier;
    }
}

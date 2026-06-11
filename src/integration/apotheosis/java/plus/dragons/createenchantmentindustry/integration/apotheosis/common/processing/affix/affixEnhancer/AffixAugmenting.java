/*
 * Copyright (C) 2025  DragonsPlus
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package plus.dragons.createenchantmentindustry.integration.apotheosis.common.processing.affix.affixEnhancer;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.processing.affix.AffixOperationCosts;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.processing.affix.blazeComposer.AffixComposingRules;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.processing.affix.blazeComposer.OverlimitAffixHelper;
import plus.dragons.createenchantmentindustry.integration.apotheosis.config.CEIAXConfig;

public class AffixAugmenting {
    public static Optional<Result> getResult(ItemStack stack) {
        if (stack.isEmpty())
            return Optional.empty();
        float maxLevel = CEIAXConfig.server().affixes().affixAugmentorMaxLevel.getF();
        if (maxLevel <= AffixOperationCosts.EPSILON)
            return Optional.empty();
        return AffixHelper.streamAffixes(stack)
                .filter(AffixInstance::isValid)
                .filter(instance -> canAugment(instance, maxLevel))
                .sorted(Comparator
                        .comparingDouble(AffixInstance::level)
                        .thenComparing(instance -> instance.affix().getId()))
                .findFirst()
                .map(instance -> createResult(instance, maxLevel));
    }

    public static boolean canAugment(ItemStack stack) {
        return getResult(stack).isPresent();
    }

    public static ItemStack apply(ItemStack stack, Result result) {
        ItemStack output = stack.copy();
        output.setCount(1);
        OverlimitAffixHelper.setAffixLevel(output, result.target().affix(), result.resultLevel());
        return output;
    }

    private static boolean canAugment(AffixInstance instance, float maxLevel) {
        return !instance.isLevelIndependent()
                && instance.level() < maxLevel - AffixOperationCosts.EPSILON
                && !AffixComposingRules.INSTANCE.deniesAugmenting(instance);
    }

    private static Result createResult(AffixInstance instance, float maxLevel) {
        float currentLevel = instance.level();
        float resultLevel = Math.min(currentLevel + CEIAXConfig.server().affixes().affixTemplateMergeStep.getF(), maxLevel);
        int cost = AffixOperationCosts.augmentingCost(instance, currentLevel, resultLevel);
        return new Result(instance, currentLevel, resultLevel, cost);
    }

    public record Result(AffixInstance target, float currentLevel, float resultLevel, int cost) {}
}

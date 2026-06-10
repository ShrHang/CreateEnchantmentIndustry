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

package plus.dragons.createenchantmentindustry.integration.apotheosis.common.processing.affix.blazeComposer;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixType;
import plus.dragons.createenchantmentindustry.integration.apotheosis.config.CEIAXConfig;

public class BlazeComposingCost {
    public static int calculate(BlazeComposerMode mode, AffixTemplateTier tier, AffixTemplateData data, float resultLevel) {
        var config = CEIAXConfig.server().affixes();
        float cost = baseCost(mode);
        cost += config.blazeComposerCostPerLevel.get() * levelCostFactor(resultLevel);
        cost *= tierMultiplier(tier);
        cost *= typeMultiplier(data.affix().get().definition().type());
        cost *= AffixComposingRules.INSTANCE.getCostMultiplier(data);
        return Math.max(1, Math.round(cost));
    }

    public static float levelCostFactor(float level) {
        var config = CEIAXConfig.server().affixes();
        float standard = Math.min(level, Affix.STANDARD_MAX_LEVEL);
        float crystal = Math.max(0, Math.min(level, Affix.MAX_LEVEL) - Affix.STANDARD_MAX_LEVEL);
        float hyper = Math.max(0, level - Affix.MAX_LEVEL);
        return standard
                + crystal * config.blazeComposerCrystalLevelMultiplier.getF()
                + (float) Math.pow(hyper, config.blazeComposerHyperLevelExponent.getF()) * config.blazeComposerHyperLevelMultiplier.getF();
    }

    public static int baseCost(BlazeComposerMode mode) {
        var config = CEIAXConfig.server().affixes();
        return switch (mode) {
            case EXTRACT -> config.blazeComposerExtractBaseCost.get();
            case APPLY -> config.blazeComposerApplyBaseCost.get();
            case MERGE -> config.blazeComposerMergeBaseCost.get();
        };
    }

    public static float tierMultiplier(AffixTemplateTier tier) {
        var config = CEIAXConfig.server().affixes();
        return switch (tier) {
            case BRASS -> config.brassAffixTemplateCostMultiplier.getF();
            case CRYSTAL -> config.crystalAffixTemplateCostMultiplier.getF();
            case APOTHEOTIC -> config.apotheoticAffixTemplateCostMultiplier.getF();
        };
    }

    public static float typeMultiplier(AffixType type) {
        var config = CEIAXConfig.server().affixes();
        return switch (type) {
            case STAT -> config.statAffixTypeCostMultiplier.getF();
            case BASIC_EFFECT -> config.basicEffectAffixTypeCostMultiplier.getF();
            case ABILITY -> config.abilityAffixTypeCostMultiplier.getF();
        };
    }
}

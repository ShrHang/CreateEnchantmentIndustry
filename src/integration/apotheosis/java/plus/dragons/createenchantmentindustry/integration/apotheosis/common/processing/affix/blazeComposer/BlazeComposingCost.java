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
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.processing.affix.AffixOperationCosts;
import plus.dragons.createenchantmentindustry.integration.apotheosis.config.CEIAXConfig;

public class BlazeComposingCost {
    public static int calculate(Operation operation, BlazeComposerMode mode, AffixTemplateTier tier, AffixTemplateData data, float fromLevel, float resultLevel) {
        float cost = baseCost(mode);
        cost += levelCost(operation, fromLevel, resultLevel);
        cost *= tierMultiplier(tier);
        cost *= AffixOperationCosts.typeMultiplier(data.affix().get().definition().type());
        cost *= AffixComposingRules.INSTANCE.getCostMultiplier(data);
        return AffixOperationCosts.roundCost(cost);
    }

    public static float levelCost(Operation operation, float fromLevel, float resultLevel) {
        var config = CEIAXConfig.server().affixes();
        float stepWeight = Math.max(AffixOperationCosts.EPSILON, AffixOperationCosts.weightedLevelSpan(0, AffixOperationCosts.APOTHEOSIS_AUGMENTING_STEP));
        float standardEnd = Math.min(resultLevel, Affix.MAX_LEVEL);
        float standardCost = AffixOperationCosts.apotheosisUpgradeReferenceCost()
                * AffixOperationCosts.weightedLevelSpan(fromLevel, standardEnd)
                / stepWeight
                * operation.multiplier();
        if (standardCost > 0) {
            standardCost = Math.min(
                    standardCost,
                    AffixOperationCosts.apotheosisUpgradeReferenceCost() * config.blazeComposerStandardOperationCostCap.getF());
        }
        float hyperCost = AffixOperationCosts.apotheosisUpgradeReferenceCost()
                * AffixOperationCosts.weightedLevelSpan(Math.max(fromLevel, Affix.MAX_LEVEL), resultLevel)
                / stepWeight
                * operation.multiplier();
        return standardCost + hyperCost;
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

    public enum Operation {
        EXTRACT_SNAPSHOT,
        APPLY_NEW_TEMPLATE,
        APPLY_UPGRADE_DELTA,
        MERGE_UPGRADE_DELTA;

        public float multiplier() {
            var config = CEIAXConfig.server().affixes();
            return switch (this) {
                case EXTRACT_SNAPSHOT -> config.blazeComposerExtractSnapshotMultiplier.getF();
                case APPLY_NEW_TEMPLATE -> config.blazeComposerApplyNewTemplateMultiplier.getF();
                case APPLY_UPGRADE_DELTA -> config.blazeComposerApplyUpgradeDeltaMultiplier.getF();
                case MERGE_UPGRADE_DELTA -> config.blazeComposerMergeUpgradeDeltaMultiplier.getF();
            };
        }
    }
}

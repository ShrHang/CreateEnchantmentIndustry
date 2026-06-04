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

package plus.dragons.createenchantmentindustry.integration.apotheosis.common.processing.socket.gem.gemCutter;

import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import java.util.HashMap;
import java.util.Map;
import plus.dragons.createenchantmentindustry.integration.apotheosis.config.CEIAXConfig;

public class GemCutting {
    public static final Map<Purity, Integer> PURITY_COST = new HashMap<>();

    protected static int getCutCost(Purity purity) {
        if (PURITY_COST.isEmpty()) buildPurityCostMap();
        return PURITY_COST.get(purity);
    }

    private static void buildPurityCostMap() {
        for (Purity purity : Purity.values()) {
            if (purity == Purity.PERFECT) continue;
            PURITY_COST.put(purity, getGemEqual(Purity.BY_ID.apply(purity.ordinal() + 1)));
        }
    }

    private static int getGemEqual(Purity purity) {
        if (purity == Purity.CRACKED) return CEIAXConfig.server().fluids().gemCutterCostCrackedGemToCrystalEssenceRatio.get();
        else return (1 + purity.ordinal() * 2) * CEIAXConfig.server().fluids().gemCutterCostGemDustToCrystalEssenceRatio.get() +
                getGemEqual(Purity.BY_ID.apply(purity.ordinal() - 1))
                + pow3(purity.ordinal() + 1) * CEIAXConfig.server().fluids().gemCutterCostApotheoticEssenceCostToCrystalEssenceRatio.get();
    }

    private static int pow3(int exponent) {
        int result = 1;
        for (int i = 0; i < exponent; i++)
            result *= 3;
        return result;
    }
}

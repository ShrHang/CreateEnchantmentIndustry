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

package plus.dragons.createenchantmentindustry.config;

import net.createmod.catnip.config.ConfigBase;

public class CEIProcessingConfig extends ConfigBase {
    public final ConfigFloat regularLightningStrikeTransformXpBlockChance = f(1, 0, 1,
            "regularLightningStrikeTransformXpBlockChance",
            CEIProcessingConfig.Comments.regularLightningStrikeTransformXpBlockChance);
    public final ConfigInt classicBlazeEnchanterFluidCapacity = i(4000, 1000,
            "classicBlazeEnchanterFluidCapacity",
            Comments.classicBlazeEnchanterFluidCapacity);
    public final ConfigFloat classicBlazeEnchanterNormalEnchantingCostCoefficient = f(1.0f, 0.01f,
            "classicBlazeEnchanterNormalEnchantingCostCoefficient",
            Comments.classicBlazeEnchanterNormalEnchantingCostCoefficient);
    public final ConfigFloat classicBlazeEnchanterSuperEnchantingCostCoefficient = f(1.0f, 0.01f,
            "classicBlazeEnchanterSuperEnchantingCostCoefficient",
            Comments.classicBlazeEnchanterSuperEnchantingCostCoefficient);
    public final ConfigFloat classicBlazeEnchanterSuperEnchantingCurseLevelDroppingRate = f(0.25f, 0.01f,
            "classicBlazeEnchanterSuperEnchantingCurseLevelDroppingRate",
            Comments.classicBlazeEnchanterSuperEnchantingCurseLevelDroppingRate);

    @Override
    public String getName() {
        return "processing";
    }

    static class Comments {
        static final String regularLightningStrikeTransformXpBlockChance = "Probability of natural lightning strikes transforming Blocks of Experience.";
        static final String classicBlazeEnchanterFluidCapacity = "The amount of liquid a Classic Blaze Enchanter can hold (mB).";
        static final String classicBlazeEnchanterNormalEnchantingCostCoefficient = "Experience cost coefficient of Classic Blaze Enchanter regular enchanting.";
        static final String classicBlazeEnchanterSuperEnchantingCostCoefficient = "Experience cost coefficient of Classic Blaze Enchanter super enchanting.";
        static final String classicBlazeEnchanterSuperEnchantingCurseLevelDroppingRate = "Probability that a cursed Classic Blaze Enchanter super enchants resulting in a drop in enchantment level.";
    }
}

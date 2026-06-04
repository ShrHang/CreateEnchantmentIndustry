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

package plus.dragons.createenchantmentindustry.integration.apotheosis.config;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ui.ConfigAnnotations;
import net.neoforged.neoforge.common.ModConfigSpec;

public class CEIAXFluidConfig extends ConfigBase {
    public final ConfigInt gemCutterCostGemDustToCrystalEssenceRatio = i(10, 1,
            "gemCutterCostGemDustToCrystalEssenceRatio",
            Comments.gemCutterCostGemDustToCrystalEssenceRatio);

    public final ConfigInt gemCutterCostCrackedGemToCrystalEssenceRatio = i(20, 1,
            "gemCutterCostCrackedGemToCrystalEssenceRatio",
            Comments.gemCutterCostCrackedGemToCrystalEssenceRatio);

    public final ConfigInt gemCutterCostApotheoticEssenceCostToCrystalEssenceRatio = i(10, 1,
            "gemCutterCostApotheoticEssenceCostToCrystalEssenceRatio",
            Comments.gemCutterCostApotheoticEssenceCostToCrystalEssenceRatio);

    public final ConfigInt affixAugmentorCostExperienceToApotheoticEssenceTotal = i(19347, 1,
            "affixAugmentorCostExperienceToApotheoticEssenceTotal",
            Comments.affixAugmentorCostExperienceToApotheoticEssenceTotal);

    public final ConfigInt affixAugmentorCostSigilToApotheoticEssenceRatio = i(81, 1,
            "affixAugmentorCostSigilToApotheoticEssenceRatio",
            Comments.affixAugmentorCostSigilToApotheoticEssenceRatio);

    @Override
    public void registerAll(ModConfigSpec.Builder builder) {
        super.registerAll(builder);
    }

    @Override
    public String getName() {
        return "ex-fluid";
    }

    static class Comments {
        static final String[] gemCutterCostGemDustToCrystalEssenceRatio = { "This setting affects the calculation of operating costs of Gem Cutter.",
                "In Apotheosis, Gem cutting/upgrading requires consuming one Gem of the same type & purity,",
                "a certain quantity of Gem Dust, and Rarity Materials.",
                "Specifically, it typically requires 1 + (purity level) x 2 Gem Dust, alongside Rarity Materials equivalent to 3^(purity level) units of common Rarity Material.",
                "Therefore, when calculating processing cost of Gem Cutter, both Gem Dust consumption and Gem consumption are converted into Crystal Essence,",
                "while Rarity Material consumption is converted into Apotheotic Essence, which is then converted into Crystal Essence.",
                "This config determines the conversion ratio of Gem Dust to Crystal Essence (mB) IN GEM CUTTING ONLY.",
                ConfigAnnotations.RequiresRestart.SERVER.asComment() };
        static final String[] gemCutterCostCrackedGemToCrystalEssenceRatio = { "The config above has explained the calculation mechanism for the operating costs of Gem Cutter.",
                "This config determines the conversion ratio of Gem of \"Cracked\" purity to Crystal Essence (mB) IN GEM CUTTING ONLY.",
                ConfigAnnotations.RequiresRestart.SERVER.asComment() };
        static final String[] gemCutterCostApotheoticEssenceCostToCrystalEssenceRatio = { "The config above has explained the calculation mechanism for the operating costs of Gem Cutter.",
                "This config determines the conversion ratio of Apotheotic Essence (mB) to Crystal Essence (mB) IN GEM CUTTER ONLY.",
                ConfigAnnotations.RequiresRestart.SERVER.asComment() };

        static final String[] affixAugmentorCostExperienceToApotheoticEssenceTotal = { "This setting affects the calculation of operating costs of Affix Augmentor.",
                "In Apotheosis, Affix augmenting requires consuming 225 levels of Player Experience and 2 Sigil of Enhancement.",
                "Therefore, when calculating processing cost of Affix Augmentor, to ease customization, Experience consumption is replaced by Apotheotic Essence,",
                "while Sigil of Enhancement consumption is converted into Apotheotic Essence too.",
                "This config determines the total Apotheotic Essence (mB) that replaces Experience Cost of Augmenting Table IN AFFIX AUGMENTOR ONLY.",
                ConfigAnnotations.RequiresRestart.SERVER.asComment() };
        static final String[] affixAugmentorCostSigilToApotheoticEssenceRatio = { "The config above has explained the calculation mechanism for the operating costs of Affix Augmentor.",
                "This config determines the conversion ratio of Sigil of Enhancement to Apotheotic Essence (mB) IN AFFIX AUGMENTOR ONLY.",
                ConfigAnnotations.RequiresRestart.SERVER.asComment() };
    }
}

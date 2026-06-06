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

package plus.dragons.createenchantmentindustry.integration.apotheosis.common.registry;

import static plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.CEIACommon.REGISTRATE;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.Tags;
import plus.dragons.createdragonsplus.data.tag.ItemTagRegistry;
import plus.dragons.createenchantmentindustry.common.CEICommon;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.processing.affix.affixComposer.AffixTemplateItem;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.CEIACommon;

public class CEIAXItems {
    public static final ModTags MOD_TAGS = new ModTags();

    public static final ItemEntry<AffixTemplateItem> BRASS_AFFIX_TEMPLATE = REGISTRATE
            .item("brass_affix_template", AffixTemplateItem::normal)
            .asOptional()
            .properties(prop -> prop
                    .rarity(Rarity.COMMON))
            //.component()
            .register();

    public static final ItemEntry<AffixTemplateItem> CRYSTAL_AFFIX_TEMPLATE = REGISTRATE
            .item("crystal_affix_template", AffixTemplateItem::normal)
            .asOptional()
            .properties(prop -> prop
                    .rarity(Rarity.UNCOMMON))
            .register();

    public static final ItemEntry<AffixTemplateItem> APOTHEOTIC_AFFIX_TEMPLATE = REGISTRATE
            .item("apotheotic_affix_template", AffixTemplateItem::apotheotic)
            .asOptional()
            .properties(prop -> prop
                    .rarity(Rarity.RARE))
            .register();

    public static void register() {
        REGISTRATE.registerItemTags(MOD_TAGS);
    }

    public static class ModTags extends ItemTagRegistry {
        public ModTags() {
            super(CEIACommon.ID);
            addOptional(Tags.Items.BUCKETS, CEICommon.asResource("apotheotic_essence_bucket"));
            addOptional(Tags.Items.BUCKETS, CEICommon.asResource("crystal_essence_bucket"));
        }
    }
}

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
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class AffixTemplateDisplay {
    public static List<Component> describeStack(ItemStack stack) {
        AffixTemplateData data = AffixTemplateOps.getTemplateData(stack);
        if (data != null && data.isBound())
            return List.of(describeTemplate(data, stack));
        List<Component> result = new ArrayList<>();
        result.add(stack.getHoverName().copy());
        AffixHelper.getAffixes(stack).values().stream()
                .filter(AffixInstance::isValid)
                .sorted(Comparator.comparing(instance -> instance.affix().getId()))
                .map(AffixTemplateDisplay::describeAffix)
                .forEach(result::add);
        return result;
    }

    public static Component describeTemplate(AffixTemplateData data, ItemStack stack) {
        return Component.translatable(
                "create_enchantment_industry.gui.goggles.blaze_composer.result.template_affix",
                affixName(data.toInstance(stack)),
                formatLevel(data.level()),
                rarityName(data));
    }

    public static Component describeTemplateUpgrade(AffixTemplateData before, AffixTemplateData after, ItemStack stack) {
        return Component.translatable(
                "create_enchantment_industry.gui.goggles.blaze_composer.result.template_affix_upgrade",
                affixName(after.toInstance(stack)),
                formatLevel(before.level()),
                formatLevel(after.level()),
                rarityName(after));
    }

    public static Component describeEquipmentAffix(ItemStack stack, AffixInstance instance) {
        return Component.translatable(
                "create_enchantment_industry.gui.goggles.blaze_composer.result.equipment_affix",
                stack.getHoverName().copy(),
                affixName(instance),
                formatLevel(instance.level()));
    }

    public static Component describeEquipmentAffixUpgrade(ItemStack stack, DynamicHolder<Affix> affix, float before, float after) {
        AffixInstance instance = AffixHelper.getAffixes(stack).get(affix);
        if (instance == null) {
            instance = new AffixInstance(affix, after, AffixHelper.getRarity(stack), stack);
        }
        if (before <= 0) {
            return Component.translatable(
                    "create_enchantment_industry.gui.goggles.blaze_composer.result.equipment_affix",
                    stack.getHoverName().copy(),
                    affixName(instance),
                    formatLevel(after));
        }
        return Component.translatable(
                "create_enchantment_industry.gui.goggles.blaze_composer.result.equipment_affix_upgrade",
                stack.getHoverName().copy(),
                affixName(instance),
                formatLevel(before),
                formatLevel(after));
    }

    public static Component describeRemovedAffix(ItemStack stack, AffixInstance instance) {
        return Component.translatable(
                "create_enchantment_industry.gui.goggles.blaze_composer.result.equipment_removed_affix",
                stack.getHoverName().copy(),
                affixName(instance),
                formatLevel(instance.level()));
    }

    public static Component describeAffix(AffixInstance instance) {
        return Component.translatable(
                "create_enchantment_industry.gui.goggles.blaze_composer.result.affix",
                affixName(instance),
                formatLevel(instance.level()));
    }

    public static MutableComponent affixName(AffixInstance instance) {
        MutableComponent name = Component.empty().append(instance.getName(true));
        if (instance.rarity().isBound()) {
            name.withStyle(style -> style.withColor(instance.getRarity().color()));
        } else {
            name.withStyle(ChatFormatting.GRAY);
        }
        return name;
    }

    public static Component rarityName(AffixTemplateData data) {
        if (!data.rarity().isBound())
            return data.rarity().get().toComponent().withStyle(ChatFormatting.RED);
        return data.rarity().get().toComponent().withStyle(style -> style.withColor(data.rarity().get().color()));
    }

    public static String formatLevel(float level) {
        if (level == (int) level) {
            return Integer.toString((int) level);
        }
        return String.format("%.2f", level);
    }
}

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

import dev.shadowsoffire.apotheosis.affix.AttributeProvidingAffix;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.registry.CEIAXDataComponents;

public class AffixTemplateItem extends Item {
    private final AffixTemplateTier tier;

    public AffixTemplateItem(Properties properties, AffixTemplateTier tier) {
        super(properties);
        this.tier = tier;
    }

    public static AffixTemplateItem brass(Properties properties) {
        return new AffixTemplateItem(properties, AffixTemplateTier.BRASS);
    }

    public static AffixTemplateItem crystal(Properties properties) {
        return new AffixTemplateItem(properties, AffixTemplateTier.CRYSTAL);
    }

    public static AffixTemplateItem apotheotic(Properties properties) {
        return new AffixTemplateItem(properties, AffixTemplateTier.APOTHEOTIC);
    }

    public AffixTemplateTier tier() {
        return tier;
    }

    public boolean canHold(float level) {
        return tier.canHold(level);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(CEIAXDataComponents.AFFIX_TEMPLATE.get());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var data = stack.get(CEIAXDataComponents.AFFIX_TEMPLATE.get());
        if (data == null) {
            tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.blank")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.capacity", AffixTemplateDisplay.formatLevel(tier.getMaxLevel()))
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (!data.isBound()) {
            tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.unbound")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal(data.affix().getId().toString()).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        var rarity = data.rarity().get();
        var instance = data.toInstance(stack);
        MutableComponent affixName = Component.empty().append(instance.getName(true));
        tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.affix", affixName)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.rarity", data.rarity().get().toComponent()
                .withStyle(style -> style.withColor(rarity.color())))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.level", AffixTemplateDisplay.formatLevel(data.level()), AffixTemplateDisplay.formatLevel(tier.getMaxLevel()))
                .withStyle(data.level() > tier.getMaxLevel() ? ChatFormatting.RED : data.transcendent() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.category", Component.translatable(data.sourceCategory().toLanguageKey("loot_category")))
                .withStyle(ChatFormatting.DARK_GRAY));
        if (data.transcendent()) {
            tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.transcendent")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        addAffixEffectTooltip(stack, context, tooltip, flag, data);
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal(data.affix().getId().toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void addAffixEffectTooltip(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag, AffixTemplateData data) {
        var instance = data.toInstance(stack);
        var tooltipContext = AttributeTooltipContext.of(null, context, flag);
        List<Component> effects = new ArrayList<>();
        Component description = instance.getDescription(tooltipContext);
        if (description.getContents() != PlainTextContents.EMPTY) {
            effects.add(description);
        }
        if (instance.getAffix() instanceof AttributeProvidingAffix provider) {
            provider.gatherModifierTooltips(instance, tooltipContext, effects::add);
        }
        tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.effect")
                .withStyle(ChatFormatting.DARK_GRAY));
        if (effects.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.effect.unknown")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        for (Component effect : effects) {
            tooltip.add(Component.translatable("tooltip.create_enchantment_industry.affix_template.effect.line", effect)
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}

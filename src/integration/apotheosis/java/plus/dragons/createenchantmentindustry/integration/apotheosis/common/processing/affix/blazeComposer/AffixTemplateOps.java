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

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.registry.CEIAXDataComponents;
import plus.dragons.createenchantmentindustry.integration.apotheosis.config.CEIAXConfig;

public class AffixTemplateOps {
    private static final float EPSILON = 0.0001F;

    public static Result compose(BlazeComposerMode mode, boolean hyper, ItemStack firstInput, ItemStack secondInput) {
        if (firstInput.isEmpty() && secondInput.isEmpty())
            return Result.emptyInput();
        return switch (mode) {
            case EXTRACT -> extract(hyper, firstInput, secondInput);
            case APPLY -> apply(hyper, firstInput, secondInput);
            case MERGE -> merge(hyper, firstInput, secondInput);
        };
    }

    public static Result extract(boolean hyper, ItemStack equipmentInput, ItemStack templateInput) {
        if (equipmentInput.isEmpty())
            return incomplete(FailureReason.MISSING_AFFIXED_EQUIPMENT);
        if (templateInput.isEmpty())
            return incomplete(FailureReason.MISSING_BLANK_TEMPLATE);

        AffixTemplateItem templateItem = getTemplateItem(templateInput);
        if (templateItem == null || !isBlankTemplate(templateInput))
            return invalid(FailureReason.REQUIRES_BLANK_TEMPLATE);
        Result modeFailure = validateTemplateMode(templateItem.tier(), hyper);
        if (modeFailure != null)
            return modeFailure;

        DynamicHolder<LootRarity> rarity = AffixHelper.getRarity(equipmentInput);
        if (!rarity.isBound())
            return invalid(FailureReason.EQUIPMENT_HAS_NO_RARITY);

        LootCategory category = LootCategory.forItem(equipmentInput);
        if (category.isNone())
            return invalid(FailureReason.ITEM_HAS_NO_LOOT_CATEGORY, equipmentInput.getHoverName().copy());

        AffixInstance instance = firstAffix(equipmentInput);
        if (instance == null)
            return invalid(FailureReason.EQUIPMENT_HAS_NO_AFFIX);

        AffixTemplateData data = new AffixTemplateData(
                instance.affix(),
                instance.level(),
                rarity,
                category.getKey(),
                instance.level() > Affix.MAX_LEVEL);
        Result levelFailure = validateExtractionLevel(templateItem.tier(), hyper, data);
        if (levelFailure != null)
            return levelFailure;
        if (!canTemplateHold(templateItem, data))
            return invalid(
                    FailureReason.TEMPLATE_CANNOT_HOLD_LEVEL,
                    AffixTemplateDisplay.formatLevel(data.level()),
                    AffixTemplateDisplay.formatLevel(maxLevel(templateItem.tier(), data)));
        if (AffixComposingRules.INSTANCE.denies(BlazeComposerMode.EXTRACT, hyper, data))
            return invalid(FailureReason.AFFIX_DENIED_BY_RULE, AffixTemplateDisplay.affixName(instance), modeName(BlazeComposerMode.EXTRACT));

        ItemStack equipment = single(equipmentInput);
        OverlimitAffixHelper.removeAffix(equipment, instance.affix());
        rebuildAffixName(equipment);
        ItemStack template = single(templateInput);
        setTemplateData(template, data);
        int cost = BlazeComposingCost.calculate(BlazeComposerMode.EXTRACT, templateItem.tier(), data, data.level());
        return Result.ready(
                equipment,
                template,
                cost,
                AffixTemplateDisplay.describeRemovedAffix(equipment, instance),
                AffixTemplateDisplay.describeTemplate(data, template));
    }

    public static Result apply(boolean hyper, ItemStack equipmentInput, ItemStack templateInput) {
        if (equipmentInput.isEmpty())
            return incomplete(FailureReason.MISSING_EQUIPMENT);
        if (templateInput.isEmpty())
            return incomplete(FailureReason.MISSING_FILLED_TEMPLATE);

        AffixTemplateItem templateItem = getTemplateItem(templateInput);
        AffixTemplateData data = getTemplateData(templateInput);
        if (templateItem == null || data == null)
            return invalid(FailureReason.REQUIRES_FILLED_TEMPLATE);
        Result modeFailure = validateTemplateMode(templateItem.tier(), hyper);
        if (modeFailure != null)
            return modeFailure;
        if (!data.isBound())
            return invalid(FailureReason.UNBOUND_TEMPLATE_DATA);
        if (AffixComposingRules.INSTANCE.denies(BlazeComposerMode.APPLY, hyper, data))
            return invalid(FailureReason.AFFIX_DENIED_BY_RULE, AffixTemplateDisplay.affixName(data.toInstance(templateInput)), modeName(BlazeComposerMode.APPLY));

        ItemStack equipment = single(equipmentInput);
        DynamicHolder<LootRarity> existingRarity = AffixHelper.getRarity(equipment);
        if (!existingRarity.isBound()) {
            equipment.set(Apoth.Components.RARITY, data.rarity());
        } else if (!existingRarity.equals(data.rarity())) {
            if (!CEIAXConfig.server().affixes().allowRarityMismatchApplying.get())
                return invalid(FailureReason.RARITY_MISMATCH_DISALLOWED, rarityName(existingRarity), AffixTemplateDisplay.rarityName(data));
            equipment.set(Apoth.Components.RARITY, data.rarity());
        }

        LootCategory category = LootCategory.forItem(equipment);
        if (category.isNone())
            return invalid(FailureReason.ITEM_HAS_NO_LOOT_CATEGORY, equipment.getHoverName().copy());
        if (!data.affix().get().canApplyTo(equipment, category, data.rarity().get()))
            return invalid(FailureReason.AFFIX_CANNOT_APPLY_TO_ITEM, AffixTemplateDisplay.affixName(data.toInstance(templateInput)), equipment.getHoverName().copy());

        ItemAffixes currentNative = equipment.getOrDefault(Apoth.Components.AFFIXES, ItemAffixes.EMPTY);
        ItemAffixes compatibilityAffixes = currentNative.toBuilder().remove(data.affix()).build();
        boolean bypassExclusiveSet = hyper && CEIAXConfig.server().affixes().allowExclusiveSetBypassInHyperMode.get();
        if (!bypassExclusiveSet && !data.affix().get().isCompatibleWith(compatibilityAffixes))
            return invalid(FailureReason.AFFIX_INCOMPATIBLE_WITH_EQUIPMENT, AffixTemplateDisplay.affixName(data.toInstance(templateInput)), equipment.getHoverName().copy());

        AffixInstance current = AffixHelper.getAffixes(equipment).get(data.affix());
        float currentLevel = current == null ? 0 : current.level();
        float maxLevel = maxLevel(templateItem.tier(), data);
        float resultLevel = currentLevel <= 0
                ? data.level()
                : nearlyEqual(currentLevel, data.level())
                        ? Math.min(currentLevel + CEIAXConfig.server().affixes().affixTemplateMergeStep.getF(), maxLevel)
                        : Math.max(currentLevel, data.level());
        if (resultLevel > maxLevel + EPSILON)
            return invalid(
                    FailureReason.TEMPLATE_CANNOT_HOLD_LEVEL,
                    AffixTemplateDisplay.formatLevel(resultLevel),
                    AffixTemplateDisplay.formatLevel(maxLevel));
        if (resultLevel <= currentLevel + EPSILON)
            return currentLevel >= maxLevel - EPSILON
                    ? invalid(FailureReason.ALREADY_AT_TEMPLATE_CAP, AffixTemplateDisplay.formatLevel(maxLevel))
                    : invalid(FailureReason.WOULD_NOT_IMPROVE);

        OverlimitAffixHelper.setAffixLevel(equipment, data.affix(), resultLevel);
        rebuildAffixName(equipment);
        AffixTemplateData costData = data.withLevel(resultLevel);
        int cost = BlazeComposingCost.calculate(BlazeComposerMode.APPLY, templateItem.tier(), costData, resultLevel);
        return Result.ready(
                equipment,
                ItemStack.EMPTY,
                cost,
                AffixTemplateDisplay.describeEquipmentAffixUpgrade(equipment, data.affix(), currentLevel, resultLevel));
    }

    public static Result merge(boolean hyper, ItemStack firstTemplateInput, ItemStack secondTemplateInput) {
        if (firstTemplateInput.isEmpty())
            return incomplete(FailureReason.MISSING_FILLED_TEMPLATE);
        if (secondTemplateInput.isEmpty())
            return incomplete(FailureReason.MISSING_SECOND_FILLED_TEMPLATE);

        AffixTemplateItem firstItem = getTemplateItem(firstTemplateInput);
        AffixTemplateItem secondItem = getTemplateItem(secondTemplateInput);
        AffixTemplateData firstData = getTemplateData(firstTemplateInput);
        AffixTemplateData secondData = getTemplateData(secondTemplateInput);
        if (firstItem == null || firstData == null)
            return invalid(FailureReason.FIRST_REQUIRES_FILLED_TEMPLATE);
        if (secondItem == null || secondData == null)
            return invalid(FailureReason.SECOND_REQUIRES_FILLED_TEMPLATE);
        Result firstModeFailure = validateTemplateMode(firstItem.tier(), hyper);
        if (firstModeFailure != null)
            return firstModeFailure;
        Result secondModeFailure = validateTemplateMode(secondItem.tier(), hyper);
        if (secondModeFailure != null)
            return secondModeFailure;
        if (!firstData.isBound() || !secondData.isBound())
            return invalid(FailureReason.UNBOUND_TEMPLATE_DATA);
        if (!firstData.affix().equals(secondData.affix()))
            return invalid(FailureReason.TEMPLATE_AFFIX_MISMATCH);
        if (!firstData.rarity().equals(secondData.rarity()))
            return invalid(FailureReason.TEMPLATE_RARITY_MISMATCH, AffixTemplateDisplay.rarityName(firstData), AffixTemplateDisplay.rarityName(secondData));
        if (!canUpgrade(firstData, firstTemplateInput) || !canUpgrade(secondData, secondTemplateInput))
            return invalid(FailureReason.LEVEL_INDEPENDENT_AFFIX, AffixTemplateDisplay.affixName(firstData.toInstance(firstTemplateInput)));

        AffixTemplateTier tier = AffixTemplateTier.highest(firstItem.tier(), secondItem.tier());
        ItemStack result = single(firstItem.tier().isAtLeast(secondItem.tier()) ? firstTemplateInput : secondTemplateInput);
        float maxLevel = maxLevel(tier, firstData);
        float highestInputLevel = Math.max(firstData.level(), secondData.level());
        float resultLevel = nearlyEqual(firstData.level(), secondData.level())
                ? Math.min(firstData.level() + CEIAXConfig.server().affixes().affixTemplateMergeStep.getF(), maxLevel)
                : highestInputLevel;
        if (resultLevel > maxLevel + EPSILON)
            return invalid(
                    FailureReason.TEMPLATE_CANNOT_HOLD_LEVEL,
                    AffixTemplateDisplay.formatLevel(resultLevel),
                    AffixTemplateDisplay.formatLevel(maxLevel));
        if (resultLevel <= highestInputLevel + EPSILON)
            return highestInputLevel >= maxLevel - EPSILON
                    ? invalid(FailureReason.ALREADY_AT_TEMPLATE_CAP, AffixTemplateDisplay.formatLevel(maxLevel))
                    : invalid(FailureReason.WOULD_NOT_IMPROVE);

        AffixTemplateData resultData = firstData.withLevel(resultLevel);
        if (AffixComposingRules.INSTANCE.denies(BlazeComposerMode.MERGE, hyper, resultData))
            return invalid(FailureReason.AFFIX_DENIED_BY_RULE, AffixTemplateDisplay.affixName(firstData.toInstance(firstTemplateInput)), modeName(BlazeComposerMode.MERGE));

        setTemplateData(result, resultData);
        int cost = BlazeComposingCost.calculate(BlazeComposerMode.MERGE, tier, resultData, resultLevel);
        return Result.ready(
                result,
                ItemStack.EMPTY,
                cost,
                AffixTemplateDisplay.describeTemplateUpgrade(firstData, resultData, result));
    }

    public static AffixTemplateItem getTemplateItem(ItemStack stack) {
        return stack.getItem() instanceof AffixTemplateItem template ? template : null;
    }

    public static AffixTemplateData getTemplateData(ItemStack stack) {
        return stack.get(CEIAXDataComponents.AFFIX_TEMPLATE.get());
    }

    public static void setTemplateData(ItemStack stack, AffixTemplateData data) {
        stack.set(CEIAXDataComponents.AFFIX_TEMPLATE.get(), data);
    }

    public static boolean isBlankTemplate(ItemStack stack) {
        return getTemplateItem(stack) != null && getTemplateData(stack) == null;
    }

    public static boolean isFilledTemplate(ItemStack stack) {
        return getTemplateItem(stack) != null && getTemplateData(stack) != null;
    }

    private static Result incomplete(FailureReason reason, Object... args) {
        return Result.incomplete(reason.message(args));
    }

    private static Result invalid(FailureReason reason, Object... args) {
        return Result.invalid(reason.message(args));
    }

    private static Component modeName(BlazeComposerMode mode) {
        return Component.translatable("create_enchantment_industry.gui.blaze_composer.mode." + mode.getSerializedName());
    }

    private static Component rarityName(DynamicHolder<LootRarity> rarity) {
        return Component.translatable(rarity.getId().toLanguageKey("rarity"))
                .withStyle(style -> rarity.isBound() ? style.withColor(rarity.get().color()) : style);
    }

    private static Result validateTemplateMode(AffixTemplateTier tier, boolean hyper) {
        if (tier.matchesHyperMode(hyper))
            return null;
        return hyper
                ? invalid(FailureReason.NORMAL_TEMPLATE_REQUIRES_NORMAL_MODE)
                : invalid(FailureReason.APOTHEOTIC_TEMPLATE_REQUIRES_HYPER_MODE);
    }

    private static Result validateExtractionLevel(AffixTemplateTier tier, boolean hyper, AffixTemplateData data) {
        if (hyper)
            return null;
        if (data.level() > Affix.MAX_LEVEL + EPSILON)
            return invalid(
                    FailureReason.OVERLIMIT_AFFIX_REQUIRES_HYPER_TEMPLATE,
                    AffixTemplateDisplay.formatLevel(data.level()),
                    AffixTemplateDisplay.formatLevel(Affix.MAX_LEVEL));
        if (tier == AffixTemplateTier.BRASS && data.level() > Affix.STANDARD_MAX_LEVEL + EPSILON)
            return invalid(
                    FailureReason.ADVANCED_AFFIX_REQUIRES_CRYSTAL_TEMPLATE,
                    AffixTemplateDisplay.formatLevel(data.level()),
                    AffixTemplateDisplay.formatLevel(Affix.STANDARD_MAX_LEVEL));
        return null;
    }

    private static boolean canTemplateHold(AffixTemplateItem item, AffixTemplateData data) {
        return data.level() <= maxLevel(item.tier(), data) + EPSILON;
    }

    private static float maxLevel(AffixTemplateTier tier, AffixTemplateData data) {
        return AffixComposingRules.INSTANCE.getMaxLevel(data, tier.getMaxLevel());
    }

    private static boolean canUpgrade(AffixTemplateData data, ItemStack stack) {
        return CEIAXConfig.server().affixes().allowLevelIndependentAffixUpgrade.get()
                || !data.toInstance(stack).isLevelIndependent();
    }

    private static AffixInstance firstAffix(ItemStack stack) {
        return AffixHelper.getAffixes(stack).values().stream()
                .filter(AffixInstance::isValid)
                .sorted(Comparator.comparing(instance -> instance.affix().getId()))
                .findFirst()
                .orElse(null);
    }

    private static void rebuildAffixName(ItemStack stack) {
        var affixes = AffixHelper.getAffixes(stack);
        if (affixes.isEmpty()) {
            stack.remove(Apoth.Components.AFFIX_NAME);
            stack.remove(Apoth.Components.RARITY);
            stack.remove(CEIAXDataComponents.OVERLIMIT_AFFIXES.get());
            return;
        }
        DynamicHolder<LootRarity> rarity = AffixHelper.getRarity(stack);
        if (!rarity.isBound()) {
            stack.remove(Apoth.Components.AFFIX_NAME);
            return;
        }
        List<Affix> nameList = new ArrayList<>(affixes.values().stream()
                .filter(AffixInstance::isValid)
                .sorted(Comparator.comparing(instance -> instance.affix().getId()))
                .map(AffixInstance::getAffix)
                .toList());
        if (nameList.isEmpty()) {
            stack.remove(Apoth.Components.AFFIX_NAME);
            return;
        }
        String key = nameList.size() > 1 ? "misc.apotheosis.affix_name.three" : "misc.apotheosis.affix_name.two";
        MutableComponent name = Component.translatable(key, nameList.get(0).getName(true), "", nameList.size() > 1 ? nameList.get(1).getName(false) : "")
                .withStyle(Style.EMPTY.withColor(rarity.get().color()).withItalic(false));
        AffixHelper.setName(stack, name);
        stack.remove(Apoth.Components.TOUCHED_BY_MALICE);
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static boolean nearlyEqual(float first, float second) {
        return Math.abs(first - second) <= EPSILON;
    }

    public enum Status {
        EMPTY_INPUT,
        INCOMPLETE_INPUT,
        INVALID,
        READY
    }

    public record Result(Status status, Component failure, ItemStack primaryOutput, ItemStack secondaryOutput, int cost, List<Component> outputDescriptions) {
        public static Result emptyInput() {
            return new Result(Status.EMPTY_INPUT, Component.empty(), ItemStack.EMPTY, ItemStack.EMPTY, 0, List.of());
        }

        public static Result incomplete(Component failure) {
            return new Result(Status.INCOMPLETE_INPUT, failure, ItemStack.EMPTY, ItemStack.EMPTY, 0, List.of());
        }

        public static Result invalid(Component failure) {
            return new Result(Status.INVALID, failure, ItemStack.EMPTY, ItemStack.EMPTY, 0, List.of());
        }

        public static Result ready(ItemStack primaryOutput, ItemStack secondaryOutput, int cost, Component... descriptions) {
            return new Result(Status.READY, Component.empty(), primaryOutput, secondaryOutput, cost, List.of(descriptions));
        }

        public boolean valid() {
            return status == Status.READY && cost > 0 && (!primaryOutput.isEmpty() || !secondaryOutput.isEmpty());
        }
    }

    private enum FailureReason {
        MISSING_AFFIXED_EQUIPMENT("missing_affixed_equipment"),
        MISSING_EQUIPMENT("missing_equipment"),
        MISSING_BLANK_TEMPLATE("missing_blank_template"),
        MISSING_FILLED_TEMPLATE("missing_filled_template"),
        MISSING_SECOND_FILLED_TEMPLATE("missing_second_filled_template"),
        REQUIRES_BLANK_TEMPLATE("requires_blank_template"),
        REQUIRES_FILLED_TEMPLATE("requires_filled_template"),
        FIRST_REQUIRES_FILLED_TEMPLATE("first_requires_filled_template"),
        SECOND_REQUIRES_FILLED_TEMPLATE("second_requires_filled_template"),
        NORMAL_TEMPLATE_REQUIRES_NORMAL_MODE("normal_template_requires_normal_mode"),
        APOTHEOTIC_TEMPLATE_REQUIRES_HYPER_MODE("apotheotic_template_requires_hyper_mode"),
        ADVANCED_AFFIX_REQUIRES_CRYSTAL_TEMPLATE("advanced_affix_requires_crystal_template"),
        OVERLIMIT_AFFIX_REQUIRES_HYPER_TEMPLATE("overlimit_affix_requires_hyper_template"),
        UNBOUND_TEMPLATE_DATA("unbound_template_data"),
        EQUIPMENT_HAS_NO_RARITY("equipment_has_no_rarity"),
        EQUIPMENT_HAS_NO_AFFIX("equipment_has_no_affix"),
        ITEM_HAS_NO_LOOT_CATEGORY("item_has_no_loot_category"),
        TEMPLATE_CANNOT_HOLD_LEVEL("template_cannot_hold_level"),
        AFFIX_DENIED_BY_RULE("affix_denied_by_rule"),
        RARITY_MISMATCH_DISALLOWED("rarity_mismatch_disallowed"),
        AFFIX_CANNOT_APPLY_TO_ITEM("affix_cannot_apply_to_item"),
        AFFIX_INCOMPATIBLE_WITH_EQUIPMENT("affix_incompatible_with_equipment"),
        TEMPLATE_AFFIX_MISMATCH("template_affix_mismatch"),
        TEMPLATE_RARITY_MISMATCH("template_rarity_mismatch"),
        LEVEL_INDEPENDENT_AFFIX("level_independent_affix"),
        WOULD_NOT_IMPROVE("would_not_improve"),
        ALREADY_AT_TEMPLATE_CAP("already_at_template_cap");

        private final String key;

        FailureReason(String key) {
            this.key = key;
        }

        public Component message(Object... args) {
            return Component.translatable("create_enchantment_industry.gui.goggles.blaze_composer.failure." + key, args);
        }
    }
}

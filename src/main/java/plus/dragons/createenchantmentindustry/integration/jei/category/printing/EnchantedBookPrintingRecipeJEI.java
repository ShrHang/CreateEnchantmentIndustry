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

package plus.dragons.createenchantmentindustry.integration.jei.category.printing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.fluids.FluidStack;
import plus.dragons.createdragonsplus.util.Pairs;
import plus.dragons.createenchantmentindustry.common.CEICommon;
import plus.dragons.createenchantmentindustry.common.fluids.experience.ExperienceHelper;
import plus.dragons.createenchantmentindustry.common.processing.enchanter.CEIEnchantmentHelper;
import plus.dragons.createenchantmentindustry.common.registry.CEIDataMaps;
import plus.dragons.createenchantmentindustry.common.registry.CEIEnchantments;
import plus.dragons.createenchantmentindustry.common.registry.CEIFluids;
import plus.dragons.createenchantmentindustry.config.CEIConfig;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class EnchantedBookPrintingRecipeJEI implements PrintingRecipeJEI {
    private static final int LEVELS_PER_PAGE = 100;
    public static final PrintingRecipeJEI.Type TYPE = PrintingRecipeJEI
            .register(CEICommon.asResource("enchanted_book"), EnchantedBookPrintingRecipeJEI::createCodec);
    private final ResourceLocation id;
    private final ResourceKey<Enchantment> enchantmentKey;
    private final int page;

    private EnchantedBookPrintingRecipeJEI(ResourceKey<Enchantment> enchantmentKey, int page) {
        var baseId = PrintingRecipeJEI.super.getRegistryName().withSuffix("/" +
                enchantmentKey.location().getNamespace() + "/" +
                enchantmentKey.location().getPath());
        this.id = page == 0 ? baseId : baseId.withSuffix("/page_" + page);
        this.enchantmentKey = enchantmentKey;
        this.page = page;
    }

    public static MapCodec<EnchantedBookPrintingRecipeJEI> createCodec(ICodecHelper codecHelper, IRecipeManager recipeManager) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("enchantment")
                                .forGetter((EnchantedBookPrintingRecipeJEI recipe) -> recipe.enchantmentKey.location()),
                        Codec.INT.optionalFieldOf("page", 0)
                                .forGetter(recipe -> recipe.page))
                .apply(instance, (enchantment, page) -> new EnchantedBookPrintingRecipeJEI(
                        ResourceKey.create(Registries.ENCHANTMENT, enchantment), page)));
    }

    public static List<PrintingRecipeJEI> listAll() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return List.of();
        return minecraft.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .listElements()
                .filter(enchantment -> !enchantment.is(CEIEnchantments.MOD_TAGS.printingDeny))
                .flatMap(enchantment -> enchantment.unwrapKey().stream()
                        .flatMap(key -> IntStream.range(0, getPageCount(enchantment))
                                .mapToObj(page -> (PrintingRecipeJEI) new EnchantedBookPrintingRecipeJEI(key, page))))
                .toList();
    }

    private static int getPageCount(Holder.Reference<Enchantment> enchantment) {
        int minLevel = enchantment.value().getMinLevel();
        int maxLevel = CEIEnchantmentHelper.maxLevel(enchantment);
        return Math.max(1, (maxLevel - minLevel) / LEVELS_PER_PAGE + 1);
    }

    private int getFirstLevel(Holder.Reference<Enchantment> enchantment) {
        return enchantment.value().getMinLevel() + page * LEVELS_PER_PAGE;
    }

    private int getLastLevel(Holder.Reference<Enchantment> enchantment) {
        return Math.min(getFirstLevel(enchantment) + LEVELS_PER_PAGE - 1,
                CEIEnchantmentHelper.maxLevel(enchantment));
    }

    private Optional<Holder.Reference<Enchantment>> resolveEnchantment() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return Optional.empty();
        return minecraft.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(enchantmentKey);
    }

    private static ItemStack createEnchantmentBook(Holder.Reference<Enchantment> enchantment, int level) {
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
    }

    private int getCost(Holder.Reference<Enchantment> enchantment, int level) {
        var customCost = enchantment.getData(CEIDataMaps.PRINTING_ENCHANTED_BOOK_COST);
        if (customCost != null) {
            for (var pair : customCost) {
                if (pair.level() == level)
                    return pair.value();
            }
        }
        return (int) (CEIEnchantmentHelper.getEnchantmentCost(enchantment, level)
                * CEIConfig.fluids().printingEnchantedBookCostMultiplier.get());
    }

    private void addAllLevels(IRecipeSlotBuilder slot) {
        resolveEnchantment().ifPresent(enchantment -> {
            for (int level = getFirstLevel(enchantment); level <= getLastLevel(enchantment); level++) {
                slot.addItemStack(createEnchantmentBook(enchantment, level));
            }
        });
    }

    @Override
    public void setBase(IRecipeSlotBuilder slot) {
        slot.addItemLike(Items.BOOK);
    }

    @Override
    public void setTemplate(IRecipeSlotBuilder slot) {
        addAllLevels(slot);
    }

    @Override
    public void setFluid(IRecipeSlotBuilder slot) {
        resolveEnchantment().ifPresent(enchantment -> {
            for (int level = getFirstLevel(enchantment); level <= getLastLevel(enchantment); level++) {
                int cost = getCost(enchantment, level);
                slot.addFluidStack(CEIFluids.EXPERIENCE.get(), cost);
                CEIDataMaps.getSourceFluidEntries(CEIDataMaps.FLUID_UNIT_EXPERIENCE)
                        .forEach(Pairs.accept((fluid, unit) -> slot.addFluidStack(fluid, (long) unit * cost)));
            }
        });
    }

    @Override
    public void setOutput(IRecipeSlotBuilder slot) {
        addAllLevels(slot);
    }

    @Override
    public Type getType() {
        return TYPE;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return id;
    }

    @Override
    public void onDisplayedIngredientsUpdate(IRecipeSlotDrawable baseSlot, IRecipeSlotDrawable templateSlot, IRecipeSlotDrawable fluidSlot, IRecipeSlotDrawable outputSlot, IFocusGroup focuses) {
        resolveEnchantment().ifPresent(enchantment -> {
            boolean hasOutputFocus = focuses.getFocuses(RecipeIngredientRole.OUTPUT).findAny().isPresent();
            ItemStack displayed = (hasOutputFocus ? outputSlot : templateSlot).getDisplayedItemStack().orElse(ItemStack.EMPTY);

            int level = EnchantmentHelper.getEnchantmentsForCrafting(displayed).getLevel(enchantment);
            if (level <= 0) level = getFirstLevel(enchantment);

            var stack = createEnchantmentBook(enchantment, level);
            if (hasOutputFocus) templateSlot.createDisplayOverrides().addItemStack(stack);
            else outputSlot.createDisplayOverrides().addItemStack(stack);

            int cost = getCost(enchantment, level);
            var displayedFluid = fluidSlot.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK)
                    .orElse(new FluidStack(CEIFluids.EXPERIENCE.get(), cost));

            fluidSlot.createDisplayOverrides().addIngredient(NeoForgeTypes.FLUID_STACK,
                    displayedFluid.copyWithAmount(ExperienceHelper.getFluidFromExperience(displayedFluid, cost)));
        });
    }
}

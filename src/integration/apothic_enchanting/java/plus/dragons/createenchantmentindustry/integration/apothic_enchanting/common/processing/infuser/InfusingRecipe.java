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

package plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.processing.infuser;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import dev.shadowsoffire.apothic_enchanting.util.MiscUtil;
import java.util.*;
import net.createmod.catnip.data.Iterate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.registry.CEIAFluids;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.registry.CEIARecipes;

public class InfusingRecipe extends ProcessingRecipe<SingleRecipeInput, InfusingRecipeParams> {
    public InfusingRecipe(InfusingRecipeParams params) {
        super(CEIARecipes.INFUSING, params);
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 1;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 1;
    }

    public static boolean match(InfuserBlockEntity infuser, BasinBlockEntity basin, Recipe<?> r) {
        FilteringBehaviour filter = basin.getFilter();
        if (filter == null)
            return false;

        InfusingRecipe recipe = (InfusingRecipe) r;
        if (!recipe.params.stats.qualifid(infuser.infusionStats))
            return false;

        boolean filterTest = filter.test(recipe.getResultItem(basin.getLevel()
                .registryAccess()));

        if (recipe.getRollableResults()
                .isEmpty()
                && !recipe.getFluidResults()
                        .isEmpty())
            filterTest = filter.test(recipe.getFluidResults()
                    .get(0));

        if (!filterTest)
            return false;

        return apply(infuser, basin, recipe, true);
    }

    public static boolean apply(InfuserBlockEntity infuser, BasinBlockEntity basin, Recipe<?> recipe) {
        return apply(infuser, basin, recipe, false);
    }

    private static boolean apply(InfuserBlockEntity infuser, BasinBlockEntity basin, Recipe<?> r, boolean test) {
        var level = infuser.getLevel();
        IItemHandler availableItems = level.getCapability(Capabilities.ItemHandler.BLOCK, basin.getBlockPos(), null);
        IFluidHandler availableFluids = level.getCapability(Capabilities.FluidHandler.BLOCK, basin.getBlockPos(), null);
        IFluidHandler infusingIngredient = level.getCapability(Capabilities.FluidHandler.BLOCK, infuser.getBlockPos(), null);

        if (availableItems == null || availableFluids == null || infusingIngredient == null)
            return false;

        if (!infusingIngredient.getFluidInTank(0).is(CEIAFluids.MOD_TAGS.infusing_ingredients))
            return false;
        InfusingRecipe recipe = (InfusingRecipe) r;

        var requireAmount = MiscUtil.getExpCostForSlot((int) recipe.params.stats.eterna(), 0);

        if (infusingIngredient.getFluidInTank(0).getAmount() < requireAmount)
            return false;

        List<ItemStack> recipeOutputItems = new ArrayList<>();
        List<FluidStack> recipeOutputFluids = new ArrayList<>();

        for (boolean simulate : Iterate.trueAndFalse) {

            if (!simulate && test)
                return true;

            int[] extractedItemsFromSlot = new int[availableItems.getSlots()];

            if (!recipe.ingredients.isEmpty()) {
                Ingredient ingredient = recipe.ingredients.getFirst();

                for (int slot = 0; slot < availableItems.getSlots(); slot++) {
                    if (simulate && availableItems.getStackInSlot(slot)
                            .getCount() <= extractedItemsFromSlot[slot])
                        continue;
                    ItemStack extracted = availableItems.extractItem(slot, 1, true);
                    if (!ingredient.test(extracted))
                        continue;
                    if (!simulate)
                        availableItems.extractItem(slot, 1, false);
                    extractedItemsFromSlot[slot]++;
                }

                if (Arrays.stream(extractedItemsFromSlot).reduce(Integer::sum).getAsInt() == 0)
                    return false;
            } else {
                SizedFluidIngredient fluidIngredient = recipe.fluidIngredients.getFirst();

                int[] extractedFluidsFromTank = new int[availableFluids.getTanks()];
                boolean fluidsAffected = false;
                int amountRequired = fluidIngredient.amount();

                for (int tank = 0; tank < availableFluids.getTanks(); tank++) {
                    FluidStack fluidStack = availableFluids.getFluidInTank(tank);
                    if (!fluidIngredient.test(fluidStack))
                        continue;
                    int drainedAmount = Math.min(amountRequired, fluidStack.getAmount());
                    amountRequired -= drainedAmount;
                    extractedFluidsFromTank[tank] += drainedAmount;
                    if (amountRequired == 0)
                        break;
                }

                if (Arrays.stream(extractedFluidsFromTank).reduce(Integer::sum).getAsInt() < fluidIngredient.amount())
                    return false;

                if (!simulate) {
                    for (int tank = 0; tank < availableFluids.getTanks(); tank++) {
                        FluidStack fluidStack = availableFluids.getFluidInTank(tank);
                        fluidStack.shrink(extractedFluidsFromTank[tank]);
                    }
                    fluidsAffected = true;
                }

                if (fluidsAffected) {
                    basin.getBehaviour(SmartFluidTankBehaviour.INPUT)
                            .forEach(SmartFluidTankBehaviour.TankSegment::onFluidStackChanged);
                    basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT)
                            .forEach(SmartFluidTankBehaviour.TankSegment::onFluidStackChanged);
                }
            }

            if (simulate) {
                recipeOutputItems.addAll(recipe.rollResults(level.random));
                for (FluidStack fluidStack : recipe.getFluidResults())
                    if (!fluidStack.isEmpty())
                        recipeOutputFluids.add(fluidStack);
            } else {
                infusingIngredient.drain(requireAmount, IFluidHandler.FluidAction.EXECUTE);
            }

            if (!basin.acceptOutputs(recipeOutputItems, recipeOutputFluids, simulate))
                return false;
        }

        return true;
    }

    @Override
    public boolean matches(SingleRecipeInput basin, Level level) {
        return false;
    }

    public static class Builder extends ProcessingRecipeBuilder<InfusingRecipeParams, InfusingRecipe, InfusingRecipe.Builder> {
        public Builder(ResourceLocation recipeId, InfusionStats stats) {
            super(InfusingRecipe::new, recipeId);
            this.params.stats = stats;
        }

        @Override
        protected InfusingRecipeParams createParams() {
            return new InfusingRecipeParams();
        }

        @Override
        public InfusingRecipe.Builder self() {
            return this;
        }
    }

    public static class Serializer<R extends InfusingRecipe> implements RecipeSerializer<R> {
        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        public Serializer(ProcessingRecipe.Factory<InfusingRecipeParams, R> factory) {
            this.codec = ProcessingRecipe.codec(factory, InfusingRecipeParams.CODEC);
            this.streamCodec = ProcessingRecipe.streamCodec(factory, InfusingRecipeParams.STREAM_CODEC);
        }

        @Override
        public MapCodec<R> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
            return streamCodec;
        }
    }

    public static RecipeHolder<InfusingRecipe> convertInfusionRecipe(RecipeHolder<?> recipe) {
        InfusionRecipe infusionRecipe = (InfusionRecipe) recipe.value();
        var requirements = infusionRecipe.getRequirements();
        var stats = new InfusionStats(requirements.eterna(), requirements.quanta(), requirements.arcana());
        InfusingRecipe infusingRecipe = new InfusingRecipe.Builder(recipe.id(), stats).withItemIngredients(infusionRecipe.getInput())
                .withSingleItemOutput(infusionRecipe.getOutput())
                .build();
        return new RecipeHolder<>(recipe.id(), infusingRecipe);
    }
}

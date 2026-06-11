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

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import java.util.function.Consumer;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;
import plus.dragons.createdragonsplus.common.advancements.AdvancementBehaviour;
import plus.dragons.createdragonsplus.common.fluids.tank.ConfigurableFluidTank;
import plus.dragons.createdragonsplus.common.fluids.tank.FluidTankBehaviour;
import plus.dragons.createdragonsplus.common.processing.blaze.BlazeBlockEntity;
import plus.dragons.createdragonsplus.util.FieldsNullabilityUnknownByDefault;
import plus.dragons.createenchantmentindustry.integration.apotheosis.client.registry.CEIAXPartialModels;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.registry.CEIAXFluids;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.registry.CEIAXItems;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.registry.CEIAXStats;
import plus.dragons.createenchantmentindustry.integration.apotheosis.config.CEIAXConfig;
import plus.dragons.createenchantmentindustry.util.CEILang;

@FieldsNullabilityUnknownByDefault
public class BlazeComposerBlockEntity extends BlazeBlockEntity implements Clearable, IHaveGoggleInformation {
    protected int processingTime = -1;
    protected BlazeComposerMode mode = BlazeComposerMode.EXTRACT;
    protected boolean hyper;
    protected boolean hyperUnlocked;
    protected final BlazeComposerInventory inventory;
    protected BlazeComposerModeBehaviour modeSelector;
    protected FluidTankBehaviour tanks;
    protected IFluidHandler fuelHandler;
    protected AdvancementBehaviour advancement;

    public BlazeComposerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.inventory = new BlazeComposerInventory(this);
    }

    public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
        if ((side == Direction.DOWN || side == null) && !isRemoved())
            return fuelHandler;
        return null;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        modeSelector = new BlazeComposerModeBehaviour(this, new ModeTransform());
        tanks = new FluidTankBehaviour(this, List.of(this::createNormalTank, this::createHyperTank), false);
        fuelHandler = new HyperFuelFluidHandler(this::getNormalTank, this::getHyperTank, this::canFillHyperTank);
        advancement = new AdvancementBehaviour(this);
        behaviours.add(modeSelector);
        behaviours.add(tanks);
        behaviours.add(advancement);
    }

    protected ConfigurableFluidTank createNormalTank(Consumer<FluidStack> fluidUpdateCallback) {
        return new ConfigurableFluidTank(CEIAXConfig.server().affixes().blazeComposerFluidCapacity.get(), fluidUpdateCallback)
                .allowInsertion(fluidStack -> fluidStack.is(CEIAXFluids.APOTHEOTIC_ESSENCE));
    }

    protected ConfigurableFluidTank createHyperTank(Consumer<FluidStack> fluidUpdateCallback) {
        return new ConfigurableFluidTank(CEIAXConfig.server().affixes().blazeComposerHyperFluidCapacity.get(), fluidUpdateCallback)
                .allowInsertion(fluidStack -> fluidStack.is(CEIAXFluids.APOTHEOTIC_ESSENCE));
    }

    @Override
    public boolean isActive() {
        return processingTime > 0;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    @Override
    public HeatLevel getHeatLevel() {
        if (getHyperEssence() > 0)
            return HeatLevel.SEETHING;
        return getNormalEssence() > 0 ? HeatLevel.KINDLED : HeatLevel.SMOULDERING;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected @Nullable PartialModel getHatModel(HeatLevel heatLevel) {
        return heatLevel.isAtLeast(HeatLevel.FADING)
                ? CEIAXPartialModels.BLAZE_COMPOSER_HAT
                : CEIAXPartialModels.BLAZE_COMPOSER_HAT_SMALL;
    }

    @Override
    public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("ProcessingTime", processingTime);
        compound.putInt("Mode", mode.ordinal());
        compound.putBoolean("HyperUnlocked", hyperUnlocked);
        compound.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        processingTime = compound.contains("ProcessingTime") ? compound.getInt("ProcessingTime") : -1;
        mode = BlazeComposerMode.BY_ID.apply(compound.getInt("Mode"));
        hyperUnlocked = compound.getBoolean("HyperUnlocked");
        inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level != null)
            ItemHelper.dropContents(level, worldPosition, inventory);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null)
            return;
        if (level.isClientSide() && !isVirtual())
            return;
        boolean hyper = isHyper();
        if (this.hyper != hyper) {
            this.hyper = hyper;
            processingTime = -1;
            inventory.updateResult();
            notifyUpdate();
        }
        int cost = inventory.getEssenceCost();
        if (cost > 0 && consumeEssence(cost, hyper, true)) {
            if (processingTime < 0) {
                processingTime = processingTime();
                notifyUpdate();
                return;
            }
            if (processingTime > 0) {
                processingTime--;
                notifyUpdate();
                return;
            }
            consumeEssence(cost, hyper, false);
            processingTime = -1;
            inventory.applyResult();
            advancement.awardStat(CEIAXStats.COMPOSE_AFFIX.get(), 1);
            notifyUpdate();
            level.playSound(null, worldPosition, SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 0.8F, 0.9F + 0.2F * level.random.nextFloat());
            level.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.5F, 0.7F + 0.2F * level.random.nextFloat());
        } else if (processingTime != -1) {
            processingTime = -1;
            notifyUpdate();
        }
    }

    public boolean consumeEssence(int amount, boolean hyper, boolean simulate) {
        var fluid = new FluidStack(CEIAXFluids.APOTHEOTIC_ESSENCE, amount);
        var tank = hyper ? getHyperTank() : getNormalTank();
        var drained = tank.drain(fluid, FluidAction.SIMULATE);
        if (drained.getAmount() != amount)
            return false;
        if (!simulate)
            tank.drain(fluid, FluidAction.EXECUTE);
        return true;
    }

    public SmartFluidTank getNormalTank() {
        return tanks.getHandlers()[0];
    }

    public SmartFluidTank getHyperTank() {
        return tanks.getHandlers()[1];
    }

    public int getNormalEssence() {
        return getNormalTank().getFluidAmount();
    }

    public int getHyperEssence() {
        return getHyperTank().getFluidAmount();
    }

    public boolean isHyper() {
        return getHyperEssence() > 0;
    }

    public boolean isHyperUnlocked() {
        return hyperUnlocked;
    }

    public boolean canFillHyperTank() {
        return hyperUnlocked || getHyperEssence() > 0;
    }

    public BlazeComposerMode getMode() {
        return mode;
    }

    public void setMode(BlazeComposerMode mode) {
        if (this.mode == mode)
            return;
        this.mode = mode;
        processingTime = -1;
        inventory.updateResult();
        notifyUpdate();
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        var original = stack;
        if (inventory.hasRemainingOutput())
            return stack;
        stack = unlockHyper(stack, simulate);
        if (!ItemStack.isSameItemSameComponents(original, stack) || original.getCount() != stack.getCount())
            return stack;
        if (isHyperActivator(stack))
            return stack;
        if (!stack.isEmpty())
            stack = inventory.insertItem(0, stack, simulate);
        if (!stack.isEmpty())
            stack = inventory.insertItem(1, stack, simulate);
        if (!simulate && (original.getCount() != stack.getCount() || !ItemStack.isSameItemSameComponents(original, stack))) {
            inventory.updateResult();
            notifyUpdate();
        }
        return stack;
    }

    public ItemStack extractItem(boolean simulate) {
        for (int i = inventory.getSlots() - 1; i >= 0; i--) {
            ItemStack extracted = inventory.extractItem(i, 1, simulate);
            if (!extracted.isEmpty()) {
                if (!simulate && i < 2) {
                    inventory.updateResult();
                    notifyUpdate();
                }
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean isHyperActivator(ItemStack stack) {
        return stack.is(CEIAXItems.MOD_TAGS.blazeComposerHyperActivators);
    }

    public boolean canUnlockHyper(ItemStack stack) {
        return !stack.isEmpty()
                && isHyperActivator(stack)
                && !hyperUnlocked
                && getHyperEssence() == 0
                && getNormalEssence() >= getNormalTank().getCapacity();
    }

    public ItemStack unlockHyper(ItemStack stack, boolean simulate) {
        if (!canUnlockHyper(stack))
            return stack;
        ItemStack remainder = stack.copy();
        remainder.shrink(1);
        if (!simulate) {
            hyperUnlocked = true;
            processingTime = -1;
            inventory.updateResult();
            notifyUpdate();
            if (level != null && !level.isClientSide()) {
                level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.35F, 1.6F + 0.2F * level.random.nextFloat());
                level.playSound(null, worldPosition, SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.BLOCKS, 0.45F, 0.75F + 0.2F * level.random.nextFloat());
            }
        }
        return remainder;
    }

    protected int processingTime() {
        return CEIAXConfig.server().affixes().blazeComposerProcessingTime.get();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");
        CreateLang.translate("gui.goggles.fluid_container")
                .forGoggles(tooltip);
        addTankTooltip(tooltip, mb, "gui.goggles.blaze_composer.normal_essence", getNormalTank(), ChatFormatting.GOLD);
        addTankTooltip(tooltip, mb, "gui.goggles.blaze_composer.hyper_essence", getHyperTank(), ChatFormatting.BLUE);
        boolean hyper = isHyper();
        ChatFormatting essenceStyle = hyper ? ChatFormatting.BLUE : ChatFormatting.GOLD;
        CEILang.translate(
                "gui.goggles.blaze_composer.hyper_mode",
                CEILang.translate("gui.blaze_composer.hyper_mode." + (isHyper()? "hyper": "normal")).style(essenceStyle))
                .forGoggles(tooltip);
        CEILang.translate("gui.goggles.blaze_composer.mode", CEILang.translate("gui.blaze_composer.mode." + mode.getSerializedName()).style(ChatFormatting.AQUA))
                .forGoggles(tooltip);
        if (inventory.hasRemainingOutput()) {
            CEILang.translate("gui.goggles.blaze_composer.output_blocked").style(ChatFormatting.YELLOW).forGoggles(tooltip);
            return true;
        }
        var result = inventory.getLastResult();
        if (result.status() == AffixTemplateOps.Status.EMPTY_INPUT) {
            addModeHelp(tooltip);
        } else if (result.status() == AffixTemplateOps.Status.INCOMPLETE_INPUT) {
            addModeHelp(tooltip);
            CEILang.builder().add(result.failure().copy()).style(ChatFormatting.YELLOW).forGoggles(tooltip, 1);
        } else if (result.status() == AffixTemplateOps.Status.INVALID) {
            CEILang.builder().add(result.failure().copy()).style(ChatFormatting.RED).forGoggles(tooltip);
        } else if (result.valid()) {
            int cost = result.cost();
            CEILang.translate("gui.goggles.blaze_composer.cost", CEILang.number(cost).add(mb).style(essenceStyle))
                    .forGoggles(tooltip);
            CEILang.translate("gui.goggles.blaze_composer.result").forGoggles(tooltip);
            for (Component description : result.outputDescriptions()) {
                CEILang.builder().add(description.copy()).forGoggles(tooltip, 1);
            }
            int essence = hyper ? getHyperEssence() : getNormalEssence();
            if (essence < cost) {
                CEILang.translate(
                        hyper ? "gui.goggles.blaze_composer.insufficient_hyper_essence" : "gui.goggles.blaze_composer.insufficient_essence",
                        CEILang.number(essence).add(mb).style(essenceStyle),
                        CEILang.number(cost).add(mb).style(essenceStyle))
                        .style(ChatFormatting.RED)
                        .forGoggles(tooltip);
            }
        }
        return true;
    }

    private void addTankTooltip(List<Component> tooltip, LangBuilder mb, String labelKey, SmartFluidTank tank, ChatFormatting amountStyle) {
        CEILang.translate(labelKey).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CreateLang.builder()
                .add(CreateLang.number(tank.getFluidAmount()).add(mb).style(amountStyle))
                .text(ChatFormatting.GRAY, " / ")
                .add(CreateLang.number(tank.getCapacity()).add(mb).style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 2);
    }

    private void addModeHelp(List<Component> tooltip) {
        CEILang.translate("gui.goggles.blaze_composer.mode_help." + mode.getSerializedName())
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
        CEILang.translate("gui.goggles.blaze_composer.requires").forGoggles(tooltip);
        CEILang.translate(
                "gui.goggles.blaze_composer.requires.first",
                CEILang.translate("gui.goggles.blaze_composer.requires." + mode.getSerializedName() + ".first").component())
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
        CEILang.translate(
                "gui.goggles.blaze_composer.requires.second",
                CEILang.translate("gui.goggles.blaze_composer.requires." + mode.getSerializedName() + ".second").component())
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

    private static class ModeTransform extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 13.5);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack poseStack) {
            float yRot = AngleHelper.horizontalAngle(getSide()) + 180;
            TransformStack.of(poseStack).rotateYDegrees(yRot);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal();
        }
    }
}

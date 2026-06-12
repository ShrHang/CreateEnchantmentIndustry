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

import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.HOLD;
import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.PASS;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import java.util.*;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.kinetics.belt.lowerProcessingAppliance.LowerBeltProcessingBehaviour;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.registry.CEIAXFluids;
import plus.dragons.createenchantmentindustry.util.CEILang;

public class GemCutterBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {
    public static final int UNIT_PROCESSING_TIME = 200;
    public int processingTicks = -1;
    public boolean powered;
    public float chargingPercentage;

    public GemCutterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        BeltProcessingBehaviour processing = new LowerBeltProcessingBehaviour(this)
                .whenItemEnters(this::onItemEnters)
                .whileItemHeld(this::onItemHeld);
        behaviours.add(processing);
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            if (powered && chargingPercentage < 1) {
                chargingPercentage = Math.min(chargingPercentage + 0.025f, 1);
            } else if (!powered && chargingPercentage > 0) {
                chargingPercentage = Math.max(0, chargingPercentage - 0.025f);
            }
        }
        if (!level.isClientSide && !isVirtual()) {
            var fluidTank = getExternalFluidTank();
            if (fluidTank.isEmpty()) {
                if (powered) {
                    powered = false;
                    notifyUpdate();
                }
            } else {
                var tank = fluidTank.get().getTankInventory();
                if (tank.getFluid().is(CEIAXFluids.CRYSTAL_ESSENCE)) {
                    if (!powered) {
                        powered = true;
                        notifyUpdate();
                    }
                } else {
                    if (powered) {
                        powered = false;
                        notifyUpdate();
                    }
                }
            }
        }
        if (processingTicks >= 0) {
            if (powered) {
                processingTicks--;
                if (level.isClientSide && processingTicks > 25) {
                    spawnParticles();
                }
            } else if (processingTicks != -1) {
                processingTicks = -1;
                notifyUpdate();
            }
        }
    }

    private Optional<FluidTankBlockEntity> getExternalFluidTank() {
        assert level != null;
        var be = level.getBlockEntity(worldPosition.below(2));
        if (be instanceof FluidTankBlockEntity tank) return Optional.of(tank.getControllerBE());
        else return Optional.empty();
    }

    public BeltProcessingBehaviour.ProcessingResult onItemEnters(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        Level level = this.level;
        assert level != null;

        if (handler.blockEntity.isVirtual())
            return PASS;

        if (!isUpgradableGem(transported.stack))
            return PASS;

        return HOLD;
    }

    public BeltProcessingBehaviour.ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        Level level = this.level;
        assert level != null;
        if (processingTicks != -1 && processingTicks != 25)
            return HOLD;
        var context = getCuttingContext(transported.stack);
        if (context.status() == CuttingStatus.ALREADY_PERFECT)
            return PASS;
        if (context.status() != CuttingStatus.READY)
            return HOLD;

        if (processingTicks == -1) {
            processingTicks = UNIT_PROCESSING_TIME;
            notifyUpdate();
            return HOLD;
        }

        TransportedItemStack result = transported.copy();
        result.clearFanProcessingData();
        TransportedItemStack remains = null;
        if (result.stack.getCount() > 1) {
            remains = transported.copy();
            remains.stack.shrink(1);
            result.stack.setCount(1);
        }
        GemItem.setPurity(result.stack, context.to());
        handler.handleProcessingOnItem(transported, TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(List.of(result), remains));
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.AMETHYST_CLUSTER_HIT,
                SoundSource.BLOCKS, 0.75f, .9f + 0.2f * level.random.nextFloat());
        context.tank().get().getTankInventory().drain(context.cost(), IFluidHandler.FluidAction.EXECUTE);
        notifyUpdate();
        return HOLD;
    }

    private CuttingContext getCuttingContext(ItemStack stack) {
        var fluidTank = getExternalFluidTank();
        if (fluidTank.isEmpty())
            return CuttingContext.noTank();
        var tank = fluidTank.get().getTankInventory();
        if (tank.isEmpty())
            return CuttingContext.withTank(CuttingStatus.EMPTY_TANK, fluidTank);
        if (!tank.getFluid().is(CEIAXFluids.CRYSTAL_ESSENCE))
            return CuttingContext.withTank(CuttingStatus.WRONG_FLUID, fluidTank);
        if (!stack.is(Apoth.Items.GEM))
            return CuttingContext.withTank(CuttingStatus.NOT_A_GEM, fluidTank);
        var from = GemItem.getPurity(stack);
        if (!GemCutting.canCut(from))
            return CuttingContext.withGem(CuttingStatus.ALREADY_PERFECT, fluidTank, from, from, 0);
        var to = GemCutting.resultPurity(from);
        int cost = GemCutting.getCutCost(from);
        if (cost > tank.getCapacity())
            return CuttingContext.withGem(CuttingStatus.TANK_TOO_SMALL, fluidTank, from, to, cost);
        if (cost > tank.getFluidAmount())
            return CuttingContext.withGem(CuttingStatus.INSUFFICIENT_ESSENCE, fluidTank, from, to, cost);
        return CuttingContext.withGem(CuttingStatus.READY, fluidTank, from, to, cost);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().expandTowards(0, -1, 0);
    }

    private void spawnParticles() {
        ParticleOptions data = ParticleTypes.FALLING_OBSIDIAN_TEAR;
        Vec3 center = VecHelper.getCenterOf(worldPosition);
        for (int i = 0; i < 5; i++) {
            var c = VecHelper.offsetRandomly(center, level.random, 3 / 16f);
            level.addParticle(data, c.x, center.y, c.z, 0, -20, 0);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("ProcessingTicks", processingTicks);
        tag.putBoolean("Powered", powered);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        processingTicks = tag.contains("ProcessingTicks") ? tag.getInt("ProcessingTicks") : -1;
        if (processingTicks == 0)
            processingTicks = -1;
        powered = tag.getBoolean("Powered");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CEILang.translate("gui.goggles.gem_cutter").forGoggles(tooltip);
        addTankTooltip(tooltip);
        if (processingTicks > 0) {
            int progress = Math.round((UNIT_PROCESSING_TIME - processingTicks) * 100F / UNIT_PROCESSING_TIME);
            CEILang.translate("gui.goggles.gem_cutter.processing", CEILang.number(progress).text("%").component())
                    .style(ChatFormatting.GREEN)
                    .forGoggles(tooltip);
        } else {
            CEILang.translate("gui.goggles.gem_cutter.waiting")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
        }
        addCostTooltip(tooltip);
        return true;
    }

    private void addTankTooltip(List<Component> tooltip) {
        var fluidTank = getExternalFluidTank();
        if (fluidTank.isEmpty()) {
            CEILang.translate("gui.goggles.gem_cutter.missing_tank")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
            return;
        }
        var tank = fluidTank.get().getTankInventory();
        CEILang.translate("gui.goggles.gem_cutter.tank")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        if (tank.isEmpty()) {
            CEILang.translate("gui.goggles.gem_cutter.empty_tank")
                    .style(ChatFormatting.YELLOW)
                    .forGoggles(tooltip, 1);
            return;
        }
        var fluid = tank.getFluid();
        var amount = amount(tank.getFluidAmount(), tank.getCapacity());
        CEILang.builder()
                .add(fluid.getHoverName())
                .text(" ")
                .add(amount)
                .style(fluid.is(CEIAXFluids.CRYSTAL_ESSENCE) ? ChatFormatting.GREEN : ChatFormatting.RED)
                .forGoggles(tooltip, 1);
        if (!fluid.is(CEIAXFluids.CRYSTAL_ESSENCE)) {
            CEILang.translate("gui.goggles.gem_cutter.wrong_fluid")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip, 1);
        }
        int maxCost = maxCutCost();
        if (tank.getCapacity() < maxCost) {
            CEILang.translate("gui.goggles.gem_cutter.max_cost_tank_too_small", amount(maxCost, tank.getCapacity()).component())
                    .style(ChatFormatting.YELLOW)
                    .forGoggles(tooltip, 1);
        }
    }

    private void addCostTooltip(List<Component> tooltip) {
        CEILang.translate("gui.goggles.gem_cutter.costs")
                .forGoggles(tooltip);
        addCostLine(tooltip, Purity.CRACKED);
        addCostLine(tooltip, Purity.CHIPPED);
        addCostLine(tooltip, Purity.FLAWED);
        addCostLine(tooltip, Purity.NORMAL);
        addCostLine(tooltip, Purity.FLAWLESS);
    }

    private void addCostLine(List<Component> tooltip, Purity from) {
        CEILang.translate(
                "gui.goggles.gem_cutter.cost",
                from.toComponent(),
                GemCutting.resultPurity(from).toComponent(),
                amount(GemCutting.getCutCost(from)).component())
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
    }

    private static int maxCutCost() {
        int max = 0;
        for (var purity : Purity.values()) {
            if (GemCutting.canCut(purity))
                max = Math.max(max, GemCutting.getCutCost(purity));
        }
        return max;
    }

    private static LangBuilder amount(int amount) {
        return CEILang.number(amount).text(" mB");
    }

    private static LangBuilder amount(int amount, int capacity) {
        return amount(amount).text(" / ").add(amount(capacity));
    }

    public static boolean isUpgradableGem(ItemStack stack) {
        if (!stack.is(Apoth.Items.GEM))
            return false;
        return GemItem.getPurity(stack) != Purity.PERFECT;
    }

    private enum CuttingStatus {
        MISSING_TANK,
        EMPTY_TANK,
        WRONG_FLUID,
        NOT_A_GEM,
        ALREADY_PERFECT,
        TANK_TOO_SMALL,
        INSUFFICIENT_ESSENCE,
        READY
    }

    private record CuttingContext(
            CuttingStatus status,
            Optional<FluidTankBlockEntity> tank,
            Purity from,
            Purity to,
            int cost) {
        private static CuttingContext noTank() {
            return new CuttingContext(CuttingStatus.MISSING_TANK, Optional.empty(), Purity.CRACKED, Purity.CRACKED, 0);
        }

        private static CuttingContext withTank(CuttingStatus status, Optional<FluidTankBlockEntity> tank) {
            return new CuttingContext(status, tank, Purity.CRACKED, Purity.CRACKED, 0);
        }

        private static CuttingContext withGem(CuttingStatus status, Optional<FluidTankBlockEntity> tank, Purity from, Purity to, int cost) {
            return new CuttingContext(status, tank, from, to, cost);
        }
    }
}

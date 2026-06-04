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

package plus.dragons.createenchantmentindustry.integration.apotheosis.common.processing.affix.affixEnhancer;

import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.HOLD;
import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.PASS;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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

public class AffixAugmentorBlockEntity extends KineticBlockEntity {
    public static final Map<Purity, Integer> PURITY_COST = new HashMap<>();
    public static final int UNIT_PROCESSING_TIME = 200;
    public int processingTicks;
    public boolean powered;
    public float chargingPercentage;

    public AffixAugmentorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
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
                if (tank.getFluid().is(CEIAXFluids.APOTHEOTIC_ESSENCE)) {
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
            } else {
                if (processingTicks != 0) {
                    processingTicks = 0;
                }
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

        if (!hasUpgradableAffix(transported.stack))
            return PASS;

        return HOLD;
    }

    public BeltProcessingBehaviour.ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        Level level = this.level;
        assert level != null;
        if (processingTicks != -1 && processingTicks != 25)
            return HOLD;
        var fluidTank = getExternalFluidTank();
        if (fluidTank.isEmpty())
            return HOLD;
        var tank = fluidTank.get().getTankInventory();
        if (!tank.getFluid().is(CEIAXFluids.APOTHEOTIC_ESSENCE))
            return HOLD;

        if (AffixHelper.streamAffixes(transported.stack).noneMatch(entry -> entry.level() < Affix.STANDARD_MAX_LEVEL))
            return PASS;

        var cost = AffixAugmenting.getCost();
        if (cost > tank.getCapacity() || cost > tank.getFluidAmount())
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
        var upgradableAffix = AffixHelper.streamAffixes(transported.stack).filter(entry -> entry.level() < Affix.STANDARD_MAX_LEVEL).findFirst();
        AffixHelper.applyAffix(result.stack, upgradableAffix.get().withNewLevel(Math.min(upgradableAffix.get().level() + 0.25F, Affix.STANDARD_MAX_LEVEL)));
        handler.handleProcessingOnItem(transported, TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(List.of(result), remains));
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 0.8f, .9f + 0.2f * level.random.nextFloat());
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.BLOCKS, 0.24f, .72f + 0.2f * level.random.nextFloat());
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.32f, .35f + 0.7f * level.random.nextFloat());
        tank.drain(cost, IFluidHandler.FluidAction.EXECUTE);
        notifyUpdate();
        return HOLD;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().expandTowards(0, -1, 0);
    }

    private void spawnParticles() {
        ParticleOptions data = ParticleTypes.ENCHANT;
        Vec3 center = VecHelper.getCenterOf(worldPosition).add(0, -3.5 / 16f, 0);
        for (int i = 0; i < 3; i++) {
            var c = VecHelper.offsetRandomly(center, level.random, 1 / 16f);
            level.addParticle(data, c.x, center.y, c.z, 0, 0, 0);
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
        processingTicks = tag.getInt("ProcessingTicks");
        powered = tag.getBoolean("Powered");
    }

    public static boolean hasUpgradableAffix(ItemStack stack) {
        var affix = AffixHelper.getAffixes(stack);
        if (affix == null)
            return false;

        if (affix.isEmpty())
            return false;

        return AffixHelper.streamAffixes(stack).anyMatch(instance -> instance.level() < Affix.STANDARD_MAX_LEVEL);
    }
}

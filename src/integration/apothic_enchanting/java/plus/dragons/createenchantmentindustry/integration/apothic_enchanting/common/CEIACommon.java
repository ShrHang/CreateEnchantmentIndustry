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

package plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import plus.dragons.createdragonsplus.common.CDPRegistrate;
import plus.dragons.createenchantmentindustry.common.CEICommon;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.processing.infuser.InfuserBlockEntity;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.registry.*;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.config.CEIAConfig;

@Mod(CEICommon.ID)
public class CEIACommon {
    public static final String ID = CEICommon.ID;
    public static final CDPRegistrate REGISTRATE = CEICommon.REGISTRATE;

    public CEIACommon(IEventBus modBus, ModContainer modContainer) {
        if (!ModList.get().isLoaded("apothic_enchanting"))
            return;
        modBus.register(new Common(modBus, modContainer));
    }

    public static class Common {
        IEventBus modBus;
        ModContainer modContainer;

        Common(IEventBus modBus, ModContainer modContainer) {
            this.modBus = modBus;
            this.modContainer = modContainer;
        }

        @SubscribeEvent
        public void construct(final FMLConstructModEvent event) {
            CEIABlocks.register(modBus);
            CEIAItems.register();
            CEIABlockEntities.register(modBus);
            CEIAFluids.register(modBus);
            CEIACreativeModeTabs.register(modBus);
            CEIARecipes.register(modBus);
            CEIADataComponents.register(modBus);
            CEIAItemAttributes.register(modBus);
            modBus.register(CEIAPackets.class);
            modBus.register(new CEIAConfig(modContainer));
            NeoForge.EVENT_BUS.addListener(CEIACommon::addReloadListeners);
            NeoForge.EVENT_BUS.register(CEIAFluids.Events.class);
        }

        @SubscribeEvent
        public void setup(final FMLCommonSetupEvent event) {}
    }

    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(InfuserBlockEntity.RELOAD_LISTENER);
    }

    public static ResourceLocation asResource(String name) {
        return CEICommon.asResource(name);
    }
}

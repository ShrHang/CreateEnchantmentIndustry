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

package plus.dragons.createenchantmentindustry.integration.apothic_enchanting.client;

import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import plus.dragons.createenchantmentindustry.common.CEICommon;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.client.ponder.CEIAPonderPlugin;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.client.registry.CEIAPartialModels;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.CEIACommon;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.contraptions.actors.enderWovenBag.EnderWovenBagItem;
import plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.registry.CEIABlocks;

@Mod(value = CEICommon.ID, dist = Dist.CLIENT)
public class CEIAClient {
    public CEIAClient(IEventBus modBus) {
        if (!ModList.get().isLoaded("apothic_enchanting"))
            return;
        // CEIPartialModels must be registered here,
        // or when PartialModelEventHandler#onRegisterAdditional triggered,
        // PartialModel.ALL won't include all partial model in 'some cases'
        // AllPartialModels#ini does not do this since AllPartialModels is already triggered at AllBlocks.TRACK
        // Issue: https://github.com/Creators-of-Create/Create/issues/8259
        CEIAPartialModels.register();
        modBus.addListener(CEIAClient::setup);
        modBus.addListener(EventPriority.LOWEST, CEIAClient::ponder);
    }

    public static void ponder(final FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new CEIAPonderPlugin());
    }

    public static void setup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    CEIABlocks.ENDER_WOVEN_BAG.asItem(),
                    CEIACommon.asResource("open"),
                    EnderWovenBagItem::override);
        });
    }
}

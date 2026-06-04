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

package plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.registry;

import static plus.dragons.createenchantmentindustry.integration.apothic_enchanting.common.registry.CEIABlocks.*;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import plus.dragons.createenchantmentindustry.common.registry.CEICreativeModeTabs;

public class CEIACreativeModeTabs {
    public static void register(IEventBus modBus) {
        modBus.addListener(CEIACreativeModeTabs::buildBaseContents);
    }

    private static void buildBaseContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CEICreativeModeTabs.BASE.getKey()) {
            event.accept(INFUSER);
            event.accept(BRASS_BOOKSHELF);
            event.accept(CREATIVE_BOOKSHELF);
            event.accept(ENDER_WOVEN_BAG);
            event.accept(CEIAFluids.INFUSED_DRAGON_BREATH.getBucket().get());
        }
    }
}

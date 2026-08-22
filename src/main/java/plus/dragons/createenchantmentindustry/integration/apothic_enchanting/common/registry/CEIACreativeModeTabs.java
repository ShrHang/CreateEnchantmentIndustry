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

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import plus.dragons.createenchantmentindustry.common.CEICommon;
import plus.dragons.createenchantmentindustry.integration.ModIntegration;
import plus.dragons.createenchantmentindustry.util.CEILang;

public class CEIACreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, CEICommon.ID);
    public static final Holder<CreativeModeTab> APOTHEOTIC = TABS.register("apotheotic", CEIACreativeModeTabs::base);

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    private static CreativeModeTab base(ResourceLocation id) {
        return CreativeModeTab.builder()
                .title(CEILang.description("itemGroup", id).component())
                .icon(CEIACreativeModeTabs::icon)
                .displayItems(CEIACreativeModeTabs::buildBaseContents)
                .withTabsBefore(CEICommon.asResource("base"))
                .build();
    }

    private static ItemStack icon() {
        if (ModIntegration.APOTHIC_ENCHANTING.enabled()) {
            var brassBookshelf = BuiltInRegistries.ITEM.getOptional(CEICommon.asResource("brass_bookshelf"));
            if (brassBookshelf.isPresent())
                return brassBookshelf.get().getDefaultInstance();
        }
        if (ModIntegration.APOTHEOSIS.enabled()) {
            var gemCutter = BuiltInRegistries.ITEM.getOptional(CEICommon.asResource("gem_cutter"));
            if (gemCutter.isPresent())
                return gemCutter.get().getDefaultInstance();
        }
        return CEIAFluids.INFUSED_DRAGON_BREATH.getBucket().get().getDefaultInstance();
    }

    private static void buildBaseContents(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(CEIAFluids.INFUSED_DRAGON_BREATH.getBucket().get());
    }
}

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

package plus.dragons.createenchantmentindustry.integration.apotheosis.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import plus.dragons.createenchantmentindustry.common.CEICommon;
import plus.dragons.createenchantmentindustry.integration.ModIntegration;
import plus.dragons.createenchantmentindustry.integration.apotheosis.common.registry.CEIAXBlocks;

public class CEIAXPonderPlugin implements PonderPlugin {
    public static final ResourceLocation APOTHEOTIC_CREATION_COMPONENTS = CEICommon.asResource("apotheotic_creation_components");

    @Override
    public String getModId() {
        return CEICommon.ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        var registration = helper.<ItemProviderEntry<?, ?>>withKeyFunction(RegistryEntry::getId);
        registration.forComponents(CEIAXBlocks.GEM_CUTTER)
                .addStoryBoard("gem_cutter", CEIAXPonderScenes::gemCutter, APOTHEOTIC_CREATION_COMPONENTS);
        registration.forComponents(CEIAXBlocks.AFFIX_AUGMENTOR)
                .addStoryBoard("affix_augmentor", CEIAXPonderScenes::affixAugmentor, APOTHEOTIC_CREATION_COMPONENTS);
        registration.forComponents(CEIAXBlocks.BLAZE_COMPOSER)
                .addStoryBoard("blaze_composer", BlazeComposerScene::basic, APOTHEOTIC_CREATION_COMPONENTS)
                .addStoryBoard("blaze_composer", BlazeComposerScene::superComposing, APOTHEOTIC_CREATION_COMPONENTS)
                .addStoryBoard("automate_blaze_composer", BlazeComposerScene::automate, AllCreatePonderTags.ARM_TARGETS);
        registration.forComponents(AllBlocks.ENCASED_FAN)
                .addStoryBoard("bulk_salvaging", CEIAXPonderScenes::bulkSalvaging, APOTHEOTIC_CREATION_COMPONENTS);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<RegistryEntry<?, ?>> entryHelper = helper.withKeyFunction(RegistryEntry::getId);

        helper.registerTag(APOTHEOTIC_CREATION_COMPONENTS)
                .addToIndex()
                .item(CEIAXBlocks.GEM_CUTTER.get(), true, false)
                .title("Apotheotic Creation Components")
                .description("Components relating to Apotheotic item creation")
                .register();

        entryHelper.addToTag(APOTHEOTIC_CREATION_COMPONENTS)
                .add(CEIAXBlocks.GEM_CUTTER)
                .add(CEIAXBlocks.AFFIX_AUGMENTOR)
                .add(CEIAXBlocks.BLAZE_COMPOSER)
                .add(AllBlocks.ENCASED_FAN);

        if (ModIntegration.APOTHIC_ENCHANTING.enabled())
            helper.addToTag(APOTHEOTIC_CREATION_COMPONENTS).add(CEICommon.asResource("infuser"));
    }
}

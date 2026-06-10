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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record AffixTemplateData(
        DynamicHolder<Affix> affix,
        float level,
        DynamicHolder<LootRarity> rarity,
        ResourceLocation sourceCategory,
        boolean transcendent) {

    public static final Codec<AffixTemplateData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AffixRegistry.INSTANCE.holderCodec().fieldOf("affix").forGetter(AffixTemplateData::affix),
            Codec.floatRange(0, Float.MAX_VALUE).fieldOf("level").forGetter(AffixTemplateData::level),
            RarityRegistry.INSTANCE.holderCodec().fieldOf("rarity").forGetter(AffixTemplateData::rarity),
            ResourceLocation.CODEC.fieldOf("source_category").forGetter(AffixTemplateData::sourceCategory),
            Codec.BOOL.optionalFieldOf("transcendent", false).forGetter(AffixTemplateData::transcendent))
            .apply(instance, AffixTemplateData::new));
    public static final StreamCodec<ByteBuf, AffixTemplateData> STREAM_CODEC = StreamCodec.composite(
            AffixRegistry.INSTANCE.holderStreamCodec(), AffixTemplateData::affix,
            ByteBufCodecs.FLOAT, AffixTemplateData::level,
            RarityRegistry.INSTANCE.holderStreamCodec(), AffixTemplateData::rarity,
            ResourceLocation.STREAM_CODEC, AffixTemplateData::sourceCategory,
            ByteBufCodecs.BOOL, AffixTemplateData::transcendent,
            AffixTemplateData::new);
    public boolean isBound() {
        return affix.isBound() && rarity.isBound();
    }

    public AffixTemplateData withLevel(float level) {
        return new AffixTemplateData(affix, Math.max(0, level), rarity, sourceCategory, level > Affix.MAX_LEVEL || transcendent);
    }

    public AffixTemplateData withRarity(DynamicHolder<LootRarity> rarity) {
        return new AffixTemplateData(affix, level, rarity, sourceCategory, transcendent);
    }

    public AffixInstance toInstance(ItemStack stack) {
        return new AffixInstance(affix, level, rarity, stack);
    }
}

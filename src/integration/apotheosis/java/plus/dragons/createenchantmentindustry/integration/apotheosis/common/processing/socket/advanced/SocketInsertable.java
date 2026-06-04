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

package plus.dragons.createenchantmentindustry.integration.apotheosis.common.processing.socket.advanced;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.GemBonus;
import dev.shadowsoffire.apotheosis.tiers.Constraints;
import dev.shadowsoffire.apotheosis.tiers.TieredWeights;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import plus.dragons.createdragonsplus.util.CodeReference;

@CodeReference(targets = "dev.shadowsoffire.apotheosis.socket.gem.Gem", source = "Apotheosis", license = "mit")
public class SocketInsertable implements CodecProvider<SocketInsertable>, TieredWeights.Weighted, Constraints.Constrained {
    public static final Codec<SocketInsertable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.listOf().fieldOf("socket").forGetter(SocketInsertable::getSocketTypes),
            GemBonus.CODEC.listOf().fieldOf("bonuses").forGetter(SocketInsertable::getBonuses),
            TieredWeights.CODEC.fieldOf("weights").forGetter(TieredWeights.Weighted::weights),
            Constraints.CODEC.optionalFieldOf("constraints", Constraints.EMPTY).forGetter(Constraints.Constrained::constraints),
            Codec.BOOL.optionalFieldOf("unique", false).forGetter(SocketInsertable::isUnique))
            .apply(inst, SocketInsertable::new));

    protected final List<String> socketTypes;
    protected final List<GemBonus> bonuses;
    protected final TieredWeights weights;
    protected final Constraints constraints;
    protected final boolean unique;

    protected transient final Map<LootCategory, GemBonus> bonusMap = new IdentityHashMap<>();

    public SocketInsertable(List<String> socketTypes, List<GemBonus> bonuses, TieredWeights weights, Constraints constraints, boolean unique) {
        this.socketTypes = socketTypes;
        this.bonuses = bonuses;
        this.weights = weights;
        this.constraints = constraints;
        this.unique = unique;
        Preconditions.checkArgument(!bonuses.isEmpty(), "No bonuses were provided.");
        for (GemBonus bonus : this.bonuses) {
            validateBonus(bonus);
            for (Holder<LootCategory> category : bonus.getGemClass().types()) {
                this.bonusMap.put(category.value(), bonus);
            }
        }
    }

    private void validateBonus(GemBonus bonus) {
        for (Holder<LootCategory> category : bonus.getGemClass().types()) {
            if (this.bonusMap.containsKey(category.value())) {
                GemBonus conflict = this.bonusMap.get(category.value());
                throw new IllegalArgumentException("Gem Bonus for class %s conflicts with existing bonus for class %s (categories overlap)".formatted(bonus.getGemClass().key(), conflict.getGemClass().key()));
            }
        }
    }

    public boolean isUnique() {
        return unique;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public TieredWeights getWeights() {
        return weights;
    }

    public List<GemBonus> getBonuses() {
        return bonuses;
    }

    public List<String> getSocketTypes() {
        return socketTypes;
    }

    @Override
    public Constraints constraints() {
        return constraints;
    }

    @Override
    public TieredWeights weights() {
        return weights;
    }

    @Override
    public Codec<? extends SocketInsertable> getCodec() {
        return CODEC;
    }
}

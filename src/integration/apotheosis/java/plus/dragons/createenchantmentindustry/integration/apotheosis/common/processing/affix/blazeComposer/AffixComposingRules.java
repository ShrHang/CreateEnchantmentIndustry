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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AffixComposingRules extends SimpleJsonResourceReloadListener {
    public static final AffixComposingRules INSTANCE = new AffixComposingRules();
    private static final Logger LOGGER = LoggerFactory.getLogger(AffixComposingRules.class);
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "create_enchantment_industry/affix_composing_rules";

    private Map<ResourceLocation, Rule> affixRules = Map.of();
    private Map<ResourceLocation, Rule> rarityRules = Map.of();

    private AffixComposingRules() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, Rule> affixes = new HashMap<>();
        Map<ResourceLocation, Rule> rarities = new HashMap<>();
        objects.forEach((id, element) -> RuleFile.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(message -> LOGGER.warn("Failed to parse affix composing rule {}: {}", id, message))
                .ifPresent(ruleFile -> {
                    ruleFile.affix.ifPresent(affix -> affixes.merge(affix, ruleFile.rule, Rule::merge));
                    ruleFile.rarity.ifPresent(rarity -> rarities.merge(rarity, ruleFile.rule, Rule::merge));
                    if (ruleFile.affix.isEmpty() && ruleFile.rarity.isEmpty()) {
                        LOGGER.warn("Affix composing rule {} does not target an affix or rarity and was ignored", id);
                    }
                }));
        affixRules = Map.copyOf(affixes);
        rarityRules = Map.copyOf(rarities);
        LOGGER.info("Loaded {} affix composing affix rules and {} rarity rules", affixRules.size(), rarityRules.size());
    }

    public float getCostMultiplier(AffixTemplateData data) {
        return getAffixRule(data.affix()).costMultiplier()
                * getRarityRule(data.rarity().getId()).costMultiplier();
    }

    public float getAugmentingCostMultiplier(AffixInstance instance) {
        Rule affixRule = getAffixRule(instance.affix());
        Rule rarityRule = getRarityRule(instance.rarity().getId());
        return affixRule.costMultiplier()
                * affixRule.augmentingCostMultiplier()
                * rarityRule.costMultiplier()
                * rarityRule.augmentingCostMultiplier();
    }

    public float getMaxLevel(AffixTemplateData data, float templateMaxLevel) {
        float maxLevel = templateMaxLevel;
        Optional<Float> affixMax = getAffixRule(data.affix()).maxLevel();
        Optional<Float> rarityMax = getRarityRule(data.rarity().getId()).maxLevel();
        if (affixMax.isPresent())
            maxLevel = Math.min(maxLevel, affixMax.get());
        if (rarityMax.isPresent())
            maxLevel = Math.min(maxLevel, rarityMax.get());
        return maxLevel;
    }

    public boolean denies(BlazeComposerMode mode, boolean hyper, AffixTemplateData data) {
        Rule affixRule = getAffixRule(data.affix());
        Rule rarityRule = getRarityRule(data.rarity().getId());
        return affixRule.denies(mode, hyper) || rarityRule.denies(mode, hyper);
    }

    public boolean deniesAugmenting(AffixInstance instance) {
        Rule affixRule = getAffixRule(instance.affix());
        Rule rarityRule = getRarityRule(instance.rarity().getId());
        return affixRule.denyAugmenting() || rarityRule.denyAugmenting();
    }

    private Rule getAffixRule(DynamicHolder<Affix> affix) {
        return affixRules.getOrDefault(affix.getId(), Rule.DEFAULT);
    }

    private Rule getRarityRule(ResourceLocation rarity) {
        return rarityRules.getOrDefault(rarity, Rule.DEFAULT);
    }

    public record RuleFile(
            Optional<ResourceLocation> affix,
            Optional<ResourceLocation> rarity,
            Rule rule) {
        public static final Codec<RuleFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("affix").forGetter(RuleFile::affix),
                ResourceLocation.CODEC.optionalFieldOf("rarity").forGetter(RuleFile::rarity),
                Rule.CODEC.optionalFieldOf("rule", Rule.DEFAULT).forGetter(RuleFile::rule))
                .apply(instance, RuleFile::new));
    }

    public record Rule(
            float costMultiplier,
            float augmentingCostMultiplier,
            Optional<Float> maxLevel,
            boolean denyExtraction,
            boolean denyApplying,
            boolean denyMerge,
            boolean denyAugmenting,
            boolean denyHyper) {

        public static final Rule DEFAULT = new Rule(1, 1, Optional.empty(), false, false, false, false, false);
        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("cost_multiplier", 1F).forGetter(Rule::costMultiplier),
                Codec.FLOAT.optionalFieldOf("augmenting_cost_multiplier", 1F).forGetter(Rule::augmentingCostMultiplier),
                Codec.FLOAT.optionalFieldOf("max_level").forGetter(Rule::maxLevel),
                Codec.BOOL.optionalFieldOf("deny_extraction", false).forGetter(Rule::denyExtraction),
                Codec.BOOL.optionalFieldOf("deny_applying", false).forGetter(Rule::denyApplying),
                Codec.BOOL.optionalFieldOf("deny_merge", false).forGetter(Rule::denyMerge),
                Codec.BOOL.optionalFieldOf("deny_augmenting", false).forGetter(Rule::denyAugmenting),
                Codec.BOOL.optionalFieldOf("deny_hyper", false).forGetter(Rule::denyHyper))
                .apply(instance, Rule::new));
        public Rule merge(Rule other) {
            return new Rule(
                    costMultiplier * other.costMultiplier,
                    augmentingCostMultiplier * other.augmentingCostMultiplier,
                    other.maxLevel.or(() -> maxLevel),
                    denyExtraction || other.denyExtraction,
                    denyApplying || other.denyApplying,
                    denyMerge || other.denyMerge,
                    denyAugmenting || other.denyAugmenting,
                    denyHyper || other.denyHyper);
        }

        public boolean denies(BlazeComposerMode mode, boolean hyper) {
            if (hyper && denyHyper)
                return true;
            return switch (mode) {
                case EXTRACT -> denyExtraction;
                case APPLY -> denyApplying;
                case MERGE -> denyMerge;
            };
        }
    }
}

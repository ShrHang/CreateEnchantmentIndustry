## Create: Enchantment Industry 2.5.0

### Update
* Add Blaze Composer & Affix Templates for handling apotheosis affixes.
* Apotheotic Content now has an independent CreativeTab.
* Added more customizable options of Blaze Forger & Enchanter.
* Improved Arm Interaction for Blaze Enchanter and Blaze Forger.
* Improved goggle tooltips of Blaze Forger & Enchanter.
* Added Goggle tooltip for Gem Cutter & Affix Augmentor.
* Added support for third-party lightning rods exposed through shared block tags ([#457](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/issues/457)).
* Added Printer support for Dye Depot's extended colors in banner patterns, custom names, goggles, and JEI previews ([#466](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/issues/466)).

If you are **modpack developer**:

#### Migration From Older Version (if you are already using preview version of 2.5.0 you can safely ignore these)

The old server config option `enchantmentMaxLevelExtension` is no longer used. There is no automatic fallback from that old config value. Modpacks that previously changed it must set both new options explicitly.

Some old datapack datamaps are still supported through a compatibility layer (might be removed in the future), but they are deprecated and will print a server log warning when used.

Migration table:

| Old entry | New entry | Compatibility fallback |
| --- | --- | --- |
| `enchantmentMaxLevelExtension` config | `blazeEnchanterMaxLevelExtension` and `blazeForgerMaxLevelExtension` config | No |
| `super_enchanting/custom_level_extension` datamap | `enchantment_processing/rules.level_extension.blaze_enchanter` and `level_extension.blaze_forger` | Yes |
| `forging/cost_multiplier` datamap | `enchantment_processing/rules.cost_multiplier.blaze_forger.merge` and `cost_multiplier.blaze_forger.apply` | Yes |
| `forging/split_enchantment_cost_multiplier` datamap | `enchantment_processing/rules.cost_multiplier.blaze_forger.split` | Yes |

If you need any help on customization, please read https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/blob/1.21.1/6.0.0-dev/MODPACK-README.md

### Developer API Change

> [!IMPORTANT]
> `PrintingBehaviour.register(Provider)` has been removed. Addons must now register uniquely identified `PrintingBehaviourProvider` entries in the NeoForge registry identified by `CEIRegistries.PRINTING_BEHAVIOUR_PROVIDER`, preferably through `DeferredRegister`. Providers accept an optional priority and follow the standard NeoForge registry lifecycle ([#452](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/issues/452)).

### Change
* Affix Augmentor now has configurable maximum level and global cost multiplier, and affix composing rules can deny or reprice augmenting separately.
* Blaze Forger now needs to select mode manually via side panel for better automation setup.
* Brass Affix Templates now use Copper Sheets, while Apotheotic Affix Templates use Sturdy Sheets, making every Affix Template craftable in survival. Recipe changes contributed by [@Eatocee](https://github.com/Eatocee) in [#456](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/pull/456), with the generated recipes refreshed for [#455](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/issues/455).
* Blaze Enchanter penalty curses are now limited to obtainable sources and can be customized with datapack allow/deny tags ([#449](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/issues/449)).
* Updated Create: Dragons Plus, Sable, and Create: Aeronautics-family integration dependencies.

### Fixed
* Fixed optional integration block loot tables losing their `neoforge:conditions` during data generation.
* Fixed Affix Augmentor idle processing state so belt-held items can start processing reliably.
* Fixed how curses work in the Blaze Forger.
* Fixed the Gem Cutter not processing flawless gems on a Depot.
* Fixed fishing experience collected by a Deployer not repairing its Mending fishing rod ([#447](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/issues/447)).
* Fixed experience being lost when an experience-holding block is removed with a Create wrench ([#454](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/issues/454)).
* Corrected total-experience calculation for levels 16 through 30, contributed by [@Jabberdrake](https://github.com/Jabberdrake) in [#458](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/pull/458).
* Fixed completed Classic Blaze Enchanter outputs remaining locked in the machine ([#462](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/issues/462)).
* Fixed the Infuser losing dynamic Apothic Enchanting recipe outputs and mishandling recipe inputs ([#464](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/issues/464)).

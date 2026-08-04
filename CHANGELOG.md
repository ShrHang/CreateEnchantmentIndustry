## Create: Enchantment Industry 2.5.1

### Change
* Updated the Apotheosis integration dependencies for Minecraft 1.21.1:
  * Placebo 9.9.2
  * Apothic Attributes 2.10.1
  * Apothic Enchanting 1.6.0
  * Apothic Spawners 1.4.0
  * Apotheosis 8.7.0
  * Gateways to Eternity 5.1.0
* Updated the development NeoForge version to 21.1.248 to satisfy the latest Apotheosis runtime requirements.
* Migrated rarity material references to the canonical names introduced by Apotheosis 8.6.0.

### Fixed
* Fixed Pondering an Encased Fan crashing while compiling the Bulk Salvaging scene with current Apotheosis versions ([#469](https://github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry/issues/469)).
* Fixed Apotheosis recipe data generation failing after the rarity material API rename.

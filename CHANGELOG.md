## Create: Enchantment Industry 2.4.0

### Update
* Added support for **apotheosis mod series**: Bulk Salvaging, Brass Bookshelf, Creative Bookshelf, Infuser, Ender Woven Bag, Gem Cutter and Affix Augmenter.
> Support for apotheosis is still *not complete in terms of "complete production line to make end game gears"*. But its existing appliances are actually playable and complete. More appliance will come in the future.
* Added Turkish Localization by @Mehmet Eymen Eskici
* Added Bulgarian Localization by @Programstor
* Added more Sable support: Experience Lantern and Ender Woven Bag pull direction when working from a Sable sub-level; charged lightning failing to transform experience blocks located inside Sable sub-levels; Added Sable physics properties for Create: Enchantment Industry blocks.

### Change
* Forging cost is now more reasonable (by @Feiyang Wu)

### Fix
* Fixed ponder crash when controller BE is null in a more suitable way (by @MSTY2003)
* Display accurate message when experience cost is greater than available amount. (by @Mathias Dejerud Rådstam)
* Fixed The mobs couldn't drop experience nugget while in chunks that in offline forceloading mode.
* Fixed deployer drop more exp nuggets than it should in long run.
* Fixed opening JEI preview for enchanted book crafting leads to crash when Cold Sweat is installed.
* Fixed Mechanical Grinder ignores recipe with no fluid input and output.
* Fixed Crushing Wheels dropping XP nuggets at a much lower rate than expected.
* Fixed XP duplication with Sable / Create Aeronautics.

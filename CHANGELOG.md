## Create: Enchantment Industry 2.4.0

### Update
* Add support for **apotheosis mod series**. 
> Support for apotheosis is still *not complete in terms of "complete production line to make end game gears"*. But its existing appliances are actually playable and complete. More appliance will come in the future.
* Add Turkish Localization by @Mehmet Eymen Eskici
* Add Bulgarian Localization by @Programstor

### Change
* Forging cost is now more reasonable (by @Feiyang Wu)

### Fix
* Fix ponder crash when controller BE is null in a more suitable way (by @MSTY2003)
* Display accurate message when experience cost is greater than available amount. (by @Mathias Dejerud Rådstam)
* Fix The mobs couldn't drop experience nugget while in chunks that in offline forceloading mode.
* Fix deployer drop more exp nuggets than it should in long run.
* Fix opening JEI preview for enchanted book crafting leads to crash when Cold Sweat is installed.
* Fix Mechanical Grinder ignores recipe with no fluid input and output.
* Fix Crushing Wheels dropping XP nuggets at a much lower rate than expected.

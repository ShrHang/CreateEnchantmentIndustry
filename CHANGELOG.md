## Create: Enchantment Industry 2.5.0

### Update
* Add Blaze Composer & Affix Templates for handling apotheosis affixes.
* Apotheotic Content now has an independent CreativeTab.
* Greatly update goggle tooltips of Blaze Forger & Enchanter.

### Change
* Blaze Composer Hyper charging now requires a configurable activator item, keeps the unlock on the machine after Hyper fuel runs out, and exposes only Normal Mode / Hyper Mode to players.
* Affix Augmentor now selects the lowest-level valid affix deterministically, skips level-independent or rule-denied affixes, and prices each upgrade from the current Apotheosis Augmenting Table cost reference plus the actual level delta.
* Affix Augmentor now has configurable maximum level and global cost multiplier, and affix composing rules can deny or reprice augmenting separately.
* Blaze Forger now needs to select mode manually via side panel for better automation setup.

### Fixed
* Fixed optional integration block loot tables losing their `neoforge:conditions` during data generation.
* Fixed Affix Augmentor idle processing state so belt-held items can start processing reliably.
* Fixed how `curse` in Blaze Forger work.

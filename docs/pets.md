# Creation
The example pet (in the example_configs folder) should contain all the reference information needed to create a new pet
- Each pet is created by defining a new `.yml` file in the `PetsPlugin/pets` directory
- All sub-folders in this directory are also parsed

# Levels
- Each pet has its own leveling system. This can be duplicated across all pets, or differ between them. It is up to you!
- You can specify the min/max level of the pet
  - Making the min/max the same will not allow the pet to level up
- The `amounts` section requires you to define `maxLevel - minLevel` values. These determine how much xp each level takes to complete
  - If you do not specify enough levels, a warning will print in the console

## Earning Experience
Experience is needed for pets to level up. Equipped pets earn experience from different events
- You can define different ways for players to obtain XP in `xp.yml`
  - Setting `useGlobalForAllPets` to true will override any entries in individual pet files
- For each pet, you use the `xp` section to define what that pet subscribes to
  - If multiple selected pets subscribe to the same entry, then the XP will be shared evenly
  - Once at max level, pets no longer earn XP so their share is redistributed among the remaining pets (if any)
- The following experience types have their own WorldGuard flag which must be set:
  - `MINING` - `flag: pets-mining-xp` - Earn XP by mining blocks
  - `KILLING` - `flag: pets-killing-xp` - Earn XP by killing entities

# Boosts
There are 6 types of boosts available. A pet can have an unlimited number of boosts attached to it
- `DAMAGE` Multiply damage dealt to all entities or the type(s) you specify
- `DOUBLE_REWARD` A percent chance to give 2 rewards instead of 1 (MineRewards _private_)
- `GENERAL_REWARD` Increase the chance to find rewards (MineRewards _private_)
- `ENCHANT` Increase activation chance of all or specified enchants (PrisonEnchants _private_)
- `POTION_EFFECT` Apply an infinite potion effect
- `XP` Increase experience earned (PlayerLevels _private_)
- All boosts apart from `POTION_EFFECT` accept an equation to determine the boost amount. The variable `x` is replaced with the pet's level

# Rewards
Each pet has its own reward system. This allows an equipped pet to run commands when different events happen
- The `rewardChance` field is the chance to give out a reward when requirements are met. This value must be between 0 and 1 [0,1]
- The following reward types have their own WorldGuard flag which must be set:
  - `MINING` - `flag: pets-mining-rewards` - Give a reward when breaking blocks
  - `KILLING` - `flag: pets-killing-rewards` - Give a reward when killing entities
- When giving a reward, the plugin will send any `messages` before sending the `commands`

# Pet Item
This is the item that players will receive in-game when interacting with this pet
- This section parses special placeholders. These are only usable in the lore of the item except for `{lvl}`
  - `{lvl}` The current level
  - `{next_lvl_xp}` The total experience needed to reach the next level
  - `{xp}` How much experience the pet has gathered since reaching this level
  - `{xp_remaining}` The amount of experience still needed to reach the next level
  - `{percent}` The percent of the way to the next level (2 decimal places max)
  - `{boost_x}` Gets the boost amount for boost ID `x` at the pet's current level

# Menu Item
Each pet has a special section, which is used when the pet is displayed in a menu (this is different from the pet item players can obtain).
- This section parses special placeholders. These are only usable in the lore of the item
  - `{max_level}` The max level of the pet
  - `{boost_x_y}` Gets the boost amount for boost ID `x` at level `y`
# PetsPlugin
An item based pets plugin made to hook into various of my other plugins

### Features
- Create item based pets with levels and custom rewards
- Each pet allows for multiple types of **boosts** which can scale with pet level
- Each pet has its own levelling system
- Each pet has its own *extremely customizable* rewards system
- Pet selection is done through a very simple GUI

### Requirements
- Required dependencies: MineRewards (private), PlayerLevels (private), PrisonEnchants (private), PrisonMines, ServerUtils

### Creating Pets
- Check the examples config folder for pet examples
- Each pet is created with its own text file in the `PetsPlugin/pets` folder
  - Pets will also be loaded from any folder in the same directory
  - Each file must be a `.yml` file
#### Levels
- Each pet has its own levelling system
- You specify the start and max level
  - Making them the same will make a pet never level up
- The `amounts:` section requires `maxLevel-startLevel` numbers in the list to define how much xp each level takes to complete
  - If you do not specify the right amount, the plugin will print a warning to the console
#### Boosts
- Pets support 5 types of boosts. You can specify as many as you want
- `type: DOUBLE_REWARD` Chance to give 2 rewards instead of one (MineRewards)
- `type: GENERAL_REWARD` Increase general reward chance (MineRewards)
- `type: ENCHANT` Increase the chance of an enchant activating (PrisonEnchants)
- `type: POTION_EFFECT` Apply an infinite potion effect
- `type: XP` Increase xp earned (PlayerLevels)
- Equations support the variable `x` which is replaced with the pet's current level
#### Rewards
- Pets support two types of rewards. You can specify as many as you want
- `allowAll` If the reward should be given for all mines blocks or all killed entities
- `whitelist` The list of Materials/EntityTypes that this reward will activate from
- `rewardChance` The chance to give out a reward when requirements are met [0, 1].
- `type: MINE` Give a reward when a block is broken in a mine manually (PrisonMines)
- `type: KILL` Give a reward when a mob is killed by a player
  - When giving a reward, the plugin will send any `messages` before sending the `commands`

### Player Commands
- The command is `pets` (alias `pet`) with the permission `petsplugin.pets`
- The only command requires no arguments and it opens the pet selection menu
  - This permission should be given since players have permission to edit the menu without opening it

### Admin Commands
- The base command is `petsadmin` with the permission `petsplugin.petsadmin`
- All commands require permission to use which follows the format `petsplugin.petsadmin.command` where command is the name of the command
- `petsadmin addItem <player> <petID> <menuID> [xp]` Adds a pet to the player's /rew pages inventory
- `petsadmin confirm` Confirm an action
- `petsadmin give <player> <petID> [xp]` Gives a pet to the player
- `petsadmin help` Opens the help menu
- `petsadmin info` Print out a pet's data
- `petsadmin list` Opens the pet list menu
- `petsadmin reload [arg]` Reloads this plugin or a specified portion
- `petsadmin resetData` Deletes all selected pets from the database

### Notes
- Right-clicking while crouching with a pet in the main hand will put it in the `/pets` menu
  - If the player has a pet selected, the held pet will swap places with it
- When put into the `/pets` menu, the pet item is not saved. Any changes made to a pet's name and lore will be deleted once a pet enters the menu.
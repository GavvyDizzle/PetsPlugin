# Player Commands
- The command is `pets` (alias `pet`) with the permission `petsplugin.player`
- All commands require permission to use which follows the format `petsplugin.player.command` where command is the name of the command
    - `pets` Opens the pet selection menu (no arguments)
    - `pets help` Opens the help menu
    - `pets list` Opens the pets list main menu

# Admin Commands
- The base command is `petsadmin` with the permission `petsplugin.admin`
- All commands require permission to use which follows the format `petsplugin.admin.command` where command is the name of the command
    - `petsadmin addItem <player> <petID> <menuID> [xp]` Adds a pet to the player's /rew pages inventory (RewardsInventory)
    - `petsadmin confirm` Confirm an action
    - `petsadmin give <player> <petID> [xp]` Gives a pet to the player
    - `petsadmin help` Opens the help menu
    - `petsadmin info` Print out a pet's data
    - `petsadmin list` Opens the pet list menu
    - `petsadmin openPlayerMenu <player>` Opens a players selection menu (supports offline players)
    - `petsadmin reload [arg]` Reloads this plugin or a specified portion
    - `petsadmin resetData` Deletes all selected pets from the database
    - `petsadmin rewardInfo <petID> <rewardID>` Print out a pet's reward information
    - `petsadmin setXP <xp>` Update your held pet's total experience. This will update the pet's name and lore placeholders
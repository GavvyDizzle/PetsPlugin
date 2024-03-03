package com.github.gavvydizzle.petsplugin.commands;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.commands.admin.*;
import com.github.gavvydizzle.petsplugin.gui.InventoryManager;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.player.PlayerManager;
import com.github.gavvydizzle.petsplugin.storage.PlayerData;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.CommandManager;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public class AdminCommandManager extends CommandManager {

    private final CommandConfirmationManager confirmationManager;
    private String helpCommandPadding;

    public AdminCommandManager(PluginCommand command, CommandConfirmationManager confirmationManager, PlayerManager playerManager, PetManager petManager, InventoryManager inventoryManager, PlayerData data) {
        super(command);
        this.confirmationManager = confirmationManager;

        if (PetsPlugin.getInstance().isRewardsInventoryLoaded()) {
            registerCommand(new AddToRewardsMenuCommand(this, petManager));
        }
        registerCommand(new AdminConfirmCommand(this));
        registerCommand(new AdminHelpCommand(this));
        registerCommand(new GiveToPlayerCommand(this, petManager));
        registerCommand(new OpenPetListCommand(this, inventoryManager));
        registerCommand(new OpenPlayerMenu(this, playerManager, inventoryManager, data));
        registerCommand(new PetInfoCommand(this));
        registerCommand(new PetRewardInfoCommand(this, petManager));
        registerCommand(new ReloadCommand(this));
        registerCommand(new ResetDatabaseCommand(this, petManager, data));
        registerCommand(new SetPetXPCommand(this, petManager));
        sortCommands();

        reload();
    }

    public void reload() {
        FileConfiguration config = PetsPlugin.getConfigManager().get("commands");
        if (config == null) return;

        config.addDefault("commandDisplayName.admin", getCommandDisplayName());
        config.addDefault("helpCommandPadding.admin", "&6-----(" + PetsPlugin.getInstance().getName() + " Admin Commands)-----");

        for (SubCommand subCommand : getSubcommands()) {
            setAdminDescriptionDefault(config, subCommand);
        }

        setCommandDisplayName(config.getString("commandDisplayName.admin"));
        helpCommandPadding = Colors.conv(config.getString("helpCommandPadding.admin"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length > 0) {
            for (SubCommand subCommand : getSubcommands()) {
                if (args[0].equalsIgnoreCase(subCommand.getName())) {

                    if (!subCommand.hasPermission(sender)) {
                        onNoPermission(sender, args);
                        return true;
                    }

                    if (subCommand instanceof ConfirmationCommand) {
                        confirmationManager.attemptConfirmationCreation(subCommand, ((ConfirmationCommand) subCommand).getConfirmationMessage(), sender, args);
                        return true;
                    }

                    subCommand.perform(sender, args);
                    return true;
                }
            }
            onInvalidSubcommand(sender, args);
            return true;
        }
        onNoSubcommand(sender, args);
        return true;
    }

    public String getHelpCommandPadding() {
        return helpCommandPadding;
    }


    public void setAdminDescriptionDefault(FileConfiguration fileConfiguration, SubCommand subCommand) {
        fileConfiguration.addDefault("descriptions.admin." + subCommand.getName(), subCommand.getDescription());
    }

    /**
     * @param subCommand The SubCommand
     * @return The description of this SubCommand as defined in this config file
     */
    public String getAdminDescription(SubCommand subCommand) {
        FileConfiguration config = PetsPlugin.getConfigManager().get("commands");
        if (config == null) return "";

        return config.getString("descriptions.admin." + subCommand.getName());
    }
}
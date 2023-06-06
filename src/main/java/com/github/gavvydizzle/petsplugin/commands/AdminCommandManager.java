package com.github.gavvydizzle.petsplugin.commands;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.commands.admin.*;
import com.github.gavvydizzle.petsplugin.configs.CommandsConfig;
import com.github.gavvydizzle.petsplugin.gui.InventoryManager;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
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

    public AdminCommandManager(PluginCommand command, CommandConfirmationManager confirmationManager,
                               PetManager petManager, InventoryManager inventoryManager, PlayerData data) {
        super(command);
        this.confirmationManager = confirmationManager;

        if (PetsPlugin.getInstance().isRewardsInventoryLoaded()) {
            registerCommand(new AddToRewardsMenuCommand(this, petManager));
        }
        registerCommand(new AdminConfirmCommand(this));
        registerCommand(new AdminHelpCommand(this));
        registerCommand(new GiveToPlayerCommand(this, petManager));
        registerCommand(new OpenPetListCommand(this, inventoryManager));
        registerCommand(new PetInfoCommand(this));
        registerCommand(new PetRewardInfoCommand(this, petManager));
        registerCommand(new ReloadCommand(this));
        registerCommand(new ResetDatabaseCommand(this, petManager, data));
        sortCommands();

        reload();
    }

    public void reload() {
        FileConfiguration config = CommandsConfig.get();
        config.options().copyDefaults(true);
        config.addDefault("commandDisplayName.admin", getCommandDisplayName());
        config.addDefault("helpCommandPadding.admin", "&6-----(" + PetsPlugin.getInstance().getName() + " Admin Commands)-----");

        for (SubCommand subCommand : getSubcommands()) {
            CommandsConfig.setAdminDescriptionDefault(subCommand);
        }
        CommandsConfig.save();

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
}
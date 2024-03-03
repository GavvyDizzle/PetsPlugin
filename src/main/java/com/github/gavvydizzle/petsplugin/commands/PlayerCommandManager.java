package com.github.gavvydizzle.petsplugin.commands;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.commands.player.OpenMenuCommand;
import com.github.gavvydizzle.petsplugin.commands.player.PlayerHelpCommand;
import com.github.gavvydizzle.petsplugin.gui.InventoryManager;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.CommandManager;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PlayerCommandManager extends CommandManager {

    private final InventoryManager inventoryManager;
    private String helpCommandPadding;

    public PlayerCommandManager(PluginCommand command, InventoryManager inventoryManager) {
        super(command);
        this.inventoryManager = inventoryManager;

        registerCommand(new OpenMenuCommand(this, inventoryManager));
        registerCommand(new PlayerHelpCommand(this));
        sortCommands();

        reload();
    }

    public void reload() {
        FileConfiguration config = PetsPlugin.getConfigManager().get("commands");
        if (config == null) return;

        config.addDefault("commandDisplayName.player", getCommandDisplayName());
        config.addDefault("helpCommandPadding.player", "&6-----(Pets Commands)-----");

        for (SubCommand subCommand : getSubcommands()) {
            setPlayerDescriptionDefault(config, subCommand);
        }

        setCommandDisplayName(config.getString("commandDisplayName.player"));
        helpCommandPadding = Colors.conv(config.getString("helpCommandPadding.player"));
    }

    @Override
    public void onNoSubcommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            inventoryManager.getPetMenu().openInventory(player);
        }
    }

    public String getHelpCommandPadding() {
        return helpCommandPadding;
    }


    public void setPlayerDescriptionDefault(FileConfiguration fileConfiguration, SubCommand subCommand) {
        fileConfiguration.addDefault("descriptions.player." + subCommand.getName(), subCommand.getDescription());
    }

    /**
     * @param subCommand The SubCommand
     * @return The description of this SubCommand as defined in this config file
     */
    public String getPlayerDescription(SubCommand subCommand) {
        FileConfiguration config = PetsPlugin.getConfigManager().get("commands");
        if (config == null) return "";

        return config.getString("descriptions.player." + subCommand.getName());
    }
}

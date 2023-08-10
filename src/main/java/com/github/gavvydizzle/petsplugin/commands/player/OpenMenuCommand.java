package com.github.gavvydizzle.petsplugin.commands.player;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.commands.PlayerCommandManager;
import com.github.gavvydizzle.petsplugin.configs.CommandsConfig;
import com.github.gavvydizzle.petsplugin.gui.InventoryManager;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class OpenMenuCommand extends SubCommand {

    private final InventoryManager inventoryManager;

    public OpenMenuCommand(PlayerCommandManager adminCommandManager, InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;

        setName("list");
        setDescription("Opens the pet list menu");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " list");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            inventoryManager.getPetListMainMenu().openInventory(player);
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
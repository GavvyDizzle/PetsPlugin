package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.gui.InventoryManager;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class OpenPetListCommand extends SubCommand {

    private final InventoryManager inventoryManager;

    public OpenPetListCommand(AdminCommandManager adminCommandManager, InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;

        setName("list");
        setDescription("Opens the pet list menu");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " list");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            inventoryManager.getPetListMenu().openInventory(((Player) sender).getPlayer());
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
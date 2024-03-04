package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.gui.InventoryManager;
import com.github.gavvydizzle.petsplugin.player.LoadedPlayer;
import com.github.gavvydizzle.petsplugin.player.PlayerManager;
import com.github.gavvydizzle.petsplugin.storage.PlayerData;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class OpenPlayerMenu extends SubCommand {

    private final PlayerManager playerManager;
    private final InventoryManager inventoryManager;
    private final PlayerData data;

    public OpenPlayerMenu(AdminCommandManager adminCommandManager, PlayerManager playerManager, InventoryManager inventoryManager, PlayerData data) {
        this.playerManager = playerManager;
        this.inventoryManager = inventoryManager;
        this.data = data;

        setName("openPlayerMenu");
        setDescription("Opens a player's pet menu");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " openPlayerMenu <player>");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;

        if (args.length < 2) {
            sender.sendMessage(getColoredSyntax());
            return;
        }

        OfflinePlayer destination = Bukkit.getPlayer(args[1]);
        if (destination == null) {
            destination = Bukkit.getOfflinePlayer(args[1]);
            if (!destination.hasPlayedBefore() && !destination.isOnline()) {
                sender.sendMessage(ChatColor.RED + args[1] + " is not a valid player.");
                return;
            }
        }

        LoadedPlayer loadedPlayer = playerManager.getOnlineOrOfflinePlayer(destination.getUniqueId());
        if (loadedPlayer != null) {
            inventoryManager.getPetMenu().adminOpenInventory(player, loadedPlayer);
        }
        else { // Load profile from database and open the main menu for the admin
            if (destination.isOnline()) {
                sender.sendMessage(ChatColor.RED + "No menu loaded for " + destination.getName());
                return;
            }

            sender.sendMessage(ChatColor.YELLOW + "Generating menu for " + destination.getName() + "...");

            OfflinePlayer finalDestination = destination;
            Bukkit.getScheduler().runTaskAsynchronously(PetsPlugin.getInstance(), () -> {
                LoadedPlayer offlineLoadedPlayer = data.getPlayerInfo(finalDestination);
                if (offlineLoadedPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Failed to generate offline player profile");
                    return;
                }

                playerManager.onAdminLoadProfile(offlineLoadedPlayer);

                // Inventory opens must be handled synchronously
                Bukkit.getScheduler().runTask(PetsPlugin.getInstance(), () -> inventoryManager.getPetMenu().adminOpenInventory(player, offlineLoadedPlayer));
            });
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return null;
        }
        return Collections.emptyList();
    }
}

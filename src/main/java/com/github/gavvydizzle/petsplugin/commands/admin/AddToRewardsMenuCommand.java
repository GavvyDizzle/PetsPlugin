package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.mittenmc.serverutils.SubCommand;
import me.gavvydizzle.rewardsinventory.api.RewardsInventoryAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class AddToRewardsMenuCommand extends SubCommand {

    private final PetManager petManager;
    private final RewardsInventoryAPI rewardsInventoryAPI;

    public AddToRewardsMenuCommand(AdminCommandManager adminCommandManager, PetManager petManager) {
        this.petManager = petManager;
        rewardsInventoryAPI = RewardsInventoryAPI.getInstance();

        setName("addItem");
        setDescription("Adds a pet to the player's /rew pages inventory");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " addItem <player> <petID> <menuID> [xp]");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(getColoredSyntax());
            return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getPlayer(args[1]);
        if (offlinePlayer == null) {
            offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                sender.sendMessage(ChatColor.RED + args[1] + " is not a valid player.");
                return;
            }
        }

        Pet pet = petManager.getPet(args[2]);
        if (pet == null) {
            sender.sendMessage(ChatColor.RED + "No pet exists for the id: " + args[2].toLowerCase());
            return;
        }

        int pageMenuID = rewardsInventoryAPI.getMenuID(args[3]);
        if (pageMenuID == -1) {
            sender.sendMessage(ChatColor.RED + "No menu exists with the id: " + args[3]);
            return;
        }

        long xp = 0;
        if (args.length >= 5) {
            try {
                xp = Math.max(0, Long.parseLong(args[4]));
            }
            catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "'" + args[4] + " is not a valid xp amount");
                return;
            }
        }

        if (!rewardsInventoryAPI.addItem(offlinePlayer, pageMenuID, pet.getItemStack(offlinePlayer, xp))) {
            sender.sendMessage(ChatColor.RED + "Failed to add the item");
            return;
        }

        if (sender instanceof Player) {
            sender.sendMessage(ChatColor.GREEN + "Successfully put  a " + pet.getId() + " pet into " + offlinePlayer.getName() + "'s /rew " + args[3] + " menu");
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        ArrayList<String> list = new ArrayList<>();

        if (args.length == 2) {
            return null;
        }
        else if (args.length == 3) {
            StringUtil.copyPartialMatches(args[2], petManager.getPetIDs(), list);
        }
        else if (args.length == 4) {
            StringUtil.copyPartialMatches(args[3], rewardsInventoryAPI.getPageMenuNames(), list);
        }

        return list;
    }
}
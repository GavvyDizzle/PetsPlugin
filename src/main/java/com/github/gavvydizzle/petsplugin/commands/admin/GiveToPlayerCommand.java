package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class GiveToPlayerCommand extends SubCommand {

    private final PetManager petManager;

    public GiveToPlayerCommand(AdminCommandManager adminCommandManager, PetManager petManager) {
        this.petManager = petManager;

        setName("give");
        setDescription("Gives a pet to the player");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " give <player> <petID> [xp]");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(getColoredSyntax());
            return;
        }

        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Invalid player");
            return;
        }

        Pet pet = petManager.getPet(args[2]);
        if (pet == null) {
            sender.sendMessage(ChatColor.RED + "No pet exists for the id: " + args[2].toLowerCase());
            return;
        }

        long xp = 0;
        if (args.length >= 4) {
            try {
                xp = Math.max(0, Long.parseLong(args[3]));
            }
            catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "'" + args[3] + " is not a valid xp amount");
                return;
            }
        }

        player.getInventory().addItem(pet.getItemStack(player, xp));
        if (sender instanceof Player) {
            sender.sendMessage(ChatColor.GREEN + "Successfully gave " + player.getName() + " a " + pet.getId() + " pet");
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

        return list;
    }
}
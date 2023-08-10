package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class DeselectPetCommand extends SubCommand {

    private final PetManager petManager;

    public DeselectPetCommand(AdminCommandManager adminCommandManager, PetManager petManager) {
        this.petManager = petManager;

        setName("deselect");
        setDescription("Force deselect a pet (deletes if one was selected)");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " deselect <player>");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getColoredSyntax());
            return;
        }

        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Invalid player");
            return;
        }

        petManager.onPetSelect(player, null);
        sender.sendMessage(ChatColor.YELLOW + "Successfully removed " + player.getName() + "'s selected pet");
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return null;
        }
        return Collections.emptyList();
    }
}
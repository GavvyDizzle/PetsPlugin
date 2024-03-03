package com.github.gavvydizzle.petsplugin.commands;

import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class CommandConfirmationManager {

    private final HashMap<UUID, CommandConfirmation> commandConfirmations;

    public CommandConfirmationManager() {
        commandConfirmations = new HashMap<>();
    }

    public void attemptConfirmationCreation(SubCommand subCommand, String commandConfirmMessage, CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender) { // If the console sent the command then no confirmation is needed
            subCommand.perform(sender, args);
            return;
        }

        if (!(sender instanceof Player player)) return;

        if (commandConfirmations.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You already have a pending command");
        }
        else {
            player.sendMessage(commandConfirmMessage);
            commandConfirmations.put(player.getUniqueId(), new CommandConfirmation(subCommand, player, args));
        }
    }

    public void onConfirmCommandRun(Player player) {
        if (commandConfirmations.containsKey(player.getUniqueId())) {
            CommandConfirmation cc = commandConfirmations.remove(player.getUniqueId());
            cc.cancelTask();
            cc.getSubCommand().perform(cc.getCommandSender(), cc.getArgs());
        }
        else {
            player.sendMessage(ChatColor.RED + "You do not have a pending command");
        }
    }

    public void onConfirmExpire(Player player) {
        commandConfirmations.remove(player.getUniqueId());
        if (player.isOnline()) {
            player.sendMessage(ChatColor.RED + "Your confirmation has expired");
        }
    }

}

package com.github.gavvydizzle.petsplugin.commands.player;

import com.github.gavvydizzle.petsplugin.commands.PlayerCommandManager;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerHelpCommand extends SubCommand {

    private final PlayerCommandManager playerCommandManager;

    public PlayerHelpCommand(PlayerCommandManager playerCommandManager) {
        this.playerCommandManager = playerCommandManager;

        setName("help");
        setDescription("Opens this help menu");
        setSyntax("/" + playerCommandManager.getCommandDisplayName() + " help");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(playerCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        String padding = playerCommandManager.getHelpCommandPadding();

        if (!padding.isEmpty()) sender.sendMessage(padding);
        ArrayList<SubCommand> subCommands = playerCommandManager.getSubcommands();
        for (SubCommand subCommand : subCommands) {
            sender.sendMessage(ChatColor.GOLD + subCommand.getSyntax() + " - " + ChatColor.YELLOW + playerCommandManager.getPlayerDescription(subCommand));
        }
        if (!padding.isEmpty()) sender.sendMessage(padding);
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
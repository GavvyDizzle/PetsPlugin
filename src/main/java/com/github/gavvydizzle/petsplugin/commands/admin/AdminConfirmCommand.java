package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AdminConfirmCommand extends SubCommand {

    public AdminConfirmCommand(AdminCommandManager adminCommandManager) {
        setName("confirm");
        setDescription("Confirm an action");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " confirm");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;
        PetsPlugin.getInstance().getCommandConfirmationManager().onConfirmCommandRun(player);
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender commandSender, String[] strings) {
        return Collections.emptyList();
    }
}
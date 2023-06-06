package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.commands.ConfirmationCommand;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.storage.PlayerData;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ResetDatabaseCommand extends SubCommand implements ConfirmationCommand {

    private final PetManager petManager;
    private final PlayerData database;

    public ResetDatabaseCommand(AdminCommandManager adminCommandManager, PetManager petManager, PlayerData database) {
        this.petManager = petManager;
        this.database = database;

        setName("resetData");
        setDescription("Deletes all selected pets from the database");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " resetData");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        petManager.clearLoadedData();

        Bukkit.getServer().getScheduler().runTaskAsynchronously(PetsPlugin.getInstance(), () -> {
            if (database.deleteAllData()) {
                sender.sendMessage(ChatColor.GREEN + "Database reset successful");
            }
            else {
                sender.sendMessage(ChatColor.RED + "Database reset failed. Loaded data has already been deleted so some data has already been lost.");
            }
        });
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender commandSender, String[] strings) {
        return Collections.emptyList();
    }

    @Override
    public String getConfirmationMessage() {
        return ChatColor.RED + "Using this command will DELETE ALL SELECTED PETS! Only run `" + getSyntax() + "` if you are certain you want to continue";
    }
}

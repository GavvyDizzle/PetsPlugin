package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.utils.PDCUtils;
import com.github.mittenmc.serverutils.Numbers;
import com.github.mittenmc.serverutils.PlayerNameCache;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PetInfoCommand extends SubCommand {

    public PetInfoCommand(AdminCommandManager adminCommandManager) {
        setName("info");
        setDescription("Print out a pet's data");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " info");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "You must be holding a pet");
            return;
        }

        String petID = PDCUtils.getPetId(item);
        if (petID == null) {
            sender.sendMessage(ChatColor.RED + "You must be holding a pet");
            return;
        }

        String ownerName = PlayerNameCache.get(PDCUtils.getOwnerUUID(item));
        long xp = PDCUtils.getXP(item);

        player.sendMessage(ChatColor.GREEN + "Pet Info:");
        player.sendMessage(ChatColor.YELLOW + " id=" + petID);
        player.sendMessage(ChatColor.YELLOW + " owner=" + ownerName);
        player.sendMessage(ChatColor.YELLOW + " xp=" + Numbers.withSuffix(xp));
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
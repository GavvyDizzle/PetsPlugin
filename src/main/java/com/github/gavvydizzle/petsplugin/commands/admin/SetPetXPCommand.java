package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.utils.PDCUtils;
import com.github.mittenmc.serverutils.Numbers;
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

public class SetPetXPCommand extends SubCommand {

    private final PetManager petManager;

    public SetPetXPCommand(AdminCommandManager adminCommandManager, PetManager petManager) {
        this.petManager = petManager;

        setName("setXP");
        setDescription("Update your held pet's XP");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " setXP <xp>");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage(getColoredSyntax());
            return;
        }

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

        Pet pet = petManager.getPet(petID);
        if (pet == null) {
            sender.sendMessage(ChatColor.RED + "You must be holding a pet");
            return;
        }

        long xp;
        try {
            xp = Long.parseLong(args[1]);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Invalid xp amount: " + args[1]);
            return;
        }
        if (xp < 0) {
            sender.sendMessage(ChatColor.RED + "Invalid xp amount: " + xp);
            return;
        }

        ItemStack newPet = pet.getItemStack(Bukkit.getOfflinePlayer(Objects.requireNonNull(PDCUtils.getOwnerUUID(item))), xp);
        newPet.setAmount(item.getAmount());

        sender.sendMessage(ChatColor.GREEN + "Updated pet xp to " + xp + " (was " + PDCUtils.getXP(item) + ")");
        ((Player) sender).getInventory().setItemInMainHand(newPet);
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
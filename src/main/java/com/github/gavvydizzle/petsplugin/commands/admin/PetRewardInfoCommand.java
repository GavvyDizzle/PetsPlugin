package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.pets.reward.RewardManager;
import com.github.gavvydizzle.petsplugin.pets.reward.RewardsSet;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class PetRewardInfoCommand extends SubCommand {

    private final PetManager petManager;

    public PetRewardInfoCommand(AdminCommandManager adminCommandManager, PetManager petManager) {
        this.petManager = petManager;

        setName("rewardInfo");
        setDescription("Print out a pet's reward information");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " rewardInfo <petID> <rewardID>");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getColoredSyntax());
            return;
        }

        Pet pet = petManager.getPet(args[1]);
        if (pet == null) {
            sender.sendMessage(ChatColor.RED + "No pet exists for the id: " + args[1].toLowerCase());
            return;
        }

        RewardManager rewardManager = pet.getRewardManager();
        int numRewards = rewardManager.getNumRewards();

        // Tell the sender how many rewards this pet has
        if (args.length == 2) {
            sender.sendMessage(ChatColor.YELLOW + "Pet " + pet.getId() + " has " + numRewards + " reward(s) loaded");
            return;
        }

        int rewardID;
        try {
            rewardID = Integer.parseInt(args[2]);
        } catch (Exception ignored) {
            sender.sendMessage(ChatColor.RED + "Invalid rewardID. It must be a number between 1 and " + numRewards);
            return;
        }
        if (rewardID <= 0 || rewardID > numRewards) {
            sender.sendMessage(ChatColor.RED + "Invalid rewardID. It must be a number between 1 and " + numRewards);
            return;
        }

        RewardsSet rewardsSet = rewardManager.getRewardsSet(rewardID-1);
        if (rewardsSet == null) {
            sender.sendMessage(ChatColor.RED + "Invalid rewardID. It must be a number between 1 and " + numRewards);
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Pet: " + pet.getId());
        rewardsSet.printRewards(sender);
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        ArrayList<String> list = new ArrayList<>();

        if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], petManager.getPetIDs(), list);
        }

        return list;
    }
}

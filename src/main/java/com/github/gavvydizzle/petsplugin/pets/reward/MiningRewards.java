package com.github.gavvydizzle.petsplugin.pets.reward;

import com.github.mittenmc.serverutils.Colors;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;

public class MiningRewards extends RewardsSet {

    @Nullable private final HashSet<Material> materials;

    public MiningRewards(double rewardChance, @Nullable ArrayList<Material> materials) {
        super(rewardChance);

        if (materials == null || materials.isEmpty()) {
            this.materials = null;
        }
        else {
            this.materials = new HashSet<>(materials);
        }
    }

    /**
     * Attempts to give a reward to the player.
     * @param player The player
     * @param material The material of the mined block
     */
    public void giveReward(Player player, Material material) {
        if (shouldNotGiveReward()) return;
        if (materials != null && !materials.contains(material)) return;

        giveReward(player);
    }


    @Override
    public void printRewards(CommandSender sender) {
        sender.sendMessage(Colors.conv("&e" + rewards.size() + " MINE Rewards"));
        sender.sendMessage(Colors.conv("&aReward Chance: " + Math.min(rewardPercent, 100) + "%"));
        sender.sendMessage(Colors.conv("&7Total Weight: " + totalWeight));
        for (Reward reward : rewards) {
            sender.sendMessage("");
            sender.sendMessage(Colors.conv(" &a[" + reward.getId() + "] - &7Weight: " + reward.getWeight()));
            sender.sendMessage(Colors.conv(" &fMessages:"));
            for (String message : reward.getMessages()) {
                sender.sendMessage("  " + message);
            }
            sender.sendMessage(Colors.conv(" &fCommands:"));
            for (String command : reward.getCommands()) {
                sender.sendMessage(Colors.conv("  &e/" + command));
            }
        }
        sender.sendMessage("");
    }
}

package com.github.gavvydizzle.petsplugin.pets.reward;

import com.github.gavvydizzle.petsplugin.player.LoadedPlayer;
import com.github.mittenmc.serverutils.Colors;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;

public class KillRewards extends RewardsSet {

    @Nullable private final HashSet<EntityType> entityTypes;

    public KillRewards(double rewardChance, @Nullable ArrayList<EntityType> entityTypes) {
        super(rewardChance);

        if (entityTypes == null || entityTypes.isEmpty()) {
            this.entityTypes = null;
        }
        else {
            this.entityTypes = new HashSet<>(entityTypes);
        }
    }

    /**
     * Attempts to give a reward to the player.
     * @param loadedPlayer The player
     * @param entityType The type of entity that was killed
     */
    public void giveReward(LoadedPlayer loadedPlayer, EntityType entityType) {
        if (shouldNotGiveReward()) return;
        if (entityTypes != null && !entityTypes.contains(entityType)) return;

        giveReward(loadedPlayer);
    }

    @Override
    public void printRewards(CommandSender sender) {
        sender.sendMessage(Colors.conv("&e-----(" + rewards.size() + " KILL Rewards)-----"));
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

package com.github.gavvydizzle.petsplugin.pets.reward;

import com.github.mittenmc.serverutils.Numbers;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public abstract class RewardsSet {

    protected double rewardPercent;
    protected int totalWeight;
    protected final ArrayList<Reward> rewards;

    public RewardsSet(double rewardChance) {
        this.rewardPercent = rewardChance * 100;
        rewards = new ArrayList<>();
    }

    /**
     * Registers a reward to this set of rewards
     * @param reward The reward
     */
    public void registerReward(Reward reward) {
        rewards.add(reward);
        totalWeight += reward.getWeight();
    }

    /**
     * @return If this reward attempt should not give a reward
     */
    protected boolean shouldNotGiveReward() {
        return !Numbers.percentChance(rewardPercent);
    }

    /**
     * Gives a reward without doing any checks
     * @param player The player
     */
    protected void giveReward(Player player) {
        int num = Numbers.randomNumber(1, totalWeight);

        for (Reward reward : rewards) {
            if (num <= reward.getWeight()) {
                reward.collect(player);
                return;
            }
            num -= reward.getWeight();
        }
    }

    /**
     * Print out the rewards of this set to chat
     * @param sender Who to print the message for
     */
    public abstract void printRewards(CommandSender sender);

}

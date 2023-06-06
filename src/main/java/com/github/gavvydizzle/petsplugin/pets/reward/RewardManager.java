package com.github.gavvydizzle.petsplugin.pets.reward;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class RewardManager {

    private final ArrayList<RewardsSet> arrayList;
    private int numMiningRewards, numKillingRewards;

    public RewardManager() {
        arrayList = new ArrayList<>();
        numMiningRewards = 0;
        numKillingRewards = 0;
    }

    public void registerRewardSet(RewardsSet rewardsSet) {
        arrayList.add(rewardsSet);

        if (rewardsSet instanceof MiningRewards) {
            numMiningRewards++;
        }
        else {
            numKillingRewards++;
        }
    }

    /**
     * Attempts to reward the player for all mining based rewards for this pet
     * @param player The player
     * @param material The material of the mined block
     */
    public void attemptMiningReward(Player player, Material material) {
        if (numMiningRewards == 0) return;

        for (RewardsSet rewardsSet : arrayList) {
            if (rewardsSet instanceof MiningRewards) {
                ((MiningRewards) rewardsSet).giveReward(player, material);
            }
        }
    }

    /**
     * Attempts to reward the player for all killing based rewards for this pet
     * @param player The player
     * @param entityType The type of entity killed
     */
    public void attemptKillReward(Player player, EntityType entityType) {
        if (numKillingRewards == 0) return;

        for (RewardsSet rewardsSet : arrayList) {
            if (rewardsSet instanceof KillRewards) {
                ((KillRewards) rewardsSet).giveReward(player, entityType);
            }
        }
    }

    @Nullable
    public RewardsSet getRewardsSet(int index) {
        if (index < 0 || index >= arrayList.size()) return null;
        return arrayList.get(index);
    }

    public int getNumRewards() {
        return arrayList.size();
    }

}

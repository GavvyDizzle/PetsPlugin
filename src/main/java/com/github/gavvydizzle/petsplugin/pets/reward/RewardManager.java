package com.github.gavvydizzle.petsplugin.pets.reward;

import com.github.gavvydizzle.petsplugin.player.LoadedPlayer;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class RewardManager {

    private final ArrayList<MiningRewards> miningRewards;
    private final ArrayList<KillRewards> killRewards;

    public RewardManager() {
        miningRewards = new ArrayList<>();
        killRewards = new ArrayList<>();
    }

    public void registerRewardSet(RewardsSet rewardsSet) {
        if (rewardsSet instanceof MiningRewards) {
            miningRewards.add((MiningRewards) rewardsSet);
        }
        else {
            killRewards.add((KillRewards) rewardsSet);
        }
    }

    /**
     * Attempts to reward the player for all mining based rewards for this pet
     * @param loadedPlayer The player
     * @param material The material of the mined block
     */
    public void attemptMiningReward(LoadedPlayer loadedPlayer, Material material) {
        if (miningRewards.isEmpty()) return;

        for (MiningRewards rewardsSet : miningRewards) {
            rewardsSet.giveReward(loadedPlayer, material);
        }
    }

    /**
     * Attempts to reward the player for all killing based rewards for this pet
     * @param loadedPlayer The player
     * @param entityType The type of entity killed
     */
    public void attemptKillReward(LoadedPlayer loadedPlayer, EntityType entityType) {
        if (killRewards.isEmpty()) return;

        for (KillRewards rewardsSet : killRewards) {
            rewardsSet.giveReward(loadedPlayer, entityType);
        }
    }

    @Nullable
    public RewardsSet getRewardsSet(int index) {
        if (index < 0 || index >= getNumRewards()) return null;

        if (index < miningRewards.size()) {
            return miningRewards.get(index);
        }

        index -= killRewards.size();
        return killRewards.get(index);
    }

    public int getNumRewards() {
        return miningRewards.size() + killRewards.size();
    }

}

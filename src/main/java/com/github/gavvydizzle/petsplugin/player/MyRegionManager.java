package com.github.gavvydizzle.petsplugin.player;

import com.github.gavvydizzle.petsplugin.pets.reward.RewardType;
import com.github.gavvydizzle.petsplugin.pets.xp.ExperienceType;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

public class MyRegionManager {

    private static Map<ExperienceType, StateFlag> experienceFlagMap;
    private static Map<RewardType, StateFlag> rewardsFlagMap;
    private RegionQuery query;

    public static void initFlags() {
        experienceFlagMap = new HashMap<>();
        rewardsFlagMap = new HashMap<>();

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        for (ExperienceType experienceType : ExperienceType.values()) {
            try {
                StateFlag flag = new StateFlag(experienceType.flagName, false);
                registry.register(flag);
                experienceFlagMap.put(experienceType, flag);
            }  catch (IllegalStateException | FlagConflictException e) {
                // If the plugin is reloaded dynamically then this will grab the existing flag
                // ... or another plugin registered it already
                Flag<?> existing = registry.get(experienceType.flagName);
                if (existing instanceof StateFlag) {
                    experienceFlagMap.put(experienceType, (StateFlag) existing);
                }
            }
        }

        for (RewardType rewardType : RewardType.values()) {
            try {
                StateFlag flag = new StateFlag(rewardType.flagName, false);
                registry.register(flag);
                rewardsFlagMap.put(rewardType, flag);
            }  catch (IllegalStateException | FlagConflictException e) {
                // If the plugin is reloaded dynamically then this will grab the existing flag
                // ... or another plugin registered it already
                Flag<?> existing = registry.get(rewardType.flagName);
                if (existing instanceof StateFlag) {
                    rewardsFlagMap.put(rewardType, (StateFlag) existing);
                }
            }
        }
    }

    private void initQuery() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        query = container.createQuery();
    }

    /**
     * @param experienceType The xp type
     * @param block The block
     * @return If the player should not receive pet experience for an action here
     */
    public boolean isNotInXpRegion(ExperienceType experienceType, Block block) {
        return isNotInXpRegion(experienceType, block.getLocation());
    }

    /**
     * @param experienceType The xp type
     * @param location The location
     * @return If the player should not receive pet experience for an action here
     */
    public boolean isNotInXpRegion(ExperienceType experienceType, Location location) {
        if (query == null) {
            initQuery();
        }

        StateFlag stateFlag = experienceFlagMap.get(experienceType);
        if (stateFlag == null) return true;

        return !query.testState(BukkitAdapter.adapt(location), null, stateFlag);
    }

    /**
     * @param rewardType The reward type
     * @param block The block
     * @return If the player should not receive pet rewards for an action here
     */
    public boolean isNotInRewardRegion(RewardType rewardType, Block block) {
        return isNotInRewardRegion(rewardType, block.getLocation());
    }

    /**
     * @param rewardType The reward type
     * @param location The location
     * @return If the player should not receive pet rewards for an action here
     */
    public boolean isNotInRewardRegion(RewardType rewardType, Location location) {
        if (query == null) {
            initQuery();
        }

        StateFlag stateFlag = rewardsFlagMap.get(rewardType);
        if (stateFlag == null) return true;

        return !query.testState(BukkitAdapter.adapt(location), null, stateFlag);
    }
}

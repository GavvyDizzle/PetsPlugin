package com.github.gavvydizzle.petsplugin.pets.boost;

import com.github.mittenmc.serverutils.Numbers;

public class RewardDoublerBoost extends Boost {

    private final String rewardID;
    private final String percentChanceEquation;

    public RewardDoublerBoost(String id, String rewardID, String percentChanceEquation) {
        super(BoostType.DOUBLE_REWARD, id);
        this.rewardID = rewardID;
        this.percentChanceEquation = percentChanceEquation;
    }

    /**
     * @param level The level of the pet
     * @return The percent chance this has to activate
     */
    @Override
    public String getPlaceholderAmount(int level) {
        return String.valueOf(Numbers.round(Numbers.eval(percentChanceEquation.replace("x", String.valueOf(level))), DECIMAL_PLACES));
    }

    public String getRewardID() {
        return rewardID;
    }

    /**
     * Randomly decides if this boost should activate or not
     * @return If this boost should activate
     */
    public boolean shouldActivate(int level) {
        return Numbers.percentChance(Numbers.eval(percentChanceEquation.replace("x", String.valueOf(level))));
    }
}
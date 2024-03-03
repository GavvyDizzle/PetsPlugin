package com.github.gavvydizzle.petsplugin.pets.reward;

import javax.annotation.Nullable;

public enum RewardType {
    KILLING("pets-killing-rewards"),
    MINING("pets-mining-rewards");

    public final String flagName;

    RewardType(String s) {
        this.flagName = s;
    }

    /**
     * @param str The string
     * @return The enum matching this string or null
     */
    @Nullable
    public static RewardType get(@Nullable String str) {
        if (str == null) return null;

        for (RewardType type : RewardType.values()) {
            if (type.toString().equalsIgnoreCase(str)) {
                return type;
            }
        }
        return null;
    }
}
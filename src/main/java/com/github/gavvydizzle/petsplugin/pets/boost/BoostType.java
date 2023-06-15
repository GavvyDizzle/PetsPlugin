package com.github.gavvydizzle.petsplugin.pets.boost;

import javax.annotation.Nullable;

public enum BoostType {

    DAMAGE,
    DOUBLE_REWARD,
    GENERAL_REWARD,
    ENCHANT,
    POTION_EFFECT,
    XP;

    /**
     * @param str The string
     * @return The BoostType matching this string or null
     */
    @Nullable
    public static BoostType getBoostType(@Nullable String str) {
        if (str == null) return null;

        for (BoostType boostType : BoostType.values()) {
            if (boostType.toString().equalsIgnoreCase(str)) {
                return boostType;
            }
        }
        return null;
    }

}

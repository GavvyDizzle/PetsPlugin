package com.github.gavvydizzle.petsplugin.pets.xp;

import javax.annotation.Nullable;

/**
 * Defines the different ways pets can earn XP
 */
public enum ExperienceType {
    KILLING("pets-killing-xp"),
    MINING("pets-mining-xp");

    public final String flagName;

    ExperienceType(String s) {
        this.flagName = s;
    }

    /**
     * @param str The string
     * @return The enum matching this string or null
     */
    @Nullable
    public static ExperienceType get(@Nullable String str) {
        if (str == null) return null;

        for (ExperienceType type : ExperienceType.values()) {
            if (type.toString().equalsIgnoreCase(str)) {
                return type;
            }
        }
        return null;
    }
}

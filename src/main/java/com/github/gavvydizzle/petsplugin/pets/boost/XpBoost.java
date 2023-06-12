package com.github.gavvydizzle.petsplugin.pets.boost;

import com.github.mittenmc.serverutils.Numbers;

public class XpBoost extends Boost {

    private final String multiplierEquation;
    private final boolean isMultiplicative;

    public XpBoost(String id, String multiplierEquation, boolean isMultiplicative) {
        super(BoostType.XP, id);
        this.multiplierEquation = multiplierEquation;
        this.isMultiplicative = isMultiplicative;
    }

    /**
     * @param level The level of the pet
     * @return The calculated multiplier as a percent chance
     */
    @Override
    public String getPlaceholderAmount(int level) {
        return String.valueOf(Numbers.round(getMultiplier(level) * 100, DECIMAL_PLACES));
    }

    public double getMultiplier(int level) {
        return Numbers.eval(multiplierEquation.replace("x", String.valueOf(level)));
    }

    public boolean isMultiplicative() {
        return isMultiplicative;
    }
}

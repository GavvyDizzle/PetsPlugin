package com.github.gavvydizzle.petsplugin.pets.boost;

import com.github.mittenmc.serverutils.Numbers;
import me.wax.prisonenchants.enchants.EnchantIdentifier;

public class EnchantBoost extends Boost {

    private final EnchantIdentifier enchantIdentifier;
    private final String multiplierEquation;
    private final boolean isMultiplicative;

    public EnchantBoost(String id, EnchantIdentifier enchantIdentifier, String multiplierEquation, boolean isMultiplicative) {
        super(BoostType.ENCHANT, id);
        this.enchantIdentifier = enchantIdentifier;
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

    public EnchantIdentifier getEnchantIdentifier() {
        return enchantIdentifier;
    }

    public double getMultiplier(int level) {
        return Numbers.eval(multiplierEquation.replace("x", String.valueOf(level)));
    }

    public boolean isMultiplicative() {
        return isMultiplicative;
    }
}

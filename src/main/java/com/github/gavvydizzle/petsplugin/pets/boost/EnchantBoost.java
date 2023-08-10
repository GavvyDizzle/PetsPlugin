package com.github.gavvydizzle.petsplugin.pets.boost;

import com.github.mittenmc.serverutils.Numbers;
import me.wax.prisonenchants.enchants.EnchantIdentifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

public class EnchantBoost extends Boost {

    private final HashSet<EnchantIdentifier> enchantIdentifiers;
    private final String multiplierEquation;
    private final boolean isMultiplicative;

    public EnchantBoost(String id, @Nullable List<EnchantIdentifier> enchantIdentifiers, String multiplierEquation, boolean isMultiplicative) {
        super(BoostType.ENCHANT, id);
        if (enchantIdentifiers == null || enchantIdentifiers.isEmpty()) {
            this.enchantIdentifiers = null;
        }
        else {
            this.enchantIdentifiers = new HashSet<>(enchantIdentifiers);
        }
        this.multiplierEquation = multiplierEquation;
        this.isMultiplicative = isMultiplicative;
    }

    /**
     * Determines if the enchant matches the type for this boost
     * @param enchantIdentifier The type of enchant that activated
     * @return If activation chance should be multiplied by this boost
     */
    public boolean shouldBoostEnchant(EnchantIdentifier enchantIdentifier) {
        return enchantIdentifiers == null || enchantIdentifiers.contains(enchantIdentifier);
    }

    /**
     * @param level The level of the pet
     * @return The calculated multiplier as a percent chance
     */
    @Override
    public String getPlaceholderAmount(int level) {
        return String.valueOf(Numbers.round(getMultiplier(level), DECIMAL_PLACES));
    }

    public double getMultiplier(int level) {
        return Numbers.eval(multiplierEquation.replace("x", String.valueOf(level)));
    }

    public boolean isMultiplicative() {
        return isMultiplicative;
    }
}

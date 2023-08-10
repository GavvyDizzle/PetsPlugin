package com.github.gavvydizzle.petsplugin.pets.boost;

import com.github.mittenmc.serverutils.Numbers;
import org.bukkit.entity.EntityType;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;

public class DamageBoost extends Boost {

    @Nullable
    private final HashSet<EntityType> entityTypes;
    private final String multiplierEquation;

    public DamageBoost(String id, @Nullable List<EntityType> entityTypes, String multiplierEquation) {
        super(BoostType.DAMAGE, id);
        if (entityTypes == null || entityTypes.isEmpty()) {
            this.entityTypes = null;
        }
        else {
            this.entityTypes = new HashSet<>(entityTypes);
        }
        this.multiplierEquation = multiplierEquation;
    }

    /**
     * Determines if the entity matches the type for this boost
     * @param entityType The type of entity that was damaged
     * @return If damage should be multiplied by this boost
     */
    public boolean shouldBoostDamage(EntityType entityType) {
        return entityTypes == null || entityTypes.contains(entityType);
    }

    /**
     * @param level The level of the pet
     * @return The calculated multiplier as a percent chance
     */
    @Override
    public String getPlaceholderAmount(int level) {
        // Hard coded to get % from the boost
        return String.valueOf(Numbers.round(getMultiplier(level) * 100 - 100, DECIMAL_PLACES));
    }

    public double getMultiplier(int level) {
        return Numbers.eval(multiplierEquation.replace("x", String.valueOf(level)));
    }
}
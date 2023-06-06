package com.github.gavvydizzle.petsplugin.pets.boost;

public abstract class Boost {

    protected final int DECIMAL_PLACES = 4;
    private final String id;
    private final BoostType boostType;

    public Boost(BoostType boostType, String id) {
        this.boostType = boostType;
        this.id = id;
    }

    /**
     * @param level The level of the pet
     * @return A string representing the variable of this boost
     */
    public abstract String getPlaceholderAmount(int level);

    public BoostType getBoostType() {
        return boostType;
    }

    public String getId() {
        return id;
    }
}

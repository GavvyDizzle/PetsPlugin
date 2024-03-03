package com.github.gavvydizzle.petsplugin.pets.boost;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectBoost extends Boost {

    private final PotionEffect potionEffect;

    public PotionEffectBoost(String id, PotionEffectType potionEffectType, int amplifier) {
        super(BoostType.POTION_EFFECT, id);
        potionEffect = new PotionEffect(potionEffectType, PotionEffect.INFINITE_DURATION, amplifier);
    }

    /**
     * @param level The level of the pet
     * @return The effect level (amplifier + 1)
     */
    @Override
    public String getPlaceholderAmount(int level) {
        return String.valueOf(potionEffect.getAmplifier() + 1);
    }

    public void applyPotionEffect(Player player) {
        player.addPotionEffect(potionEffect);
    }

    public void removePotionEffect(Player player) {
        player.removePotionEffect(potionEffect.getType());
    }
}
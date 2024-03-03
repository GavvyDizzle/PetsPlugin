package com.github.gavvydizzle.petsplugin.utils;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.mittenmc.serverutils.Colors;
import org.bukkit.configuration.file.FileConfiguration;

public class Messages {

    public static String petLevelUpMessage;
    public static String invalidPetSelect, notPetOwnerOnSelect;
    public static String petInvalidAfterReload;

    public static void reloadMessages() {
        FileConfiguration config = PetsPlugin.getConfigManager().get("messages");
        if (config == null) return;

        config.addDefault("petLevelUpMessage", "&aYour {pet_name}&a just levelled up!");
        config.addDefault("invalidPetSelect", "&cUnable to select this pet (id={id}). Please alert a staff member!");
        config.addDefault("notPetOwnerOnSelect", "&cYou can only select a pet that belongs to you!");
        config.addDefault("petInvalidAfterReload", "&cYour pet became invalid after a plugin reload! You will still receive XP, but no boosts will be active. (id={id})");

        petLevelUpMessage = Colors.conv(config.getString("petLevelUpMessage"));
        invalidPetSelect = Colors.conv(config.getString("invalidPetSelect"));
        notPetOwnerOnSelect = Colors.conv(config.getString("notPetOwnerOnSelect"));
        petInvalidAfterReload = Colors.conv(config.getString("petInvalidAfterReload"));
    }
}

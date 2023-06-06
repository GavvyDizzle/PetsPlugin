package com.github.gavvydizzle.petsplugin.utils;

import com.github.gavvydizzle.petsplugin.configs.MessagesConfig;
import com.github.mittenmc.serverutils.Colors;
import org.bukkit.configuration.file.FileConfiguration;

public class Messages {

    public static String petLevelUpMessage;
    public static String inventoryFullOnPetSelect, inventoryFullOnPetSwap, invalidPetSelect, selectedPetInvalid, notPetOwnerOnSelect;
    public static String swappingPetTooFast;
    public static String petInvalidAfterReload;

    public static void reloadMessages() {
        FileConfiguration config = MessagesConfig.get();
        config.options().copyDefaults(true);

        config.addDefault("petLevelUpMessage", "&aYour {pet_name}&a just levelled up!");

        config.addDefault("inventoryFullOnPetSelect", "&cInventory full. Cannot retrieve pet");
        config.addDefault("inventoryFullOnPetSwap", "&cInventory full. Cannot swap selected pet");
        config.addDefault("invalidPetSelect", "&cUnable to select this pet (id={id}). Please alert a staff member!");
        config.addDefault("selectedPetInvalid", "&cYour selected pet is invalid (id={id}). Please alert a staff member!");
        config.addDefault("notPetOwnerOnSelect", "&cYou can only select a pet that belongs to you!");

        config.addDefault("swappingPetTooFast", "&cPlease wait to swap your pet!");

        config.addDefault("petInvalidAfterReload", "&cYour pet became invalid after a plugin reload! You will still receive XP, but no boosts will be active. (id={id})");

        MessagesConfig.save();

        petLevelUpMessage = Colors.conv(config.getString("petLevelUpMessage"));

        inventoryFullOnPetSelect = Colors.conv(config.getString("inventoryFullOnPetSelect"));
        inventoryFullOnPetSwap = Colors.conv(config.getString("inventoryFullOnPetSwap"));
        invalidPetSelect = Colors.conv(config.getString("invalidPetSelect"));
        selectedPetInvalid = Colors.conv(config.getString("selectedPetInvalid"));
        notPetOwnerOnSelect = Colors.conv(config.getString("notPetOwnerOnSelect"));

        swappingPetTooFast = Colors.conv(config.getString("swappingPetTooFast"));

        petInvalidAfterReload = Colors.conv(config.getString("petInvalidAfterReload"));
    }
}

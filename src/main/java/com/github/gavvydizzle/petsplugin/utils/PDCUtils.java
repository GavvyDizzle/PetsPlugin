package com.github.gavvydizzle.petsplugin.utils;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.mittenmc.serverutils.UUIDConverter;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * How data is stored:<p>
 * pet_id: string<p>
 * owner: uuid<p>
 * xp: long
 */
public class PDCUtils {

    private static final NamespacedKey petIdKey = new NamespacedKey(PetsPlugin.getInstance(), "id");
    private static final NamespacedKey ownerKey = new NamespacedKey(PetsPlugin.getInstance(), "owner");
    private static final NamespacedKey xpKey = new NamespacedKey(PetsPlugin.getInstance(), "xp");
    private static final NamespacedKey randKey = new NamespacedKey(PetsPlugin.getInstance(), "rand");

    /**
     * Gets the pet ID stored on this item
     * @param item The ItemStack
     * @return The pet's ID or null if none exists
     */
    @Nullable
    public static String getPetId(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(petIdKey, PersistentDataType.STRING);
    }

    /**
     * Sets the pet ID of this item
     * @param item The ItemStack
     * @param pet The Pet
     */
    public static void setPetId(@NotNull ItemStack item, @NotNull Pet pet) {
        if (item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(petIdKey, PersistentDataType.STRING, pet.getId());
        item.setItemMeta(meta);
    }

    /**
     * Gets the owner's UUID stored on this item
     * @param item The ItemStack
     * @return The owner's UUID or null if none exists
     */
    @Nullable
    public static UUID getOwnerUUID(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return null;

        byte[] arr = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.BYTE_ARRAY);
        if (arr == null) return null;

        return UUIDConverter.convert(arr);
    }

    /**
     * Sets the pet ID of this item
     * @param item The ItemStack
     * @param player The player
     */
    public static void setOwner(@NotNull ItemStack item, @NotNull OfflinePlayer player) {
        if (item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.BYTE_ARRAY, UUIDConverter.convert(player.getUniqueId()));
        item.setItemMeta(meta);
    }

    /**
     * Gets the XP stored on this item
     * @param item The ItemStack
     * @return The pet's XP or -1 if none found
     */
    public static long getXP(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return -1;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(xpKey, PersistentDataType.LONG, -1L);
    }

    /**
     * Sets the XP stored on this item
     * @param item The ItemStack
     * @param amount The amount of XP to set to
     */
    public static void setXP(@NotNull ItemStack item, long amount) {
        if (item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(xpKey, PersistentDataType.LONG, amount);
        item.setItemMeta(meta);
    }

    /**
     * Increments the XP stored on this item
     * @param item The ItemStack
     * @param amount The amount of XP to add
     */
    public static void incrementXP(@NotNull ItemStack item, long amount) {
        if (item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(xpKey, PersistentDataType.LONG,
                meta.getPersistentDataContainer().getOrDefault(xpKey, PersistentDataType.LONG, 0L) + amount);
        item.setItemMeta(meta);
    }

    public static void setRandomKey(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(randKey, PersistentDataType.BYTE_ARRAY, UUIDConverter.convert(UUID.randomUUID()));
        item.setItemMeta(meta);
    }
}

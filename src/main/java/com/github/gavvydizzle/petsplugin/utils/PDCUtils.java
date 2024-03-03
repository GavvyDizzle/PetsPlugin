package com.github.gavvydizzle.petsplugin.utils;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.SelectedPet;
import com.github.mittenmc.serverutils.UUIDConverter;
import org.bukkit.NamespacedKey;
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
    private static final NamespacedKey useTimeKey = new NamespacedKey(PetsPlugin.getInstance(), "use_time");
    private static final NamespacedKey randKey = new NamespacedKey(PetsPlugin.getInstance(), "rand");

    @Nullable
    public static SelectedPet createSelectedPet(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return null;

        String id = getPetId(itemStack);
        if (id == null) return null;

        return new SelectedPet(getPetId(itemStack), Math.max(0, getXP(itemStack)), getLastUseTime(itemStack));
    }

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
     * Determines if this item has pet data
     * @param item The ItemStack
     * @return True if this item is a pet, false if not
     */
    public static boolean isPet(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(petIdKey, PersistentDataType.STRING);
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
     * @param uuid The player's uuid
     */
    public static void setOwner(@NotNull ItemStack item, UUID uuid) {
        if (item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.BYTE_ARRAY, UUIDConverter.convert(uuid));
        item.setItemMeta(meta);
    }

    /**
     * Gets the XP stored on this item
     * @param item The ItemStack
     * @return The pet's XP or -1 if none found
     */
    public static double getXP(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return -1;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(xpKey, PersistentDataType.DOUBLE, -1.0);
    }

    /**
     * Sets the XP stored on this item
     * @param item The ItemStack
     * @param amount The amount of XP to set to
     */
    public static void setXP(@NotNull ItemStack item, double amount) {
        if (item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(xpKey, PersistentDataType.DOUBLE, amount);
        item.setItemMeta(meta);
    }

    /**
     * Checks the item to see if it has last use data
     * @param item The ItemStack
     * @return True if this item has a timestamp attached to it
     */
    public static boolean hasTimeKey(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(useTimeKey, PersistentDataType.LONG);
    }

    /**
     * Removes the time data from this item
     * @param item The ItemStack
     */
    public static void removeTimeKey(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(useTimeKey);
        item.setItemMeta(meta);
    }

    /**
     * Gets the last use time of this item
     * @param item The ItemStack
     * @return The time in milliseconds since the last use
     */
    public static long getLastUseTime(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return -1;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(useTimeKey, PersistentDataType.LONG, 0L);
    }

    /**
     * Gets the last use time of this item
     * @param item The ItemStack
     * @return The time in milliseconds since the last use
     */
    public static long getTimeSinceLastUse(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return -1;
        return System.currentTimeMillis() - item.getItemMeta().getPersistentDataContainer().getOrDefault(useTimeKey, PersistentDataType.LONG, 0L);
    }

    /**
     * Sets the use time of this item
     * @param item The ItemStack
     * @param useTime The time to set
     */
    public static void setUseTime(@NotNull ItemStack item, long useTime) {
        if (item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(useTimeKey, PersistentDataType.LONG, useTime);
        item.setItemMeta(meta);
    }

    public static void setRandomKey(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(randKey, PersistentDataType.BYTE_ARRAY, UUIDConverter.convert(UUID.randomUUID()));
        item.setItemMeta(meta);
    }
}

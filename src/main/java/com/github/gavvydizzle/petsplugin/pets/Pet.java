package com.github.gavvydizzle.petsplugin.pets;

import com.github.gavvydizzle.petsplugin.pets.boost.Boost;
import com.github.gavvydizzle.petsplugin.pets.boost.BoostType;
import com.github.gavvydizzle.petsplugin.pets.reward.RewardManager;
import com.github.gavvydizzle.petsplugin.pets.xp.ExperienceType;
import com.github.gavvydizzle.petsplugin.utils.PDCUtils;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.Numbers;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pet implements Comparable<Pet> {

    private final Pattern pattern = Pattern.compile("\\{boost_[A-Za-z0-9]+}");
    private final Pattern patternWithLevel = Pattern.compile("\\{boost_[A-Za-z0-9]+_[0-9]+}");

    private final String id;
    private final long lockDurationMilliseconds;
    private final String permission, permissionDeniedMessage;
    private final ItemStack itemStack, listItemStack;
    private final int startLevel, maxLevel;
    private final long[] xpPerLevel, totalXpNeeded;
    private final HashMap<BoostType, ArrayList<Boost>> boostsMap;
    private final HashMap<String, Boost> boostsByID;
    private final Map<ExperienceType, Set<Object>> xpMap;
    private final RewardManager rewardManager;

    public Pet(String id,
               int lockDurationSeconds,
               String permission,
               String permissionDeniedMessage,
               ItemStack itemStack,
               ItemStack listItemStack,
               int startLevel,
               int maxLevel,
               List<Long> totalXp,
               Map<ExperienceType, Set<Object>> xpMap,
               @Nullable RewardManager rewardManager) {

        this.id = id.toLowerCase();
        this.lockDurationMilliseconds = lockDurationSeconds * 1000L;
        this.permission = permission.trim();
        this.permissionDeniedMessage = permissionDeniedMessage;
        this.startLevel = startLevel;
        this.maxLevel = maxLevel;

        // There will always be enough levels in the provided list to fill the arrays
        xpPerLevel = new long[maxLevel - startLevel];
        for (int i = 0; i < xpPerLevel.length; i++) {
            xpPerLevel[i] = totalXp.get(i);
        }

        totalXpNeeded = new long[xpPerLevel.length];
        if (totalXpNeeded.length > 0) totalXpNeeded[0] = xpPerLevel[0];
        for (int i = 1; i < xpPerLevel.length; i++) {
            totalXpNeeded[i] = totalXpNeeded[i-1] + xpPerLevel[i];
        }

        this.boostsMap = new HashMap<>();
        boostsByID = new HashMap<>();
        this.itemStack = itemStack;
        this.listItemStack = listItemStack;
        initItemStack();

        this.xpMap = xpMap;
        this.rewardManager = rewardManager;
    }

    private void initItemStack() {
        PDCUtils.setPetId(itemStack, this);
        PDCUtils.setXP(itemStack, 0);
    }

    /**
     * @param boost The Boost to add
     */
    public void addBoost(@NotNull Boost boost) {
        if (!boostsMap.containsKey(boost.getBoostType())) {
            boostsMap.put(boost.getBoostType(), new ArrayList<>());
        }

        boostsMap.get(boost.getBoostType()).add(boost);
        boostsByID.put(boost.getId(), boost);
    }

    /**
     * Gets the boosts that match the given type for this pet
     * @param boostType The BoostType
     * @return The boosts matching this type or an empty list if none exist
     */
    @NotNull
    public ArrayList<Boost> getBoostsByType(BoostType boostType) {
        return boostsMap.getOrDefault(boostType, new ArrayList<>());
    }

    /**
     * Determines if this pet subscribes to the given event
     * @param experienceType The xp type
     * @param o The key
     * @return If experience should be given for this event
     */
    public boolean subscribesTo(ExperienceType experienceType, Object o) {
        Set<Object> set = xpMap.get(experienceType);
        if (set == null) return false;

        return set.contains(o);
    }

    /**
     * Calculates the level of this pet.
     * @param xp The amount of xp this pet has
     * @return The pet's level
     */
    public int getLevel(double xp) {
        if (startLevel == maxLevel || xp < totalXpNeeded[0]) return startLevel;

        int level = Arrays.binarySearch(totalXpNeeded, (long) xp+1); // Add 1 to the key to make boundary points count as the next level
        if (level < 0) level = -level - 1;
        return Math.min(level + startLevel, maxLevel);
    }

    /**
     * Determines if this pet is at max level
     * @param xp The amount of xp this pet has
     * @return If the pet is at max level
     */
    public boolean isMaxLevel(double xp) {
        return getLevel(xp) >= maxLevel;
    }

    /**
     * Determines the amount of xp needed to level up from the current level to the next
     * @param xp The amount of xp this pet has
     * @return The amount of xp needed for the next level.<p>
     *      0 if the pet is max level
     */
    public long getXpToNextLevel(double xp) {
        int level = getLevel(xp);

        if (level == maxLevel) return 0;
        return xpPerLevel[getLevel(xp) - startLevel];
    }

    /**
     * Determines the amount of xp needed to reach the next level from the current point
     * @param xp The amount of xp this pet has
     * @return The amount of xp needed to reach the next level from the current point.<p>
     *     0 if the pet is max level
     */
    public double getCurrentLevelXp(double xp) {
        int level = getLevel(xp);

        if (level == maxLevel) return 0;
        else if (level == startLevel) return xp;
        return xp - totalXpNeeded[level-startLevel-1];
    }

    /**
     * Determines the amount of xp needed to reach the next level
     * @param xp The amount of xp this pet has
     * @return The amount of xp needed to reach the next level.<p>
     *      0 if the pet is max level
     */
    public double getCurrentLevelXpRemaining(double xp) {
        int level = getLevel(xp);

        if (level == maxLevel) return 0;
        return totalXpNeeded[level-startLevel] - xp;
    }

    /**
     * Determines how much of this level has been earned
     * @param xp The amount of xp this pet has
     * @return The ratio of xp to the next level such that 0 <= val < 1. If the pet is max level, 1 will be returned
     */
    public double getCurrentLevelRatio(double xp) {
        int level = getLevel(xp);

        if (level == maxLevel) return 1;
        // Same as calculating the value of getCurrentLevelXp() / getXpToNextLevel()
        return getCurrentLevelXp(xp) / xpPerLevel[level - startLevel];
    }


    public String getId() {
        return id;
    }

    /**
     * @param level The pet's level
     * @return The pet's item name
     */
    public String getPetName(int level) {
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        return meta.getDisplayName().replace("{lvl}", String.valueOf(level));
    }

    /**
     * Creates an item representing this pet with the owner stored on the item.
     * This will set the last use time to 0.
     * @param owner The owner
     * @param xp The amount of XP to set this pet to
     * @return An ItemStack with PDC data added
     */
    public ItemStack getItemStack(OfflinePlayer owner, double xp) {
        return getItemStack(owner, xp, 0);
    }

    public ItemStack getItemStack(UUID uuid, double xp) {
        return getItemStack(uuid, xp, 0);
    }

    public ItemStack getItemStack(OfflinePlayer owner, double xp, long lastUseTime) {
        return getItemStack(owner.getUniqueId(), xp, lastUseTime);
    }

    /**
     * Creates an item representing this pet with the owner stored on the item
     * @param uuid The owner's uuid
     * @param xp The amount of XP to set this pet to
     * @param lastUseTime The epoch time of the last equip time
     * @return An ItemStack with PDC data added
     */
    public ItemStack getItemStack(UUID uuid, double xp, long lastUseTime) {
        ItemStack item = itemStack.clone();
        PDCUtils.setOwner(item, uuid);
        PDCUtils.setXP(item, xp);
        PDCUtils.setUseTime(item, lastUseTime);
        PDCUtils.setRandomKey(item);

        int lvl = getLevel(xp);
        String level = String.valueOf(lvl);
        String xpToNextLevel = String.valueOf(getXpToNextLevel(xp));
        String currentLevelXp = String.valueOf((long) getCurrentLevelXp(xp));
        String remainingXpNeeded = String.valueOf((long) getCurrentLevelXpRemaining(xp));
        String percentToNextLevel = String.valueOf(Numbers.round(getCurrentLevelRatio(xp) * 100, 2));

        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(meta.getDisplayName()
                .replace("{lvl}", level)
        );
        if (meta.hasLore() && meta.getLore() != null) {
            ArrayList<String> lore = new ArrayList<>(meta.getLore().size());
            for (String str : meta.getLore()) {
                str = str.replace("{lvl}", level)
                        .replace("{next_lvl_xp}", xpToNextLevel)
                        .replace("{xp}", currentLevelXp)
                        .replace("{xp_remaining}", remainingXpNeeded)
                        .replace("{percent}", percentToNextLevel);

                // Support placeholders of the form {boost_id}
                Matcher matcher = pattern.matcher(str);

                while (matcher.find()) {
                    String code = matcher.group();

                    Boost boost = boostsByID.get(code.substring(code.indexOf('_') + 1, code.length() - 1));
                    if (boost == null) continue;

                    str = str.replace(code, boost.getPlaceholderAmount(lvl));
                }

                lore.add(str);
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates an item representing this pet for the list of pets menu.
     * This item should not be given to any player!
     * @return An ItemStack with extra info
     */
    public ItemStack getPetListItemStack() {
        ItemStack item = listItemStack.clone();

        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        String maxLevelString = String.valueOf(maxLevel);

        // Handle boost placeholders (these are custom to the pet list menu item)
        if (meta.hasLore() && meta.getLore() != null) {
            ArrayList<String> lore = new ArrayList<>(meta.getLore().size());
            for (String str : meta.getLore()) {
                str = str.replace("{max_level}", maxLevelString);

                // Support placeholders of the form {boost_id_level}
                Matcher matcher = patternWithLevel.matcher(str);

                while (matcher.find()) {
                    String code = matcher.group();

                    // This will give an array of size 2 [id, level]
                    String[] arr = code.substring(code.indexOf('_') + 1, code.length() - 1).split("_");
                    if (arr.length != 2) continue;

                    Boost boost = boostsByID.get(arr[0]);
                    if (boost == null) continue;

                    str = str.replace(code, boost.getPlaceholderAmount(Numbers.constrain(Integer.parseInt(arr[1]), startLevel, maxLevel)));
                }

                lore.add(str);
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates an item representing this pet for the admin list of pets menu.
     * This item should not be given to any player!
     * @return An ItemStack with extra info
     */
    public ItemStack getAdminPetListItemStack() {
        ItemStack item = itemStack.clone();
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        assert lore != null;
        lore.add("");
        lore.add(Colors.conv("&aid: " + id));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if the player has permission for this pet
     * @param player The player
     * @return True if the player has this permission
     */
    public boolean hasPermission(Player player) {
        if (permission.isBlank()) return true;
        return player.hasPermission(permission);
    }

    public void sendPermissionDeniedMessage(Player player) {
        player.sendMessage(permissionDeniedMessage);
    }

    public long getLockDurationMilliseconds() {
        return lockDurationMilliseconds;
    }

    public Map<ExperienceType, Set<Object>> getXpMap() {
        return xpMap;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    @Override
    public int compareTo(@NotNull Pet o) {
        return this.id.compareTo(o.getId());
    }
}

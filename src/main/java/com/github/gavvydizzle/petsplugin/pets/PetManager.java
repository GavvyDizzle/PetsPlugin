package com.github.gavvydizzle.petsplugin.pets;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.gui.InventoryManager;
import com.github.gavvydizzle.petsplugin.pets.boost.*;
import com.github.gavvydizzle.petsplugin.pets.reward.*;
import com.github.gavvydizzle.petsplugin.pets.xp.ExperienceType;
import com.github.gavvydizzle.petsplugin.player.LoadedPlayer;
import com.github.gavvydizzle.petsplugin.player.PetHolder;
import com.github.gavvydizzle.petsplugin.player.PlayerManager;
import com.github.gavvydizzle.petsplugin.utils.Messages;
import com.github.gavvydizzle.petsplugin.utils.PDCUtils;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.SkullUtils;
import me.gavvydizzle.minerewards.events.RewardFindEvent;
import me.gavvydizzle.minerewards.events.RewardSearchEvent;
import me.wax.prisonenchants.enchants.EnchantIdentifier;
import me.wax.prisonenchants.events.AttemptEnchantActivationEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class PetManager implements Listener {

    private final static Pattern petIDPattern = Pattern.compile("\\w*");

    private final File petsDirectory;
    private final PetsPlugin instance;
    private final PlayerManager playerManager;
    private final HashMap<String, Pet> petHashMap;

    private boolean isCheckingPetOwner;

    public PetManager(PetsPlugin instance, PlayerManager playerManager) {
        this.instance = instance;
        this.playerManager = playerManager;

        petsDirectory = new File(instance.getDataFolder(), "pets");
        petHashMap = new HashMap<>();

        reload();
        playerManager.loadOnlinePlayers();
    }

    /**
     * Handles when this plugin disables.
     */
    public void onShutdown() {
        stopAllBoosts();
    }

    /**
     * Closes all open inventories and saves data before reloading the plugin
     */
    private void closeOpenInventories() {
        InventoryManager inventoryManager = instance.getInventoryManager();
        if (inventoryManager != null) {
            inventoryManager.forceUpdateOpenSelectionMenus();
            inventoryManager.closeAllInventories();
            inventoryManager.getPetMenu().unblockSaving();
        }
    }

    /**
     * Loads all pets from .yml files in the /pets/ subdirectory
     */
    public void reload() {
        closeOpenInventories();

        isCheckingPetOwner = instance.getConfig().getBoolean("checkPetOwner");

        // Remove old selected pets
        for (LoadedPlayer loadedPlayer : playerManager.getPlayers()) {
            for (SelectedPet selectedPet : loadedPlayer.getPetHolder().getEntries()) {
                Pet oldPet = getFromSelectedPet(selectedPet);
                if (oldPet != null) {
                    stopPetBoosts(loadedPlayer.getPlayer(), oldPet);
                }
            }
        }

        petHashMap.clear();

        petsDirectory.mkdir();
        for (final File file : Objects.requireNonNull(petsDirectory.listFiles())) {
            if (!file.isDirectory()) {
                if (file.getName().endsWith(".yml")) {
                    try {
                        loadPet(file);
                    } catch (Exception e) {
                        instance.getLogger().log(Level.SEVERE, "Failed to load pet from " + file.getName() + "!", e);
                    }
                }
            }
        }

        instance.getLogger().info("Loaded " + petHashMap.size() + " pets!");

        // Reload old selected pets
        for (LoadedPlayer loadedPlayer : playerManager.getPlayers()) {
            for (SelectedPet selectedPet : loadedPlayer.getPetHolder().getEntries()) {
                if (selectedPet == null) continue;

                String newPetID = selectedPet.getPetID();
                if (newPetID == null || isInvalidPet(newPetID)) {
                    loadedPlayer.sendMessage(Messages.petInvalidAfterReload.replace("{id}", selectedPet.getPetID()));
                    return;
                }

                Pet newPet = petHashMap.get(newPetID);
                if (newPet != null) {
                    startPetBoosts(loadedPlayer.getPlayer(), newPet);
                }
            }
        }
    }

    /**
     * Loads a pet from a .yml file and adds it to the pet map
     * @param file The file to read from
     */
    private void loadPet(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.options().copyDefaults(true);
        config.addDefault("id", "todo");
        config.addDefault("lockDurationSeconds", 0);
        config.addDefault("permission", "");
        config.addDefault("permissionDenied", "&cYou don't have permission to equip this pet");
        config.addDefault("item.name", "&eName");
        config.addDefault("item.skullLink", "");
        config.addDefault("item.lore", new ArrayList<>());
        config.addDefault("boosts", new HashMap<>());
        config.addDefault("rewards", new HashMap<>());
        config.addDefault("levels.startLevel", 1);
        config.addDefault("levels.maxLevel", 100);
        config.addDefault("levels.amounts", new ArrayList<>());
        try {
            config.save(file);
        } catch (Exception e) {
            instance.getLogger().log(Level.SEVERE, "Failed to save " + file.getName(), e);
            return;
        }

        // Validate id
        String id = config.getString("id");
        if (id == null || id.equalsIgnoreCase("todo")) {
            instance.getLogger().warning("No pet id found in " + file.getName());
            return;
        }
        else if (id.equalsIgnoreCase("null")) {
            instance.getLogger().warning("Invalid pet id 'null' found in " + file.getName() + ". This is a special id that you cannot use");
            return;
        }
        else if (!petIDPattern.matcher(id).matches()) {
            instance.getLogger().warning("Invalid pet id '" + id + "' found in " + file.getName() + ". It can only contain letters, numbers, and underscores");
            return;
        }
        else if (id.length() > 32) {
            instance.getLogger().warning("Invalid pet id '" + id + "' found in " + file.getName() + ". It can be up to 32 characters long");
            return;
        }
        else if (petHashMap.containsKey(id.toLowerCase())) {
            instance.getLogger().warning("Pet id '" + id.toLowerCase() + "' already exists. The pet defined in " + file.getName() + " will not be loaded");
            return;
        }

        int lockDurationSeconds = config.getInt("lockDurationSeconds");
        String permission = config.getString("permission", "");
        String permissionDeniedMessage = Colors.conv(config.getString("permissionDenied"));
        int startLevel = config.getInt("levels.startLevel");
        int maxLevel = config.getInt("levels.maxLevel");
        List<Long> totalXp = config.getLongList("levels.amounts");

        // Check for level amounts <= 0
        for (long amount : totalXp) {
            if (amount <= 0) {
                instance.getLogger().severe("Every value in levels.amounts must be a positive number (" + file.getName() + ")");
                return;
            }
        }

        // Check that the right amount of levels were defined
        int neededLevels = maxLevel - startLevel;
        if (totalXp.size() < neededLevels) {
            instance.getLogger().severe("Not enough levels defined in levels.amounts for " + file.getName() + ". " +
                    "You need to define " + (neededLevels - totalXp.size()) + " more level(s)");
            return;
        }
        else if (totalXp.size() > neededLevels) {
            instance.getLogger().warning("Too many levels defined in levels.amounts for " + file.getName() + ". " +
                    "The last " + (totalXp.size() - neededLevels) + " level(s) are being ignored");
        }

        String skillLink = config.getString("item.skullLink");
        if (skillLink == null) {
            instance.getLogger().severe("No skull link given in " + file.getName());
            return;
        }
        ItemStack itemStack = SkullUtils.getSkull(skillLink);
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv(config.getString("item.name")));
        meta.setLore(Colors.conv(config.getStringList("item.lore")));
        itemStack.setItemMeta(meta);

        ItemStack listItem = itemStack.clone();
        ItemMeta meta2 = listItem.getItemMeta();
        assert meta2 != null;
        meta2.setDisplayName(Colors.conv(config.getString("menu_item.name")));
        meta2.setLore(Colors.conv(config.getStringList("menu_item.lore")));
        listItem.setItemMeta(meta2);

        Map<ExperienceType, Set<Object>> xpMap = new HashMap<>();
        for (ExperienceType experienceType : ExperienceType.values()) {
            Set<Object> set = loadXPSection(config.getConfigurationSection("xp"), experienceType);

            if (set != null && !set.isEmpty()) {
                xpMap.put(experienceType, set);
            }
        }

        RewardManager rewardManager = generateRewardManager(config, file.getName());

        Pet pet = new Pet(id, lockDurationSeconds, permission, permissionDeniedMessage, itemStack, listItem, startLevel, maxLevel, totalXp, xpMap, rewardManager);
        petHashMap.put(pet.getId(), pet);

        ConfigurationSection section = config.getConfigurationSection("boosts");
        if (section != null) {
            readBoosts(section, pet, file.getName());
        }
    }

    /**
     * Reads in all boosts for this pet
     * @param section The configuration section to read from
     * @param pet The pet to apply the boosts to
     */
    private void readBoosts(@NotNull ConfigurationSection section, @NotNull Pet pet, String fileName) {
        for (String key : section.getKeys(false)) {
            String path = "boosts." + key;
            ConfigurationSection boostSection = section.getConfigurationSection(key);
            if (boostSection == null) continue;

            BoostType boostType = BoostType.get(boostSection.getString("type"));
            if (boostType == null) {
                instance.getLogger().severe("Boost type missing " + fileName + " " + path);
                return;
            }


            String multiplierEquation;
            boolean isMultiplicative;

            switch (boostType) {
                case DAMAGE -> {
                    multiplierEquation = boostSection.getString("multiplier");
                    if (multiplierEquation == null) {
                        instance.getLogger().severe("Multiplier missing " + fileName + " " + path);
                        continue;
                    }

                    boolean allowAll = boostSection.getBoolean("allowAll");
                    if (allowAll) {
                        pet.addBoost(new DamageBoost(key, null, multiplierEquation));
                    }
                    else {
                        ArrayList<EntityType> entityTypes = new ArrayList<>();
                        for (String s : boostSection.getStringList("whitelist")) {
                            try {
                                entityTypes.add(EntityType.valueOf(s.toUpperCase()));
                            } catch (Exception ignored) {
                                instance.getLogger().warning("Invalid EntityType '" + s + "' at " + fileName + " " + path + ".whitelist");
                            }
                        }
                        pet.addBoost(new DamageBoost(key, entityTypes, multiplierEquation));
                    }
                }
                case DOUBLE_REWARD -> {
                    String rewardID = boostSection.getString("rewardID");
                    if (rewardID == null) {
                        instance.getLogger().severe("Reward ID missing " + fileName + " " + path);
                        continue;
                    }
                    String percentChanceEquation = boostSection.getString("percent");
                    String message = Colors.conv(boostSection.getString("message"));

                    pet.addBoost(new RewardDoublerBoost(key, rewardID, percentChanceEquation, message));
                }
                case GENERAL_REWARD -> {
                    multiplierEquation = boostSection.getString("multiplier");
                    if (multiplierEquation == null) {
                        instance.getLogger().severe("Multiplier missing " + fileName + " " + path);
                        continue;
                    }
                    isMultiplicative = boostSection.getBoolean("isMultiplicative");

                    pet.addBoost(new GeneralRewardBoost(key, multiplierEquation, isMultiplicative));
                }
                case ENCHANT -> {
                    multiplierEquation = boostSection.getString("multiplier");
                    if (multiplierEquation == null) {
                        instance.getLogger().severe("Multiplier missing " + fileName + " " + path);
                        continue;
                    }
                    isMultiplicative = boostSection.getBoolean("isMultiplicative");

                    boolean allowAll = boostSection.getBoolean("allowAll");
                    if (allowAll) {
                        pet.addBoost(new EnchantBoost(key, null, multiplierEquation, isMultiplicative));
                    }
                    else {
                        ArrayList<EnchantIdentifier> enchantIdentifiers = new ArrayList<>();
                        for (String s : boostSection.getStringList("whitelist")) {
                            try {
                                enchantIdentifiers.add(EnchantIdentifier.valueOf(s));
                            } catch (Exception e) {
                                instance.getLogger().warning("Enchant not found '" + s + "' at " + fileName + " " + path + ".whitelist");
                            }
                        }
                        pet.addBoost(new EnchantBoost(key, enchantIdentifiers, multiplierEquation, isMultiplicative));
                    }
                }
                case POTION_EFFECT -> {
                    PotionEffectType potionEffectType;
                    try {
                        potionEffectType = PotionEffectType.getByName(Objects.requireNonNull(boostSection.getString("effect")));
                        if (potionEffectType == null) throw new RuntimeException("Null potion effect type");
                    } catch (Exception e) {
                        instance.getLogger().severe("Potion effect not found " + fileName + " " + path);
                        continue;
                    }

                    int amplifier = boostSection.getInt("amplifier");

                    pet.addBoost(new PotionEffectBoost(key, potionEffectType, amplifier));
                }
                case XP -> {
                    multiplierEquation = boostSection.getString("multiplier");
                    if (multiplierEquation == null) {
                        instance.getLogger().severe("Multiplier missing " + fileName + " " + path);
                        continue;
                    }

                    isMultiplicative = boostSection.getBoolean("isMultiplicative");

                    pet.addBoost(new XpBoost(key, multiplierEquation, isMultiplicative));
                }
            }
        }
    }

    /**
     * Generates a map of keys and their xp amount.
     * @param topSection The ConfigurationSection containing all XP subsections
     * @param experienceType The xp type
     * @return A map or null
     */
    @Nullable
    public Set<Object> loadXPSection(@Nullable ConfigurationSection topSection, ExperienceType experienceType) {
        if (topSection == null) return null;

        topSection.addDefault(experienceType.name(), new ArrayList<>());

        Set<Object> set = new HashSet<>();
        for (String key : topSection.getStringList(experienceType.name())) {
            Object o = null;
            switch (experienceType) {
                case MINING -> {
                    try {
                        o = Material.valueOf(key);
                    } catch (Exception ignored) {
                        instance.getLogger().warning("Invalid Material for " + experienceType.name() + "." + key + " in xp.yml. This entry will be ignored");
                        continue;
                    }
                }
                case KILLING -> {
                    try {
                        o = EntityType.valueOf(key);
                    } catch (Exception ignored) {
                        instance.getLogger().warning("Invalid EntityType for " + experienceType.name() + "." + key + " in xp.yml. This entry will be ignored");
                        continue;
                    }
                }
            }

            set.add(o);
        }

        return set;
    }

    @Nullable
    private RewardManager generateRewardManager(FileConfiguration config, String fileName) {
        if (config.getConfigurationSection("rewards") == null) return null;
        RewardManager rewardManager = new RewardManager();

        for (String key : Objects.requireNonNull(config.getConfigurationSection("rewards")).getKeys(false)) {
            String path = "rewards." + key;

            RewardsSet rewardsSet;
            String type = config.getString(path + ".type");
            if (type == null) {
                instance.getLogger().warning("Invalid reward type (MINE or KILL) " + fileName + " " + path);
                continue;
            }

            boolean allowAll = config.getBoolean(path + ".allowAll");
            double rewardChance = config.getDouble(path + ".rewardChance");
            if (type.equalsIgnoreCase("MINE")) {
                if (allowAll) {
                    rewardsSet = new MiningRewards(rewardChance, null);
                }
                else {
                    ArrayList<Material> materials = new ArrayList<>();
                    for (String s : config.getStringList(path + ".whitelist")) {
                        try {
                            materials.add(Material.valueOf(s.toUpperCase()));
                        } catch (Exception ignored) {
                            instance.getLogger().warning("Invalid Material '" + s + "' at " + fileName + " " + path + ".whitelist");
                        }
                    }
                    rewardsSet = new MiningRewards(rewardChance, materials);
                }
            }
            else if (type.equalsIgnoreCase("KILL")) {
                if (allowAll) {
                    rewardsSet = new KillRewards(rewardChance, null);
                }
                else {
                    ArrayList<EntityType> entityTypes = new ArrayList<>();
                    for (String s : config.getStringList(path + ".whitelist")) {
                        try {
                            entityTypes.add(EntityType.valueOf(s.toUpperCase()));
                        } catch (Exception ignored) {
                            instance.getLogger().warning("Invalid EntityType '" + s + "' at " + fileName + " " + path + ".whitelist");
                        }
                    }
                    rewardsSet = new KillRewards(rewardChance, entityTypes);
                }
            }
            else {
                instance.getLogger().warning("Invalid reward type (MINE or KILL) " + fileName + " " + path);
                continue;
            }

            if (config.getConfigurationSection(path + ".rewards") == null) {
                continue;
            }
            for (String key2 : Objects.requireNonNull(config.getConfigurationSection(path + ".rewards")).getKeys(false)) {
                String path2 = path + ".rewards." + key2;

                String id = Colors.conv(config.getString(path2 + ".id"));
                int weight = config.getInt(path2 + ".weight");
                if (weight <= 0) {
                    instance.getLogger().warning("Non-positive weight at " + fileName + " " + path2 + ". This reward will be skipped");
                    continue;
                }
                List<String> messages = Colors.conv(config.getStringList(path2 + ".messages"));
                List<String> commands = config.getStringList(path2 + ".commands");
                rewardsSet.registerReward(new Reward(id, weight, messages, commands));
            }

            rewardManager.registerRewardSet(rewardsSet);
        }

        return rewardManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRewardFind(RewardFindEvent e)  {
        PetHolder petHolder = playerManager.getSelectedPets(e.getPlayer());
        if (petHolder == null) return;

        for (SelectedPet selectedPet : petHolder.getEntries()) {
            Pet pet = getFromSelectedPet(selectedPet);
            if (pet == null) return;

            int level = pet.getLevel(selectedPet.getXp());

            for (Boost boost : pet.getBoostsByType(BoostType.DOUBLE_REWARD)) {
                RewardDoublerBoost b = (RewardDoublerBoost) boost;

                if (b.getRewardID().equalsIgnoreCase(e.getReward().getId())) {

                    if (b.shouldActivate(level)) {
                        e.getReward().executeCommand(e.getPlayer());
                        b.sendMessage(e.getPlayer());
                    }

                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRewardSearch(RewardSearchEvent e)  {
        PetHolder petHolder = playerManager.getSelectedPets(e.getPlayer());
        if (petHolder == null) return;

        for (SelectedPet selectedPet : petHolder.getEntries()) {
            Pet pet = getFromSelectedPet(selectedPet);
            if (pet == null) return;

            int level = pet.getLevel(selectedPet.getXp());

            for (Boost boost : pet.getBoostsByType(BoostType.GENERAL_REWARD)) {
                GeneralRewardBoost b = (GeneralRewardBoost) boost;

                if (b.isMultiplicative()) {
                    e.multiply(b.getMultiplier(level));
                }
                else {
                    e.add(b.getMultiplier(level));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onEnchantAttempt(AttemptEnchantActivationEvent e) {
        PetHolder petHolder = playerManager.getSelectedPets(e.getPlayer());
        if (petHolder == null) return;

        for (SelectedPet selectedPet : petHolder.getEntries()) {
            Pet pet = getFromSelectedPet(selectedPet);
            if (pet == null) return;

            int level = pet.getLevel(selectedPet.getXp());

            for (Boost boost : pet.getBoostsByType(BoostType.ENCHANT)) {
                EnchantBoost b = (EnchantBoost) boost;
                if (b.shouldBoostEnchant(e.getEnchant().getEnchantIdentifier())) {

                    if (b.isMultiplicative()) {
                        e.multiplyActivationChance(b.getMultiplier(level));
                    }
                    else {
                        e.setActivationChance(e.getActivationChance() + b.getMultiplier(level));
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;

        PetHolder petHolder = playerManager.getSelectedPets(player);
        if (petHolder == null) return;

        for (SelectedPet selectedPet : petHolder.getEntries()) {
            Pet pet = getFromSelectedPet(selectedPet);
            if (pet == null) return;

            int level = pet.getLevel(selectedPet.getXp());

            for (Boost boost : pet.getBoostsByType(BoostType.DAMAGE)) {
                DamageBoost b = (DamageBoost) boost;
                if (b.shouldBoostDamage(e.getEntityType())) {
                    e.setDamage(e.getDamage() * b.getMultiplier(level));
                }
            }
        }
    }

    @EventHandler
    private void onPetBlockPlace(BlockPlaceEvent e) {
        if (e.getItemInHand().getType() == Material.PLAYER_HEAD) {
            if (PDCUtils.getPetId(e.getItemInHand()) != null) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent e) {
        LoadedPlayer loadedPlayer = playerManager.getLoadedPlayer(e.getPlayer());
        if (loadedPlayer == null) return;

        if (e.getBlock().hasMetadata("player_placed") || playerManager.getRegionManager().isNotInRewardRegion(RewardType.MINING, e.getBlock())) return;

        PetHolder petHolder = loadedPlayer.getPetHolder();

        for (SelectedPet selectedPet : petHolder.getEntries()) {
            Pet pet = getFromSelectedPet(selectedPet);
            if (pet == null) return;

            pet.getRewardManager().attemptMiningReward(loadedPlayer, e.getBlock().getType());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() == null) return;

        LoadedPlayer loadedPlayer = playerManager.getLoadedPlayer(e.getEntity().getKiller());
        if (loadedPlayer == null) return;

        if (playerManager.getRegionManager().isNotInRewardRegion(RewardType.KILLING, e.getEntity().getLocation())) return;

        PetHolder petHolder = loadedPlayer.getPetHolder();

        for (SelectedPet selectedPet : petHolder.getEntries()) {
            Pet pet = getFromSelectedPet(selectedPet);
            if (pet == null) return;

            pet.getRewardManager().attemptKillReward(loadedPlayer, e.getEntityType());
        }
    }

    /**
     * Gets a Pet for this selected pet
     * @param selectedPet The selected pet
     * @return The Pet object associated with this SelectedPet
     */
    @Nullable
    public Pet getFromSelectedPet(@Nullable SelectedPet selectedPet) {
        if (selectedPet == null) return null;
        return petHashMap.get(selectedPet.getPetID());
    }

    /**
     * Reequips all pets for this player
     * @param player The player
     */
    private void refreshPetBoosts(Player player) {
        PetHolder petHolder = playerManager.getSelectedPets(player);
        if (petHolder == null) return;

        onPetUpdate(player, petHolder.getEntries(), petHolder.getEntries());
    }

    /**
     * Equips the new pet boosts and un equips the old pet boosts.
     * @param player The player
     * @param oldSelectedPets The old pets list
     * @param newSelectedPets The new pets list
     */
    public void onPetUpdate(Player player, @Nullable SelectedPet[] oldSelectedPets, SelectedPet[] newSelectedPets) {
        // Stop all boosts
        if (oldSelectedPets != null) {
            for (SelectedPet oldSelectedPet : oldSelectedPets) {
                Pet oldPet = getFromSelectedPet(oldSelectedPet);
                if (oldPet != null) {
                    stopPetBoosts(player, oldPet);
                }
            }
        }

        // Restart all boosts
        for (SelectedPet newSelectedPet : newSelectedPets) {
            Pet newPet = getFromSelectedPet(newSelectedPet);
            if (newPet != null) {
                if (newSelectedPet.getPetID().equals("null")) {
                    player.sendMessage(ChatColor.RED + "No pet found for id=" + newSelectedPet.getPetID() + ". Please alert a staff member!");
                }
                else {
                    startPetBoosts(player, newPet);
                }
            }
        }
    }

    /**
     * Starts all boosts when this pet is equipped
     * @param player The player
     * @param pet The pet
     */
    private void startPetBoosts(Player player, @Nonnull Pet pet) {
        startPotionBoosts(player, pet);
    }

    /**
     * Stops all boosts when this pet is unequipped
     * @param player The player
     * @param pet The pet
     */
    private void stopPetBoosts(Player player, @Nonnull Pet pet) {
        stopPotionBoosts(player, pet);
    }

    /**
     * Stops all pet boosts on server shutdown.
     */
    private void stopAllBoosts() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            LoadedPlayer lp = playerManager.getLoadedPlayer(player);
            if (lp == null) return;

            PetHolder petHolder = lp.getPetHolder();

            for (SelectedPet selectedPet : petHolder.getEntries()) {
                Pet oldPet = getFromSelectedPet(selectedPet);
                if (oldPet != null) {
                    stopPetBoosts(player, oldPet);
                }
            }
        }
    }

    private void stopPotionBoosts(Player player, @Nonnull Pet pet) {
        for (Boost boost : pet.getBoostsByType(BoostType.POTION_EFFECT)) {
            PotionEffectBoost b = (PotionEffectBoost) boost;
            b.removePotionEffect(player);
        }
    }

    private void startPotionBoosts(Player player, @Nonnull Pet pet) {
        for (Boost boost : pet.getBoostsByType(BoostType.POTION_EFFECT)) {
            PotionEffectBoost b = (PotionEffectBoost) boost;
            b.applyPotionEffect(player);
        }
    }

    /**
     * Clears all loaded SelectedPet data
     */
    public void clearLoadedData() {
        for (LoadedPlayer loadedPlayer : playerManager.getPlayers()) {
            for (SelectedPet selectedPet : loadedPlayer.getPetHolder().getEntries()) {
                Pet pet = getFromSelectedPet(selectedPet);
                if (pet != null) stopPetBoosts(loadedPlayer.getPlayer(), pet);
            }
            loadedPlayer.resetPlayer();
        }
    }

    public boolean isInvalidPet(String petID) {
        return !petHashMap.containsKey(petID);
    }

    @Nullable
    public Pet getPet(String id) {
        return petHashMap.get(id.toLowerCase());
    }

    public Collection<Pet> getLoadedPets() {
        return petHashMap.values();
    }

    public Set<String> getPetIDs() {
        return petHashMap.keySet();
    }

    public boolean isCheckingPetOwner() {
        return isCheckingPetOwner;
    }
}

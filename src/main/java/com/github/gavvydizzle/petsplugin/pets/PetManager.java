package com.github.gavvydizzle.petsplugin.pets;

import com.cryptomorin.xseries.SkullUtils;
import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.pets.boost.*;
import com.github.gavvydizzle.petsplugin.pets.reward.*;
import com.github.gavvydizzle.petsplugin.storage.PlayerData;
import com.github.gavvydizzle.petsplugin.utils.Messages;
import com.github.gavvydizzle.petsplugin.utils.PDCUtils;
import com.github.gavvydizzle.petsplugin.utils.Sounds;
import com.github.gavvydizzle.prisonmines.api.PrisonMinesAPI;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.Pair;
import com.github.mittenmc.serverutils.RepeatingTask;
import me.gavvydizzle.minerewards.events.RewardFindEvent;
import me.gavvydizzle.minerewards.events.RewardSearchEvent;
import me.gavvydizzle.playerlevels.events.GivePlayerExperienceEvent;
import me.wax.prisonenchants.enchants.EnchantIdentifier;
import me.wax.prisonenchants.events.AttemptEnchantActivationEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class PetManager implements Listener {

    private final static int AUTO_SAVE_SECONDS = 300;
    private final static int PET_SWAP_MILLIS_DELAY = 500;
    private final static Pattern petIDPattern = Pattern.compile("[\\w]*");

    private final File petsDirectory;
    private final PetsPlugin instance;
    private final PlayerData data;
    private final PrisonMinesAPI prisonMinesAPI;
    private final HashMap<String, Pet> petHashMap;
    private final HashMap<UUID, SelectedPet> selectedPets;
    private final HashMap<UUID, Long> lastSwapTimeMap;

    private boolean isCheckingPetOwner;

    public PetManager(PetsPlugin instance, PlayerData data) {
        this.instance = instance;
        this.data = data;
        prisonMinesAPI = PrisonMinesAPI.getInstance();
        petsDirectory = new File(instance.getDataFolder(), "pets");
        petHashMap = new HashMap<>();
        selectedPets = new HashMap<>();
        lastSwapTimeMap = new HashMap<>();

        startAutoSaving();
        reload();
    }

    private void startAutoSaving() {
        new RepeatingTask(instance, AUTO_SAVE_SECONDS * 20, AUTO_SAVE_SECONDS * 20) {
            @Override
            public void run() {
                if (!selectedPets.isEmpty())
                    Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                        if (!data.savePlayerInfo(selectedPets)) {
                            instance.getLogger().severe("Failed to auto-save selected pets");
                        }
                    });
            }
        };
    }

    /**
     * Saves plugin data on server shutdown
     * @return If the data saved successfully
     */
    public boolean saveDataOnShutdown() {
        return data.savePlayerInfo(selectedPets);
    }

    /**
     * Loads all pets from .yml files in the /pets/ subdirectory
     */
    public void reload() {
        isCheckingPetOwner = instance.getConfig().getBoolean("checkPetOwner");

        HashMap<UUID, SelectedPet> selectedPetsClone = new HashMap<>(selectedPets);

        // Remove old selected pets
        for (UUID uuid : selectedPetsClone.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            SelectedPet selectedPet = selectedPetsClone.get(uuid);

            Pet oldPet = petHashMap.get(selectedPet.getPetID());
            if (oldPet != null) {
                stopPotionBoosts(player, oldPet);
            }

            selectedPets.remove(player.getUniqueId());
        }

        petHashMap.clear();

        petsDirectory.mkdir();
        for (final File file : Objects.requireNonNull(petsDirectory.listFiles())) {
            if (!file.isDirectory()) {
                if (file.getName().endsWith(".yml")) {
                    try {
                        loadPet(file);
                    } catch (Exception e) {
                        instance.getLogger().severe("Failed to load pet from " + file.getName() + "!");
                        e.printStackTrace();
                    }
                }
            }
        }

        instance.getLogger().info("Loaded " + petHashMap.size() + " pets!");

        // Reload old selected pets
        for (UUID uuid : selectedPetsClone.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            SelectedPet selectedPet = selectedPetsClone.get(uuid);
            selectedPets.put(player.getUniqueId(), selectedPet);

            String newPetID = selectedPet.getPetID();
            if (newPetID == null || !petHashMap.containsKey(newPetID)) {
                player.sendMessage(Messages.petInvalidAfterReload.replace("{id}", selectedPet.getPetID()));
                return;
            }

            Pet newPet = petHashMap.get(newPetID);
            if (newPet != null) {
                startPotionBoosts(player, newPet);
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
            instance.getLogger().severe("Failed to save " + file.getName());
            e.printStackTrace();
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

        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        assert itemStack.getItemMeta() != null;
        String skillLink = config.getString("item.skullLink");
        if (skillLink == null) {
            instance.getLogger().severe("No skull link given in " + file.getName());
            return;
        }
        SkullMeta meta = SkullUtils.applySkin(itemStack.getItemMeta(), skillLink);
        meta.setDisplayName(Colors.conv(config.getString("item.name")));
        meta.setLore(Colors.conv(config.getStringList("item.lore")));
        itemStack.setItemMeta(meta);

        ItemStack listItem = itemStack.clone();
        ItemMeta meta2 = listItem.getItemMeta();
        assert meta2 != null;
        meta2.setDisplayName(Colors.conv(config.getString("menu_item.name")));
        meta2.setLore(Colors.conv(config.getStringList("menu_item.lore")));
        listItem.setItemMeta(meta2);

        RewardManager rewardManager = generateRewardManager(config, file.getName());

        Pet pet = new Pet(id, startLevel, maxLevel, totalXp, itemStack, listItem, rewardManager);
        petHashMap.put(pet.getId(), pet);

        if (config.getConfigurationSection("boosts") != null) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("boosts")).getKeys(false)) {
                String path = "boosts." + key;

                BoostType boostType = BoostType.getBoostType(config.getString(path + ".type"));
                if (boostType == null) {
                    instance.getLogger().severe("Boost type missing " + file.getName() + " " + path);
                    return;
                }

                String multiplierEquation;
                boolean isMultiplicative;

                if (boostType == BoostType.DAMAGE) {
                    multiplierEquation = config.getString(path + ".multiplier");
                    if (multiplierEquation == null) {
                        instance.getLogger().severe("Multiplier missing " + file.getName() + " " + path);
                        continue;
                    }

                    boolean allowAll = config.getBoolean(path + ".allowAll");
                    if (allowAll) {
                        pet.addBoost(new DamageBoost(key, null, multiplierEquation));
                    }
                    else {
                        ArrayList<EntityType> entityTypes = new ArrayList<>();
                        for (String s : config.getStringList(path + ".whitelist")) {
                            try {
                                entityTypes.add(EntityType.valueOf(s.toUpperCase()));
                            } catch (Exception ignored) {
                                instance.getLogger().warning("Invalid EntityType '" + s + "' at " + file.getName() + " " + path + ".whitelist");
                            }
                        }
                        pet.addBoost(new DamageBoost(key, entityTypes, multiplierEquation));
                    }
                }
                else if (boostType == BoostType.DOUBLE_REWARD) {
                    String rewardID = config.getString(path + ".rewardID");
                    if (rewardID == null) {
                        instance.getLogger().severe("Reward ID missing " + file.getName() + " " + path);
                        continue;
                    }
                    String percentChanceEquation = config.getString(path + ".percent");
                    String message = Colors.conv(config.getString(path + ".message"));

                    pet.addBoost(new RewardDoublerBoost(key, rewardID, percentChanceEquation, message));
                }
                else if (boostType == BoostType.GENERAL_REWARD) {
                    multiplierEquation = config.getString(path + ".multiplier");
                    if (multiplierEquation == null) {
                        instance.getLogger().severe("Multiplier missing " + file.getName() + " " + path);
                        continue;
                    }
                    isMultiplicative = config.getBoolean(path + ".isMultiplicative");

                    pet.addBoost(new GeneralRewardBoost(key, multiplierEquation, isMultiplicative));
                }
                else if (boostType == BoostType.ENCHANT) {
                    multiplierEquation = config.getString(path + ".multiplier");
                    if (multiplierEquation == null) {
                        instance.getLogger().severe("Multiplier missing " + file.getName() + " " + path);
                        continue;
                    }
                    isMultiplicative = config.getBoolean(path + ".isMultiplicative");

                    boolean allowAll = config.getBoolean(path + ".allowAll");
                    if (allowAll) {
                        pet.addBoost(new EnchantBoost(key, null, multiplierEquation, isMultiplicative));
                    }
                    else {
                        ArrayList<EnchantIdentifier> enchantIdentifiers = new ArrayList<>();
                        for (String s : config.getStringList(path + ".whitelist")) {
                            try {
                                enchantIdentifiers.add(EnchantIdentifier.valueOf(s));
                            } catch (Exception e) {
                                instance.getLogger().warning("Enchant not found '" + s + "' at " + file.getName() + " " + path + ".whitelist");
                            }
                        }
                        pet.addBoost(new EnchantBoost(key, enchantIdentifiers, multiplierEquation, isMultiplicative));
                    }
                }
                else if (boostType == BoostType.POTION_EFFECT) {
                    PotionEffectType potionEffectType;
                    try {
                        potionEffectType = PotionEffectType.getByName(Objects.requireNonNull(config.getString(path + ".effect")));
                        if (potionEffectType == null) throw new RuntimeException("Null potion effect type");
                    } catch (Exception e) {
                        instance.getLogger().severe("Potion effect not found " + file.getName() + " " + path);
                        continue;
                    }

                    int amplifier = config.getInt(path + ".amplifier");

                    pet.addBoost(new PotionEffectBoost(key, potionEffectType, amplifier));
                }
                else if (boostType == BoostType.XP) {
                    multiplierEquation = config.getString(path + ".multiplier");
                    if (multiplierEquation == null) {
                        instance.getLogger().severe("Multiplier missing " + file.getName() + " " + path);
                        continue;
                    }

                    isMultiplicative = config.getBoolean(path + ".isMultiplicative");

                    pet.addBoost(new XpBoost(key, multiplierEquation, isMultiplicative));
                }
            }
        }
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

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            Pair<SelectedPet, Integer> pair = data.getSelectedPet(e.getPlayer());
            if (pair.second() == 1) {
                e.getPlayer().sendMessage("&cAn error occurred when loading your pet. You will not be able to change your pet. Please alert a staff member!");
                selectedPets.put(e.getPlayer().getUniqueId(), new SelectedPet("null", 0));
                return;
            }

            SelectedPet selectedPet = pair.first();
            if (selectedPet != null) {
                Bukkit.getScheduler().runTask(instance, () -> onPlayerJoinLoadPet(e.getPlayer(), selectedPet));
            }
        });
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        lastSwapTimeMap.remove(e.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTaskAsynchronously(instance, () ->
                data.savePlayerInfo(e.getPlayer().getUniqueId(), selectedPets.remove(e.getPlayer().getUniqueId())));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRewardFind(RewardFindEvent e)  {
        Pet pet = getSelectedPet(e.getPlayer());
        if (pet == null) return;

        SelectedPet selectedPet = selectedPets.get(e.getPlayer().getUniqueId());
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRewardSearch(RewardSearchEvent e)  {
        Pet pet = getSelectedPet(e.getPlayer());
        if (pet == null) return;

        SelectedPet selectedPet = selectedPets.get(e.getPlayer().getUniqueId());
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onEnchantAttempt(AttemptEnchantActivationEvent e) {
        Pet pet = getSelectedPet(e.getPlayer());
        if (pet == null) return;

        SelectedPet selectedPet = selectedPets.get(e.getPlayer().getUniqueId());
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onXPGain(GivePlayerExperienceEvent e) {
        Pet pet = getSelectedPet(e.getPlayer());
        if (pet == null) return;

        SelectedPet selectedPet = selectedPets.get(e.getPlayer().getUniqueId());
        if (selectedPet == null) return;

        // Using the PlayerLevels xp system to add pet xp
        int oldLevel = pet.getLevel(selectedPet.getXp());
        selectedPet.addXP((long) e.getDefaultXp());

        int newLevel = pet.getLevel(selectedPet.getXp());

        for (Boost boost : pet.getBoostsByType(BoostType.XP)) {
            XpBoost b = (XpBoost) boost;

            if (b.isMultiplicative()) {
                e.multiply(b.getMultiplier(newLevel));
            }
            else {
                e.add(b.getMultiplier(newLevel));
            }
        }

        if (oldLevel != newLevel) {
            if (!Messages.petLevelUpMessage.isEmpty()) {
                e.getPlayer().sendMessage(Messages.petLevelUpMessage.replace("{pet_name}", pet.getPetName(newLevel)));
                e.getPlayer().sendMessage(Messages.petLevelUpMessage.replace("{lvl}", String.valueOf(newLevel)));
                Sounds.petLevelUpSound.playSound(e.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;

        Pet pet = getSelectedPet(player);
        if (pet == null) return;

        SelectedPet selectedPet = selectedPets.get(player.getUniqueId());
        int level = pet.getLevel(selectedPet.getXp());

        for (Boost boost : pet.getBoostsByType(BoostType.DAMAGE)) {
            DamageBoost b = (DamageBoost) boost;
            if (b.shouldBoostDamage(e.getEntityType())) {
                e.setDamage(e.getDamage() * b.getMultiplier(level));
            }
        }
    }

    @EventHandler
    private void onPetShiftClick(PlayerInteractEvent e) {
        if (e.getPlayer().isSneaking() && e.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack heldPet =  e.getPlayer().getInventory().getItemInMainHand();
            if (heldPet.getType() == Material.AIR) return;

            // Check to see if the petID exists
            String petID = PDCUtils.getPetId(heldPet);
            if (petID == null) return;
            else if (isInvalidPet(petID)) {
                e.getPlayer().sendMessage(Messages.invalidPetSelect.replace("{id}", petID));
                Sounds.generalFailSound.playSound(e.getPlayer());
                return;
            }

            // Check to see if the player is the owner of the pet
            if (isCheckingPetOwner) {
                UUID ownerUUID = PDCUtils.getOwnerUUID(heldPet);
                if (ownerUUID == null || !ownerUUID.equals(e.getPlayer().getUniqueId())) {
                    e.getPlayer().sendMessage(Messages.notPetOwnerOnSelect);
                    Sounds.generalFailSound.playSound(e.getPlayer());
                    return;
                }
            }

            // Pet is now valid

            // Check for quick swap
            if (isOnSwapCooldown(e.getPlayer())) {
                e.getPlayer().sendMessage(Messages.swappingPetTooFast);
                Sounds.generalFailSound.playSound(e.getPlayer());
                return;
            }

            // Collect old selected pet if one exists
            Pet pet = getSelectedPet(e.getPlayer());
            ItemStack selectedPetItem = null;
            if (pet == null) {
                if (selectedPets.containsKey(e.getPlayer().getUniqueId())) { // Selected pet has invalid ID
                    e.getPlayer().sendMessage(Messages.selectedPetInvalid.replace("{id}", selectedPets.get(e.getPlayer().getUniqueId()).getPetID()));
                    return;
                }
            }
            else {
                selectedPetItem = pet.getItemStack(e.getPlayer(), getSelectedPetXP(e.getPlayer()));
            }

            // Move new pet
            e.getPlayer().getInventory().setItem(e.getPlayer().getInventory().getHeldItemSlot(), selectedPetItem != null ? selectedPetItem : new ItemStack(Material.AIR));
            onPetSelect(e.getPlayer(), heldPet);
            Sounds.generalClickSound.playSound(e.getPlayer());
        }
    }

    @EventHandler
    private void onPetBlockPlace(BlockPlaceEvent e) {
        if (e.getItemInHand().getType() == Material.PLAYER_HEAD) {
            if (PDCUtils.getPetId(e.getItemInHand()) != null) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onMineBlockBreak(BlockBreakEvent e) {
        Pet pet = getSelectedPet(e.getPlayer());
        if (pet == null) return;

        if (!e.getBlock().hasMetadata("player_placed") && prisonMinesAPI.getFirstMine(e.getBlock()) != null) {
            pet.getRewardManager().attemptMiningReward(e.getPlayer(), e.getBlock().getType());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() == null) return;
        Player player = e.getEntity().getKiller();

        Pet pet = getSelectedPet(player);
        if (pet == null) return;

        pet.getRewardManager().attemptKillReward(player, e.getEntityType());
    }

    /**
     * @param player The player's pet to get
     * @return The player's selected pet. Null if none is selected or the pet ID does not map to a loaded pet.
     */
    @Nullable
    public Pet getSelectedPet(Player player) {
        SelectedPet selectedPet = selectedPets.get(player.getUniqueId());
        if (selectedPet == null) return null;

        return petHashMap.get(selectedPet.getPetID());

    }

    /**
     * @param player The player
     * @return The amount of xp on this player's selected pet or -1 if no pet is selected
     */
    public long getSelectedPetXP(Player player) {
        SelectedPet selectedPet = selectedPets.get(player.getUniqueId());
        if (selectedPet == null) return -1;

        return selectedPet.getXp();

    }

    /**
     * Sets the pet that the player just selected.
     * If the player deselected a pet, pass a null pet.
     * Any changes will be pushed to the database
     * @param player The player
     * @param petItemStack The selected pet ItemStack or null
     */
    public void onPetSelect(Player player, @Nullable ItemStack petItemStack) {
        Pet oldPet = getSelectedPet(player);
        if (oldPet != null) {
            stopPotionBoosts(player, oldPet);
        }

        if (petItemStack == null) {
            selectedPets.remove(player.getUniqueId());
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> data.savePlayerInfo(player.getUniqueId(), null));
        }
        else {
            String newPetID = PDCUtils.getPetId(petItemStack);
            if (newPetID == null || !petHashMap.containsKey(newPetID)) {
                player.sendMessage(ChatColor.RED + "No pet found for this item. Please alert a staff member!");
                return;
            }

            SelectedPet selectedPet = new SelectedPet(newPetID, Math.max(0, PDCUtils.getXP(petItemStack)));
            selectedPets.put(player.getUniqueId(), selectedPet);
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> data.savePlayerInfo(player.getUniqueId(), selectedPet));

            Pet newPet = petHashMap.get(newPetID);
            if (newPet != null) {
                startPotionBoosts(player, newPet);
            }
        }

        lastSwapTimeMap.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Sets the selected pet for the player.
     * This method should only be called on player join.
     * @param player The player
     * @param selectedPet The selected pet ItemStack or null
     */
    public void onPlayerJoinLoadPet(Player player, @Nullable SelectedPet selectedPet) {
        if (selectedPet == null) return;

        if (!petHashMap.containsKey(selectedPet.getPetID())) {
            player.sendMessage(ChatColor.RED + "Invalid saved pet (id=" + selectedPet.getPetID() + " " + "xp=" + selectedPet.getXp() + "). Please alert a staff member!");
            selectedPets.put(player.getUniqueId(), selectedPet);
            return;
        }

        selectedPets.put(player.getUniqueId(), selectedPet);

        Pet newPet = petHashMap.get(selectedPet.getPetID());
        if (newPet != null) {
            startPotionBoosts(player, newPet);
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
        selectedPets.clear();
        lastSwapTimeMap.clear();
    }

    /**
     * Determines if the player has swapped their pet too recently
     * @param player The player
     * @return If the player cannot swap their pet
     */
    public boolean isOnSwapCooldown(Player player) {
        if (!lastSwapTimeMap.containsKey(player.getUniqueId())) return false;
        return System.currentTimeMillis() - lastSwapTimeMap.get(player.getUniqueId()) < PET_SWAP_MILLIS_DELAY;
    }

    public boolean isInvalidPet(String petID) {
        return !petHashMap.containsKey(petID);
    }

    public boolean hasSelectedPet(Player player) {
        return selectedPets.containsKey(player.getUniqueId());
    }

    @Nonnull
    public String getSelectedPetID(Player player) {
        if (hasSelectedPet(player)) {
            return selectedPets.get(player.getUniqueId()).getPetID();
        }
        return "";
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

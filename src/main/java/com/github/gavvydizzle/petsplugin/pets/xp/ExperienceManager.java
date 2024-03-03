package com.github.gavvydizzle.petsplugin.pets.xp;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.pets.SelectedPet;
import com.github.gavvydizzle.petsplugin.player.LoadedPlayer;
import com.github.gavvydizzle.petsplugin.player.PetHolder;
import com.github.gavvydizzle.petsplugin.player.PlayerManager;
import com.github.gavvydizzle.petsplugin.utils.Messages;
import com.github.mittenmc.serverutils.Colors;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ExperienceManager implements Listener {

    private final PetsPlugin instance;
    private final PlayerManager playerManager;
    private final PetManager petManager;

    private boolean globalXpOverrides;
    private final Map<ExperienceType, Map<Object, Double>> xpMap;

    public ExperienceManager(PetsPlugin instance, PlayerManager playerManager, PetManager petManager) {
        this.instance = instance;
        this.playerManager = playerManager;
        this.petManager = petManager;

        globalXpOverrides = false;
        xpMap = new HashMap<>();

        reload();
    }

    public void reload() {
        FileConfiguration config = PetsPlugin.getConfigManager().get("xp");
        if (config == null) return;

        config.addDefault("useGlobalForAllPets", false);
        globalXpOverrides = config.getBoolean("useGlobalForAllPets", false);

        xpMap.clear();

        for (ExperienceType experienceType : ExperienceType.values()) {
            Map<Object, Double> subMap = loadXPSection(config, experienceType);

            if (subMap != null && !subMap.isEmpty()) {
                xpMap.put(experienceType, subMap);
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
    public Map<Object, Double> loadXPSection(ConfigurationSection topSection, ExperienceType experienceType) {
        topSection.addDefault(experienceType.name(), new HashMap<>());

        ConfigurationSection section = topSection.getConfigurationSection(experienceType.name());
        if (section == null) return null;

        Map<Object, Double> subMap = new HashMap<>();
        for (String key : section.getKeys(false)) {

            double xp = section.getDouble(key);
            if (xp <= 0) {
                instance.getLogger().warning("Invalid xp amount at " + experienceType.name() + "." + key + " in xp.yml. This entry will be ignored");
                continue;
            }

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

            subMap.put(o, xp);
        }

        return subMap;
    }

    /**
     * Gets the xp value
     * @param experienceType The xp type
     * @param o The key
     * @return The xp value for this type and key
     */
    private double getValue(ExperienceType experienceType, Object o) {
        Map<Object, Double> subMap = xpMap.get(experienceType);
        if (subMap == null) return -1;

        return subMap.getOrDefault(o, -1.0);
    }

    /**
     * Gives this set of pets XP.
     * Given the xp type and key, the player's pets which subscribe to this pair will split the xp evenly.
     * @param loadedPlayer The player
     * @param experienceType The xp type
     * @param o The key
     */
    private void givePetsXP(@NotNull LoadedPlayer loadedPlayer, ExperienceType experienceType, Object o) {
        double totalAmount = getValue(experienceType, o);
        if (totalAmount <= 0) return;

        PetHolder petHolder = loadedPlayer.getPetHolder();

        List<SelectedPet> subscribers = new ArrayList<>();
        for (SelectedPet selectedPet : petHolder.getEntries()) {
            Pet pet = petManager.getFromSelectedPet(selectedPet);
            if (pet == null) continue;

            if (globalXpOverrides || pet.subscribesTo(experienceType, o)) {
                // Don't give XP to max level pets
                if (!pet.isMaxLevel(selectedPet.getXp())) subscribers.add(selectedPet);
            }
        }

        if (subscribers.isEmpty()) return;

        double realAmount = totalAmount / subscribers.size();
        for (SelectedPet selectedPet : subscribers) {
            Pet pet = petManager.getFromSelectedPet(selectedPet);
            if (pet == null) return;

            int oldLevel = pet.getLevel(selectedPet.getXp());
            selectedPet.addXP(realAmount);

            int newLevel = pet.getLevel(selectedPet.getXp());

            if (oldLevel != newLevel) {
                if (!Messages.petLevelUpMessage.isEmpty() && loadedPlayer.areLevelUpMessagesOn()) {
                    loadedPlayer.sendMessage(Messages.petLevelUpMessage
                            .replace("{pet_name}", pet.getPetName(newLevel))
                            .replace("{lvl}", String.valueOf(newLevel))
                    );
                }
            }
        }
    }

    /**
     * Validates the global set of xp entries with each pet.
     * Any inconsistencies in the two sets will be sent to the sender.
     * @param sender Who to send output to
     */
    public void validateExperienceEntries(CommandSender sender) {
        if (globalXpOverrides) {
            sender.sendMessage(Colors.conv("&a[" + instance.getName() + "] You are using global xp settings for all pets"));
            return;
        }

        List<MissingExperienceEntry> list = new ArrayList<>();

        for (Pet pet : petManager.getLoadedPets()) {
            for (Map.Entry<ExperienceType, Set<Object>> entry : pet.getXpMap().entrySet()) {
                for (Object o : entry.getValue()) {
                    if (getValue(entry.getKey(), o) < 0) {
                        list.add(new MissingExperienceEntry(pet, entry.getKey(), o));
                    }
                }
            }
        }

        if (list.isEmpty()) {
            sender.sendMessage(Colors.conv("&a[" + instance.getName() + "] All xp entries are valid"));
        }
        else {
            sender.sendMessage(Colors.conv("&c[" + instance.getName() + "] " + list.size() + " xp entries are invalid:"));
            for (MissingExperienceEntry mee : list) {
                sender.sendMessage(Colors.conv("&e - " + mee));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent e) {
        LoadedPlayer loadedPlayer = playerManager.getLoadedPlayer(e.getPlayer());
        if (loadedPlayer == null) return;

        if (e.getBlock().hasMetadata("player_placed") || playerManager.getRegionManager().isNotInXpRegion(ExperienceType.MINING, e.getBlock())) return;

        givePetsXP(loadedPlayer, ExperienceType.MINING, e.getBlock().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() == null) return;

        LoadedPlayer loadedPlayer = playerManager.getLoadedPlayer(e.getEntity().getKiller());
        if (loadedPlayer == null) return;

        if (playerManager.getRegionManager().isNotInXpRegion(ExperienceType.KILLING, e.getEntity().getLocation())) return;

        givePetsXP(loadedPlayer, ExperienceType.KILLING, e.getEntityType());
    }

}

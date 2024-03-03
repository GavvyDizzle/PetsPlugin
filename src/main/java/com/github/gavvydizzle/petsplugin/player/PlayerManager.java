package com.github.gavvydizzle.petsplugin.player;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.storage.PlayerData;
import com.github.mittenmc.serverutils.RepeatingTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager implements Listener {

    private final static int AUTO_SAVE_SECONDS = 300;

    private final Map<UUID, LoadedPlayer> players;
    private final Map<UUID, LoadedPlayer> offlinePlayers;
    private final PetsPlugin instance;
    private final PlayerData database;
    private final MyRegionManager regionManager;

    private RepeatingTask autoSaveTask;

    public PlayerManager(PetsPlugin instance, PlayerData database) {
        this.instance = instance;
        this.database = database;
        players = new HashMap<>();
        offlinePlayers = new HashMap<>();
        regionManager = new MyRegionManager();

        startAutoSaving();
    }

    /**
     * Handles when this plugin disables.
     */
    public void onShutdown() {
        if (!saveDataOnShutdown()) {
            instance.getLogger().severe("Failed to save player data on server shutdown");
        }

        if (autoSaveTask != null) autoSaveTask.cancel();
    }

    /**
     * Saves plugin data on server shutdown
     * @return If the data saved successfully
     */
    private boolean saveDataOnShutdown() {
        return database.saveProfileData(players.values()) && database.saveSelectedPets(players.values());
    }

    private void startAutoSaving() {
        autoSaveTask = new RepeatingTask(instance, AUTO_SAVE_SECONDS * 20, AUTO_SAVE_SECONDS * 20) {
            @Override
            public void run() {
                if (!players.isEmpty())
                    Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                        if (!database.saveSelectedPets(players.values())) {
                            instance.getLogger().severe("Failed to auto-save selected pets");
                        }
                    });
            }
        };
    }

    /**
     * Loads the pets for any online players.
     * Calling this ensures data loads when using Plugman.
     */
    public void loadOnlinePlayers() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                handlePlayerJoin(player);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        handlePlayerJoin(e.getPlayer());
    }

    private void handlePlayerJoin(Player player) {
        // If the player's menu/profile is currently loaded by an admin
        if (offlinePlayers.containsKey(player.getUniqueId())) {
            LoadedPlayer lp = offlinePlayers.remove(player.getUniqueId());
            players.put(player.getUniqueId(), lp);
            lp.logon();
        }
        else {
            Bukkit.getServer().getScheduler().runTaskAsynchronously(instance, () -> {
                LoadedPlayer lp = database.getPlayerInfo(player);
                if (lp == null) {
                    player.sendMessage(ChatColor.RED + "[Pets] Failed to load your data. You can try relogging to fix this");
                    return;
                }

                players.put(lp.getUuid(), lp);
                lp.logon();

                // Push initial pet boosts to the PetManager
                PetManager petManager = instance.getPetManager();
                if (petManager != null) {
                    Bukkit.getScheduler().runTask(instance, () -> petManager.onPetUpdate(player, null, lp.getPetHolder().getEntries()));
                }
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        LoadedPlayer lp = getLoadedPlayer(e.getPlayer());
        if (lp == null) return;

        players.remove(lp.getUuid());
        lp.logout();
        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> database.saveSelectedPets(lp));

        // If any admins are still looking at the player's menu, move them to the offline map
        if (!lp.getProfileViewers().isEmpty()) {
            offlinePlayers.put(e.getPlayer().getUniqueId(), lp);
        }
        else {
            // Otherwise save their profile data
            Bukkit.getScheduler().runTaskAsynchronously(instance, () -> database.saveProfileData(lp));
        }
    }

    public void onAdminLoadProfile(LoadedPlayer loadedPlayer) {
        if (!players.containsKey(loadedPlayer.getUuid())) offlinePlayers.put(loadedPlayer.getUuid(), loadedPlayer);
    }

    /**
     * Creates a task that will attempt to unload this profile in 1 tick.
     * The profile will unload and save if nobody is looking at the offline profile
     * @param loadedPlayer The LoadedPlayer profile
     */
    public void schedulePlayerUnloadAttempt(LoadedPlayer loadedPlayer) {
        if (!offlinePlayers.containsKey(loadedPlayer.getUuid())) return;

        Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () ->  {
            if (loadedPlayer.getProfileViewers().isEmpty()) {
                unloadProfile(loadedPlayer);
            }
        }, 1);
    }

    // Cleans up and saves the profile's data
    private void unloadProfile(LoadedPlayer loadedPlayer) {
        offlinePlayers.remove(loadedPlayer.getUuid());
        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> database.saveProfileData(loadedPlayer));
    }

    /**
     * Gets the pets selected by this player
     * @param player The player
     * @return Their selected pet(s)
     */
    @Nullable
    public PetHolder getSelectedPets(Player player) {
        LoadedPlayer lp = getLoadedPlayer(player);
        if (lp == null) return null;

        return lp.getPetHolder();
    }

    /**
     * Gets a player's LoadedPlayer
     * @param uuid The player's UUID
     * @return The player as a LoadedPlayer
     */
    @Nullable
    public LoadedPlayer getLoadedPlayer(UUID uuid) {
        return players.get(uuid);
    }

    /**
     * Gets a player's LoadedPlayer by checking the online and offline maps
     * @param uuid The player's UUID
     * @return The player as a LoadedPlayer
     */
    @Nullable
    public LoadedPlayer getOnlineOrOfflinePlayer(UUID uuid) {
        LoadedPlayer loadedPlayer = players.get(uuid);
        if (loadedPlayer != null) return loadedPlayer;

        return offlinePlayers.get(uuid);
    }

    @Nullable
    public LoadedPlayer getLoadedPlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public Collection<LoadedPlayer> getPlayers() {
        return players.values();
    }

    public MyRegionManager getRegionManager() {
        return regionManager;
    }
}

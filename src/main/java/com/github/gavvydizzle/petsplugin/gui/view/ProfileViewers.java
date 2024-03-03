package com.github.gavvydizzle.petsplugin.gui.view;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.player.LoadedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Represents a list of players who are viewing this player
 */
public class ProfileViewers {

    private final LoadedPlayer loadedPlayer;
    private int viewerID;
    private final HashMap<UUID, Viewer> viewers;

    public ProfileViewers(LoadedPlayer loadedPlayer) {
        this.loadedPlayer = loadedPlayer;
        viewers = new HashMap<>();
    }

    @Nullable
    private Viewer getViewer(UUID uuid) {
        return viewers.get(uuid);
    }

    /**
     * Adds a new viewer to this profile
     * @param viewerUUID The UUID of this viewer
     * @param menuType The type of menu opened
     */
    public void addViewer(UUID viewerUUID, MenuType menuType) {
        if (viewers.containsKey(viewerUUID)) {
            viewers.put(viewerUUID, new Viewer(viewerID, viewerUUID, menuType));
        }
        else {
            viewers.put(viewerUUID, new Viewer(viewerID, viewerUUID, menuType));
            viewerID++;
        }
    }

    /**
     * Removes a viewer from this profile
     * @param uuid The uuid
     */
    public void removeViewer(UUID uuid) {
        Viewer viewer = viewers.remove(uuid);
        if (viewer != null) pushUpdatesOnUnlock(viewer);
    }

    /**
     * Determines if this page is locked.
     * If multiple players are on the same page, priority will be given to the player who opened it first.
     * @param uuid The player's uuid
     * @return If this player should not be able to edit their current page
     */
    public boolean isMenuLocked(UUID uuid) {
        if (viewers.size() <= 1) return false;

        Viewer viewer = getViewer(uuid);
        if (viewer == null) return true;

        ArrayList<Viewer> otherViewers = new ArrayList<>(viewers.values());
        otherViewers.remove(viewer);

        for (Viewer other : otherViewers) {
            // Only care about locking PET_STORAGE menus since the other menus are view-only for admins
            // Feel free to edit this if you need different functionality
            if (other.menuType == MenuType.PET_STORAGE && viewer.menuType == MenuType.PET_STORAGE) {
                if (viewer.compareTo(other) > 0) return true;
            }
        }
        return false;
    }

    /**
     * Refreshes the inventory of any player who was locked out
     * @param oldViewer The viewer before changing its page
     */
    private void pushUpdatesOnUnlock(Viewer oldViewer) {
        ArrayList<Viewer> otherViewers = new ArrayList<>(viewers.values());
        otherViewers.remove(oldViewer);

        for (Viewer other : otherViewers) {
            // Only care about locking PET_STORAGE menus since the other menus are view-only for admins and do not need to be refreshed
            // Feel free to edit this if you need different functionality
            if (other.menuType == MenuType.PET_STORAGE && oldViewer.menuType == MenuType.PET_STORAGE) {
                // If "other" is locked out by "oldViewer"
                if (other.compareTo(oldViewer) > 0) {
                    Player player = Bukkit.getPlayer(other.uuid);
                    if (player != null) PetsPlugin.getInstance().getInventoryManager().getPetMenu().refreshMenu(player, loadedPlayer);
                }
            }
        }
    }

    public boolean isEmpty() {
        return viewers.isEmpty();
    }


    private static class Viewer implements Comparable<Viewer> {

        private final int id;
        private final UUID uuid;
        private final MenuType menuType;
        private final long accessMillis;

        public Viewer(int id, UUID uuid, MenuType menuType) {
            this.id = id;
            this.uuid = uuid;
            this.menuType = menuType;
            accessMillis = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "Viewer " + id + ": " + uuid + " (" + menuType + "," + accessMillis + ")";
        }

        @Override
        public int compareTo(@NotNull ProfileViewers.Viewer o) {
            if (this.accessMillis != o.accessMillis) {
                return Long.compare(this.accessMillis, o.accessMillis);
            }
            return Integer.compare(this.id, o.id);
        }
    }

}

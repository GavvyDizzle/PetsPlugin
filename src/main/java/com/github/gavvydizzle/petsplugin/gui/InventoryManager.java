package com.github.gavvydizzle.petsplugin.gui;

import com.github.gavvydizzle.petsplugin.pets.PetManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.UUID;

public class InventoryManager implements Listener {

    private final PetMenu petMenu;
    private final PetListMenu petListMenu;
    private final HashMap<UUID, ClickableMenu> playersInInventory;

    public InventoryManager(PetManager petManager) {
        playersInInventory = new HashMap<>();

        petMenu = new PetMenu(this, petManager);
        petListMenu = new PetListMenu(this, petManager);
    }

    public void reload() {
        petMenu.reload();
    }

    /**
     * Saves the menu the player opened so clicks can be passed to it correctly
     * @param player The player
     * @param clickableMenu The menu they opened
     */
    public void onMenuOpen(Player player, ClickableMenu clickableMenu) {
        playersInInventory.put(player.getUniqueId(), clickableMenu);
    }

    /**
     * Opens the given menu and adds the player to the list of players with opened menus
     * @param player The player
     * @param clickableMenu The menu to open
     */
    public void openMenu(Player player, ClickableMenu clickableMenu) {
        onMenuOpen(player, clickableMenu);
        clickableMenu.openInventory(player);
    }

    @EventHandler
    private void onInventoryClose(InventoryCloseEvent e) {
        ClickableMenu clickableMenu = playersInInventory.remove(e.getPlayer().getUniqueId());
        if (clickableMenu != null) {
            clickableMenu.closeInventory((Player) e.getPlayer());
        }
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;

        if (playersInInventory.containsKey(e.getWhoClicked().getUniqueId())) {
            e.setCancelled(true);
            // Pass along the click event without verifying the inventory the click was done in
            playersInInventory.get(e.getWhoClicked().getUniqueId()).handleClick(e);
        }
    }

    public PetMenu getPetMenu() {
        return petMenu;
    }

    public PetListMenu getPetListMenu() {
        return petListMenu;
    }
}

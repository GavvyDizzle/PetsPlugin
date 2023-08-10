package com.github.gavvydizzle.petsplugin.gui;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.gui.item.InventoryItem;
import com.github.gavvydizzle.petsplugin.gui.item.ItemType;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.utils.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class PetListMenu implements ClickableMenu {

    private final String menuID;
    private final InventoryManager inventoryManager;
    private final PetManager petManager;
    private final Inventory inventory;
    private final Map<Integer, InventoryItem> inventoryItemMap;

    public PetListMenu(String menuID, Inventory inventory, InventoryManager inventoryManager, PetManager petManager) {
        this.menuID = menuID;
        this.inventory = inventory;

        this.inventoryManager = inventoryManager;
        this.petManager = petManager;

        inventoryItemMap = new HashMap<>();
    }

    public void addInventoryItem(int slot, InventoryItem inventoryItem) {
        if (inventoryItem.getItemType() == ItemType.PET) {

            assert inventoryItem.getExtra() != null;
            Pet pet = petManager.getPet(inventoryItem.getExtra());
            if (pet != null) {
                inventoryItemMap.put(slot, inventoryItem);
                inventory.setItem(slot, pet.getPetListItemStack());
            }
        }
        else {
            inventoryItemMap.put(slot, inventoryItem);
            inventory.setItem(slot, inventoryItem.getItemStack());
        }
    }

    /**
     * Reloads all pet items displayed in this menu.
     * This is to be called after pets have reloaded.
     */
    public void updatePetItems() {
        for (Map.Entry<Integer, InventoryItem> entry : inventoryItemMap.entrySet()) {
            if (entry.getValue().getItemType() == ItemType.PET) {
                assert entry.getValue().getExtra() != null;

                Pet pet = petManager.getPet(entry.getValue().getExtra());
                if (pet == null) {
                    PetsPlugin.getInstance().getLogger().warning("Invalid petID '" + entry.getValue().getExtra() + "' in submenu menuID=" + menuID + " in menus.yml. The pet was deleted or did not load properly");
                    continue;
                }

                inventory.setItem(entry.getKey(), pet.getPetListItemStack());
            }
        }
    }

    @Override
    public void openInventory(Player player) {
        player.openInventory(inventory);
        inventoryManager.onMenuOpen(player, this);
    }

    @Override
    public void closeInventory(Player player) {

    }

    @Override
    public void handleClick(InventoryClickEvent e) {
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        InventoryItem inventoryItem = inventoryItemMap.get(e.getSlot());
        if (inventoryItem == null) return;

        if (inventoryItem.getItemType() == ItemType.LINK) {
            assert inventoryItem.getExtra() != null;
            if (!inventoryItem.getExtra().equalsIgnoreCase(menuID)) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(PetsPlugin.getInstance(), () -> inventoryManager.openMenu((Player) e.getWhoClicked(), inventoryManager.getSubmenu(inventoryItem.getExtra())));
            }
        }
        else if (inventoryItem.getItemType() == ItemType.BACK) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(PetsPlugin.getInstance(), () -> inventoryManager.openMenu((Player) e.getWhoClicked(), inventoryManager.getPetListMainMenu()));
        }
        else if (inventoryItem.getItemType() == ItemType.PET && e.getWhoClicked().hasPermission("petsplugin.petsadmin")) {
            assert inventoryItem.getExtra() != null;
            Pet pet = petManager.getPet(inventoryItem.getExtra());
            if (pet == null) return;

            e.getWhoClicked().getInventory().addItem(pet.getItemStack((Player) e.getWhoClicked(), 0));
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "You received a new " + pet.getId() + " pet");
            Sounds.generalClickSound.playSound((Player) e.getWhoClicked());
        }
    }

    public boolean isItemListEmpty() {
        return inventoryItemMap.isEmpty();
    }
}

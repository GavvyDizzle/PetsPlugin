package com.github.gavvydizzle.petsplugin.gui;

import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.utils.Sounds;
import com.github.mittenmc.serverutils.ColoredItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Admin only menu that lists all loaded pets
 */
public class PetListMenu implements ClickableMenu {

    private static final int inventorySize;
    private static final int pageDownSlot;
    private static final int pageInfoSlot;
    private static final int pageUpSlot;
    private static final ItemStack pageInfoItem;
    private static final ItemStack previousPageItem;
    private static final ItemStack nextPageItem;
    private static final ItemStack pageRowFiller;

    static {
        inventorySize = 54;
        pageDownSlot = 48;
        pageInfoSlot = 49;
        pageUpSlot = 50;

        pageInfoItem = new ItemStack(Material.PAPER);

        previousPageItem = new ItemStack(Material.PAPER);
        ItemMeta prevPageMeta = previousPageItem.getItemMeta();
        assert prevPageMeta != null;
        prevPageMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");
        previousPageItem.setItemMeta(prevPageMeta);

        nextPageItem = new ItemStack(Material.PAPER);
        ItemMeta nextPageMeta = nextPageItem.getItemMeta();
        assert nextPageMeta != null;
        nextPageMeta.setDisplayName(ChatColor.YELLOW + "Next Page");
        nextPageItem.setItemMeta(nextPageMeta);

        pageRowFiller = ColoredItems.WHITE.getGlass();
    }

    private final InventoryManager inventoryManager;
    private final PetManager petManager;
    private final String inventoryName;
    private final ArrayList<Pet> pets;
    private final ArrayList<ItemStack> petItems;
    private final HashMap<UUID, Integer> playerPages;

    public PetListMenu(InventoryManager inventoryManager, PetManager petManager) {
        this.inventoryManager = inventoryManager;
        this.petManager = petManager;

        inventoryName = "Pets List";
        pets = new ArrayList<>();
        petItems = new ArrayList<>();
        playerPages = new HashMap<>();
        reloadContents();
    }

    /**
     * Reloads the contents of this menu.
     * This method should be called after all pets have been reloaded.
     */
    public void reloadContents() {
        pets.clear();
        pets.addAll(petManager.getLoadedPets());
        Collections.sort(pets);

        petItems.clear();
        for (Pet pet : pets) {
            petItems.add(pet.getPetListItemStack());
        }
    }

    @Override
    public void openInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(player, inventorySize, inventoryName);

        for (int slot = 0; slot < getNumItemsOnPage(1); slot++) {
            inventory.setItem(slot, petItems.get(getIndexByPage(1, slot)));
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, pageRowFiller);
        }
        inventory.setItem(pageDownSlot, previousPageItem);
        inventory.setItem(pageInfoSlot, getPageItem(1));
        inventory.setItem(pageUpSlot, nextPageItem);

        inventoryManager.onMenuOpen(player, this);
        playerPages.put(player.getUniqueId(), 1);
        player.openInventory(inventory);
    }

    @Override
    public void closeInventory(Player player) {
        playerPages.remove(player.getUniqueId());
    }

    @Override
    public void handleClick(InventoryClickEvent e) {
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        if (e.getSlot() == pageUpSlot) {
            if (playerPages.get(e.getWhoClicked().getUniqueId()) < getMaxPage()) {
                playerPages.put(e.getWhoClicked().getUniqueId(), playerPages.get(e.getWhoClicked().getUniqueId()) + 1);
                updatePage((Player) e.getWhoClicked());
                Sounds.pageTurnSound.playSound((Player) e.getWhoClicked());
            }
            else {
                Sounds.generalFailSound.playSound((Player) e.getWhoClicked());
            }
        }
        else if (e.getSlot() == pageDownSlot) {
            if (playerPages.get(e.getWhoClicked().getUniqueId()) > 1) {
                playerPages.put(e.getWhoClicked().getUniqueId(), playerPages.get(e.getWhoClicked().getUniqueId()) - 1);
                updatePage((Player) e.getWhoClicked());
                Sounds.pageTurnSound.playSound((Player) e.getWhoClicked());
            }
            else {
                Sounds.generalFailSound.playSound((Player) e.getWhoClicked());
            }
        }
        else {
            Pet pet;
            try {
                pet = pets.get(getIndexByPage(playerPages.get(e.getWhoClicked().getUniqueId()), e.getSlot()));
            } catch (Exception ignored) {
                return;
            }

            if (pet == null) return;

            e.getWhoClicked().getInventory().addItem(pet.getItemStack((Player) e.getWhoClicked(), 0));
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "You received a new " + pet.getId() + " pet");
            Sounds.generalClickSound.playSound((Player) e.getWhoClicked());
        }
    }

    private void updatePage(Player player) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        int page = playerPages.get(player.getUniqueId());

        for (int i = 0; i < 45; i++) {
            inventory.clear(i);
        }

        for (int i = 0; i < getNumItemsOnPage(page); i++) {
            inventory.setItem(i, petItems.get(getIndexByPage(page, i)));
        }

        inventory.setItem(pageInfoSlot, getPageItem(page));
    }

    private ItemStack getPageItem(int page) {
        ItemStack pageInfo = pageInfoItem.clone();
        ItemMeta meta = pageInfo.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.YELLOW + "Page " + page + "/" + getMaxPage());
        pageInfo.setItemMeta(meta);
        return pageInfo;
    }

    private int getMaxPage() {
        return (petItems.size() - 1) / 45 + 1;
    }

    private int getNumItemsOnPage(int page) {
        return Math.min(45, petItems.size() - (page - 1) * 45);
    }

    private int getIndexByPage(int page, int slot) {
        return (page - 1) * 45 + slot;
    }
}

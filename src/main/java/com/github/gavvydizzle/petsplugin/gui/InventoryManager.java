package com.github.gavvydizzle.petsplugin.gui;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.configs.MenusConfig;
import com.github.gavvydizzle.petsplugin.gui.item.InventoryItem;
import com.github.gavvydizzle.petsplugin.gui.item.ItemType;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.mittenmc.serverutils.ColoredItems;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.ConfigUtils;
import com.github.mittenmc.serverutils.Numbers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryManager implements Listener {

    private final PetManager petManager;
    private final PetSelectionMenu petMenu;
    private final PetListAdminMenu petListAdminMenu;
    private final PetListMainMenu petListMainMenu;
    private final Map<String, PetListMenu> submenuMap;
    private final HashMap<UUID, ClickableMenu> playersInInventory;

    public InventoryManager(PetManager petManager) {
        this.petManager = petManager;
        playersInInventory = new HashMap<>();

        petMenu = new PetSelectionMenu(this, petManager);
        petListAdminMenu = new PetListAdminMenu(this, petManager);
        petListMainMenu = new PetListMainMenu(this, petManager);
        submenuMap = new HashMap<>();
        loadSubmenus();
    }

    public void reload() {
        petMenu.reload();
        petListMainMenu.reload();
        loadSubmenus();
    }

    /**
     * Calls the #updatePetItems() method for all applicable menus
     */
    public void updatePetItems() {
        petListMainMenu.updatePetItems();
        for (PetListMenu petListMenu : submenuMap.values()) {
            petListMenu.updatePetItems();
        }
    }

    // Call after reloading main menu
    private void loadSubmenus() {
        submenuMap.clear();

        FileConfiguration config = MenusConfig.get();

        ConfigurationSection configurationSection = config.getConfigurationSection("pet_list_submenus");
        if (configurationSection == null) return;

        for (String key : configurationSection.getKeys(false)) {
            String path = "pet_list_submenus." + key;

            String id = config.getString(path + ".id");
            if (id == null) {
                PetsPlugin.getInstance().getLogger().warning("No id defined for submenu at " + path + " in menus.yml");
                continue;
            }

            id = id.toLowerCase();
            if (submenuMap.containsKey(id)) {
                PetsPlugin.getInstance().getLogger().warning("The submenu id '" + id + "' has already been defined. Duplicate located at " + path + " in menus.yml");
                continue;
            }

            String inventoryName = Colors.conv(config.getString(path + ".name"));
            int inventorySize = Numbers.constrain(config.getInt(path + ".rows"), 1, 6) * 9;
            ItemStack filler = ColoredItems.getGlassByName(config.getString(path + ".filler"));

            Inventory inventory = Bukkit.createInventory(null, inventorySize, inventoryName);
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }

            ConfigurationSection itemsConfigurationSection = config.getConfigurationSection(path + ".items");
            if (itemsConfigurationSection == null) continue;

            PetListMenu submenu = new PetListMenu(id, inventory, this, petManager);

            for (String key2 : itemsConfigurationSection.getKeys(false)) {
                String path2 = path + ".items." + key2;

                ItemType type = ItemType.getType(config.getString(path2 + ".type"));
                if (type == null) {
                    PetsPlugin.getInstance().getLogger().warning("Invalid item type for " + path2 + " in menus.yml");
                    continue;
                }

                int slot = config.getInt(path2 + ".slot");
                if (!Numbers.isWithinRange(slot, 0, inventory.getSize())) {
                    PetsPlugin.getInstance().getLogger().warning("Slot " + slot + " is out of bounds (" + path2 + " in menus.yml)");
                    continue;
                }

                switch (type) {
                    case BACK -> {
                        ItemStack itemStack = new ItemStack(ConfigUtils.getMaterial(config.getString(path2 + ".material"), Material.PAPER));
                        ItemMeta meta = itemStack.getItemMeta();
                        assert meta != null;
                        meta.setDisplayName(Colors.conv(config.getString(path2 + ".name")));
                        meta.setLore(Colors.conv(config.getStringList(path2 + ".lore")));
                        itemStack.setItemMeta(meta);

                        submenu.addInventoryItem(slot, new InventoryItem(ItemType.BACK, itemStack, null));
                    }
                    case ITEM -> {
                        ItemStack itemStack = new ItemStack(ConfigUtils.getMaterial(config.getString(path2 + ".material"), Material.PAPER));
                        ItemMeta meta = itemStack.getItemMeta();
                        assert meta != null;
                        meta.setDisplayName(Colors.conv(config.getString(path2 + ".name")));
                        meta.setLore(Colors.conv(config.getStringList(path2 + ".lore")));
                        itemStack.setItemMeta(meta);

                        submenu.addInventoryItem(slot, new InventoryItem(ItemType.ITEM, itemStack, null));
                    }
                    case LINK -> {
                        String menuID = config.getString(path2 + ".menuID");
                        if (menuID == null) {
                            PetsPlugin.getInstance().getLogger().warning("Invalid menuID (" + path2 + " in menus.yml)");
                            continue;
                        }

                        ItemStack itemStack = new ItemStack(ConfigUtils.getMaterial(config.getString(path2 + ".material"), Material.PAPER));
                        ItemMeta meta = itemStack.getItemMeta();
                        assert meta != null;
                        meta.setDisplayName(Colors.conv(config.getString(path2 + ".name")));
                        meta.setLore(Colors.conv(config.getStringList(path2 + ".lore")));
                        itemStack.setItemMeta(meta);

                        submenu.addInventoryItem(slot, new InventoryItem(ItemType.LINK, itemStack, menuID));
                    }
                    case PET -> {
                        String petID = config.getString(path2 + ".petID");
                        if (petID == null || petManager.getPet(petID) == null) {
                            PetsPlugin.getInstance().getLogger().warning("Invalid petID (" + path2 + " in menus.yml)");
                            continue;
                        }

                        Pet pet = petManager.getPet(petID);
                        if (pet == null) {
                            PetsPlugin.getInstance().getLogger().warning("Invalid petID '" + petID + "' at " + path2 + " in menus.yml");
                            continue;
                        }

                        submenu.addInventoryItem(slot, new InventoryItem(ItemType.PET, null, petID));
                    }
                }
            }

            if (submenu.isItemListEmpty()) {
                PetsPlugin.getInstance().getLogger().warning("Empty item list for submenu at " + path + " in menus.yml");
            }
            else {
                submenuMap.put(id, submenu);
            }
        }
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
     * @param clickableMenu The menu to open. If null, nothing will happen
     */
    public void openMenu(Player player, @Nullable ClickableMenu clickableMenu) {
        if (clickableMenu == null) return;

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

    @Nullable
    protected PetListMenu getSubmenu(String id) {
        return submenuMap.get(id);
    }

    public PetSelectionMenu getPetMenu() {
        return petMenu;
    }

    public PetListMainMenu getPetListMainMenu() {
        return petListMainMenu;
    }

    public PetListAdminMenu getPetListAdminMenu() {
        return petListAdminMenu;
    }
}

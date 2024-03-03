package com.github.gavvydizzle.petsplugin.gui;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.gui.item.InventoryItem;
import com.github.gavvydizzle.petsplugin.gui.item.ItemType;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.utils.Sounds;
import com.github.mittenmc.serverutils.ColoredItems;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.ConfigUtils;
import com.github.mittenmc.serverutils.Numbers;
import com.github.mittenmc.serverutils.gui.ClickableMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class PetListMainMenu implements ClickableMenu {

    private final InventoryManager inventoryManager;
    private final PetManager petManager;
    private Inventory inventory;
    private final Map<Integer, InventoryItem> inventoryItemMap;

    public PetListMainMenu(InventoryManager inventoryManager, PetManager petManager) {
        this.inventoryManager = inventoryManager;
        this.petManager = petManager;

        inventoryItemMap = new HashMap<>();
        reload();
    }

    public void reload() {
        FileConfiguration config = PetsPlugin.getConfigManager().get("menus");
        if (config == null) return;

        config.addDefault("pet_list_main_menu.name", "Pet Select");
        config.addDefault("pet_list_main_menu.rows", 3);
        config.addDefault("pet_list_main_menu.filler", "white");
        config.addDefault("pet_list_submenus", new HashMap<>());

        String inventoryName = Colors.conv(config.getString("pet_list_main_menu.name"));
        int inventorySize = Numbers.constrain(config.getInt("pet_list_main_menu.rows"), 1, 6) * 9;
        ItemStack filler = ColoredItems.getGlassByName(config.getString("pet_list_main_menu.filler"));

        inventory = Bukkit.createInventory(null, inventorySize, inventoryName);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        reloadContents(config);
    }

    /**
     * Reloads the contents of this menu.
     */
    public void reloadContents(FileConfiguration config) {
        inventoryItemMap.clear();

        ConfigurationSection configurationSection = config.getConfigurationSection("pet_list_main_menu.items");
        if (configurationSection == null) return;

        for (String key : configurationSection.getKeys(false)) {
            String path = "pet_list_main_menu.items." + key;

            ItemType type = ItemType.get(config.getString(path + ".type"));
            if (type == null) {
                PetsPlugin.getInstance().getLogger().warning("Invalid item type for " + path + " in menus.yml");
                continue;
            }

            int slot = config.getInt(path + ".slot");
            if (!Numbers.isWithinRange(slot, 0, inventory.getSize())) {
                PetsPlugin.getInstance().getLogger().warning("Slot " + slot + " is out of bounds (" + path + " in menus.yml)");
                continue;
            }

            switch (type) {
                case BACK -> PetsPlugin.getInstance().getLogger().warning("Back buttons are not allowed in the main menu (" + path + " in menus.yml)");
                case ITEM -> {
                    ItemStack itemStack = new ItemStack(ConfigUtils.getMaterial(config.getString(path + ".material"), Material.PAPER));
                    ItemMeta meta = itemStack.getItemMeta();
                    assert meta != null;
                    meta.setDisplayName(Colors.conv(config.getString(path + ".name")));
                    meta.setLore(Colors.conv(config.getStringList(path + ".lore")));
                    itemStack.setItemMeta(meta);

                    inventory.setItem(slot, itemStack);
                }
                case LINK -> {
                    String menuID = config.getString(path + ".menuID");
                    if (menuID == null) {
                        PetsPlugin.getInstance().getLogger().warning("Invalid menuID (" + path + " in menus.yml)");
                        continue;
                    }

                    ItemStack itemStack = new ItemStack(ConfigUtils.getMaterial(config.getString(path + ".material"), Material.PAPER));
                    ItemMeta meta = itemStack.getItemMeta();
                    assert meta != null;
                    meta.setDisplayName(Colors.conv(config.getString(path + ".name")));
                    meta.setLore(Colors.conv(config.getStringList(path + ".lore")));
                    itemStack.setItemMeta(meta);

                    InventoryItem linkItem = new InventoryItem(ItemType.LINK, itemStack, menuID);
                    inventoryItemMap.put(slot, linkItem);
                    inventory.setItem(slot, itemStack);
                }
                case PET -> {
                    String petID = config.getString(path + ".petID");
                    if (petID == null || petManager.getPet(petID) == null) {
                        PetsPlugin.getInstance().getLogger().warning("Invalid petID (" + path + " in menus.yml)");
                        continue;
                    }

                    Pet pet = petManager.getPet(petID);
                    if (pet == null) {
                        PetsPlugin.getInstance().getLogger().warning("Invalid petID '" + petID + "' at " + path + " in menus.yml");
                        continue;
                    }

                    InventoryItem petItem = new InventoryItem(ItemType.PET, null, petID);
                    inventoryItemMap.put(slot, petItem);
                    inventory.setItem(slot, pet.getPetListItemStack());
                }
            }
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
                    PetsPlugin.getInstance().getLogger().warning("Invalid petID '" + entry.getValue().getExtra() + "' in main menu in menus.yml. The pet was deleted or did not load properly");
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
            Bukkit.getScheduler().scheduleSyncDelayedTask(PetsPlugin.getInstance(), () -> inventoryManager.openMenu((Player) e.getWhoClicked(), inventoryManager.getSubmenu(inventoryItem.getExtra())));
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
}

package com.github.gavvydizzle.petsplugin.gui;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.configs.MenusConfig;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.utils.Messages;
import com.github.gavvydizzle.petsplugin.utils.PDCUtils;
import com.github.gavvydizzle.petsplugin.utils.Sounds;
import com.github.mittenmc.serverutils.ColoredItems;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.ConfigUtils;
import com.github.mittenmc.serverutils.Numbers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * Where players choose their selected pet
 */
public class PetMenu implements ClickableMenu {

    private String inventoryName;
    private int inventorySize, petSlot;
    private ItemStack filler;
    private ItemStack[] defaultItems;

    private final InventoryManager inventoryManager;
    private final PetManager petManager;
    private final ItemStack brokenPetItem;

    public PetMenu(InventoryManager inventoryManager, PetManager petManager) {
        this.inventoryManager = inventoryManager;
        this.petManager = petManager;

        brokenPetItem = new ItemStack(Material.PAPER);
        ItemMeta meta = brokenPetItem.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv("&cInvalid pet :("));
        brokenPetItem.setItemMeta(meta);

        reload();
    }

    public void reload() {
        FileConfiguration config = MenusConfig.get();
        config.options().copyDefaults(true);

        config.addDefault("selection_menu.name", "Pet Select");
        config.addDefault("selection_menu.rows", 3);
        config.addDefault("selection_menu.filler", "white");
        config.addDefault("selection_menu.petSlot", 13);

        config.addDefault("selection_menu.help.slot", 0);
        config.addDefault("selection_menu.help.material", "PAPER");
        config.addDefault("selection_menu.help.name", "&ePet Selection Help");
        config.addDefault("selection_menu.help.lore", Arrays.asList("", "&aClick on a pet in your inventory to select it", "&eClick on the pet in this menu to deselect it"));

        MenusConfig.save();

        inventoryName = Colors.conv(config.getString("selection_menu.name"));
        inventorySize = Numbers.constrain(config.getInt("selection_menu.rows"), 1, 6) * 9;
        filler = ColoredItems.getGlassByName(config.getString("selection_menu.filler"));

        petSlot = config.getInt("selection_menu.petSlot");
        if (!Numbers.isWithinRange(petSlot, 0, inventorySize-1)) {
            petSlot = inventorySize / 2;
            PetsPlugin.getInstance().getLogger().warning("The pet slot was out of bounds for 'selection_menu.petSlot' in menus.yml. By default, it was set to " + petSlot);
        }

        ItemStack helpItem = new ItemStack(ConfigUtils.getMaterial(config.getString("selection_menu.help.material"), Material.PAPER));
        ItemMeta meta = filler.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv(config.getString("selection_menu.help.name")));
        meta.setLore(Colors.conv(config.getStringList("selection_menu.help.lore")));
        helpItem.setItemMeta(meta);

        defaultItems = new ItemStack[inventorySize];
        for (int i = 0; i < inventorySize; i++) {
            defaultItems[i] = filler;
        }

        int helpSlot = config.getInt("selection_menu.help.slot");
        if (Numbers.isWithinRange(helpSlot, 0, inventorySize-1)) {
            defaultItems[helpSlot] = helpItem;
        }
    }

    @Override
    public void openInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(player, inventorySize, inventoryName);
        inventory.setContents(defaultItems);

        Pet pet = petManager.getSelectedPet(player);
        if (pet != null) {
            inventory.setItem(petSlot, pet.getItemStack(player, petManager.getSelectedPetXP(player)));
        }
        else {
            if (petManager.hasSelectedPet(player)) { // Selected pet has invalid ID
                ItemStack itemStack = brokenPetItem.clone();
                ItemMeta meta = itemStack.getItemMeta();
                assert meta != null;
                meta.setLore(Colors.conv(Collections.singletonList("&7id=" + petManager.getSelectedPetID(player))));
                itemStack.setItemMeta(meta);
                inventory.setItem(petSlot, itemStack);
            }
        }

        inventoryManager.onMenuOpen(player, this);
        player.openInventory(inventory);
    }

    @Override
    public void closeInventory(Player player) {

    }

    @Override
    public void handleClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory topInv = e.getView().getTopInventory();
        Inventory pInv = player.getInventory();

        // Remove selected pet if clicked
        if (e.getClickedInventory() == topInv) {
            if (e.getSlot() == petSlot && Objects.requireNonNull(topInv.getItem(petSlot)).getType() == Material.PLAYER_HEAD) {
                if (pInv.firstEmpty() == -1) {
                    player.sendMessage(Messages.inventoryFullOnPetSelect);
                    Sounds.generalFailSound.playSound(player);
                }
                else {
                    if (petManager.isOnSwapCooldown(player)) {
                        player.sendMessage(Messages.swappingPetTooFast);
                        Sounds.generalFailSound.playSound(player);
                        return;
                    }

                    pInv.addItem(topInv.getItem(petSlot));
                    topInv.setItem(petSlot, filler);
                    petManager.onPetSelect(player, null);
                    Sounds.generalClickSound.playSound(player);
                }
            }
        }
        // Select clicked pet from player inventory
        else {
            ItemStack clickedPetItemStack =  pInv.getItem(e.getSlot());
            if (clickedPetItemStack == null || clickedPetItemStack.getType() == Material.AIR) return;

            // Check to see if the petID exists
            String petID = PDCUtils.getPetId(clickedPetItemStack);
            if (petID == null) return;
            else if (petManager.isInvalidPet(petID)) {
                player.sendMessage(Messages.invalidPetSelect.replace("{id}", petID));
                Sounds.generalFailSound.playSound(player);
                return;
            }

            // Check to see if the player is the owner of the pet
            if (petManager.isCheckingPetOwner()) {
                UUID ownerUUID = PDCUtils.getOwnerUUID(clickedPetItemStack);
                if (ownerUUID == null || !ownerUUID.equals(player.getUniqueId())) {
                    player.sendMessage(Messages.notPetOwnerOnSelect);
                    Sounds.generalFailSound.playSound(player);
                    return;
                }
            }

            // Pet is now valid

            // Check for quick swap
            if (petManager.isOnSwapCooldown(player)) {
                player.sendMessage(Messages.swappingPetTooFast);
                Sounds.generalFailSound.playSound(player);
                return;
            }

            // Collect old selected pet if one exists
            Pet pet = petManager.getSelectedPet(player);
            ItemStack selectedPetItem = null;
            if (pet == null) {
                if (petManager.hasSelectedPet(player)) { // Selected pet has invalid ID
                    player.sendMessage(Messages.selectedPetInvalid.replace("{id}", petManager.getSelectedPetID(player)));
                    return;
                }
            }
            else {
                selectedPetItem = topInv.getItem(petSlot);
            }

            // Move new pet
            topInv.setItem(petSlot, clickedPetItemStack);
            pInv.setItem(e.getSlot(), selectedPetItem != null ? selectedPetItem : new ItemStack(Material.AIR));
            petManager.onPetSelect(player, clickedPetItemStack);
            Sounds.generalClickSound.playSound(player);
        }
    }

}

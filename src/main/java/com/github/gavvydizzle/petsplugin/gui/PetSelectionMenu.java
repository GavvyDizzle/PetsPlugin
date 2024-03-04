package com.github.gavvydizzle.petsplugin.gui;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.gui.view.MenuType;
import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.player.LoadedPlayer;
import com.github.gavvydizzle.petsplugin.player.PetHolder;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.pets.SelectedPet;
import com.github.gavvydizzle.petsplugin.player.PlayerManager;
import com.github.gavvydizzle.petsplugin.utils.Messages;
import com.github.gavvydizzle.petsplugin.utils.PDCUtils;
import com.github.gavvydizzle.petsplugin.utils.Sounds;
import com.github.mittenmc.serverutils.*;
import com.github.mittenmc.serverutils.gui.ClickableMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Where players choose their selected pet
 */
public class PetSelectionMenu implements ClickableMenu {

    private final MenuType menuType = MenuType.PET_STORAGE;

    private final InventoryManager inventoryManager;
    private final PlayerManager playerManager;
    private final PetManager petManager;
    private final ItemStack brokenPetItem;

    private String inventoryName;
    private int inventorySize;
    private ItemStack filler;
    private List<Integer> petSlots;
    private List<String> permissions;
    private String lockedPetMessage, lockedMenuMessage;

    private boolean isLevelUpToggleEnabled;
    private ItemStack toggleLevelUpMessagesTemplate;
    private int levelUpToggleSlot;
    private String levelUpToggleEnabled, levelUpToggleDisabled;

    boolean invalidState, blockSaving;
    private final Map<UUID, Integer> permissionsMetMap;
    private final Set<UUID> viewOnlyPlayers;

    public PetSelectionMenu(InventoryManager inventoryManager, PlayerManager playerManager, PetManager petManager) {
        this.inventoryManager = inventoryManager;
        this.playerManager = playerManager;
        this.petManager = petManager;

        permissionsMetMap = new HashMap<>();
        viewOnlyPlayers = new HashSet<>();

        brokenPetItem = new ItemStack(Material.PAPER);
        ItemMeta meta = brokenPetItem.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv("&cInvalid pet :("));
        brokenPetItem.setItemMeta(meta);

        reload(true);
    }

    /**
     * Closes all open inventories and saves data before reloading the plugin
     */
    private void closeOpenInventories() {
        if (inventoryManager != null) {
            inventoryManager.forceUpdateOpenSelectionMenus();
            inventoryManager.closeAllInventories();
            inventoryManager.getPetMenu().unblockSaving();
        }
    }

    public void reload(boolean firstLoad) {
        if (!firstLoad) closeOpenInventories();

        FileConfiguration config = PetsPlugin.getConfigManager().get("menus");
        if (config == null) return;

        config.addDefault("selection_menu.name", "Pet Select");
        config.addDefault("selection_menu.rows", 3);
        config.addDefault("selection_menu.filler", "white");
        config.addDefault("selection_menu.lockedPetMessage", "&cYou must wait {time} before removing this pet");
        config.addDefault("selection_menu.lockedMenuMessage", "&cUnable to edit pets at this time");
        config.addDefault("selection_menu.slots", Arrays.asList(12,13,14,21,22,23,30,31,32));
        config.addDefault("selection_menu.permissions", new ArrayList<>());

        config.addDefault("selection_menu.items.toggle_level_up_messages.placeholder.enabled", "&aEnabled");
        config.addDefault("selection_menu.items.toggle_level_up_messages.placeholder.disabled", "&cDisabled");
        config.addDefault("selection_menu.items.toggle_level_up_messages.slot", 8);
        config.addDefault("selection_menu.items.toggle_level_up_messages.material", Material.OAK_SIGN.name());
        config.addDefault("selection_menu.items.toggle_level_up_messages.name", "&eToggle Level Up Messages");
        config.addDefault("selection_menu.items.toggle_level_up_messages.lore", List.of("&7Status: {status}"));

        inventoryName = Colors.conv(config.getString("selection_menu.name"));
        inventorySize = Numbers.constrain(config.getInt("selection_menu.rows"), 1, 6) * 9;
        filler = ColoredItems.getGlassByName(config.getString("selection_menu.filler"));
        lockedPetMessage = Colors.conv(config.getString("selection_menu.lockedPetMessage"));
        lockedMenuMessage = Colors.conv(config.getString("selection_menu.lockedMenuMessage"));
        petSlots = config.getIntegerList("selection_menu.slots");
        permissions = config.getStringList("selection_menu.permissions");

        invalidState = false;

        for (int n : petSlots) {
            if (!Numbers.isWithinRange(n, 0, inventorySize-1)) {
                invalidState = true;
                PetsPlugin.getInstance().getLogger().warning("The slot '" + n + "' is out of bounds. This menu will be view-only (selection_menu menus.yml)");
            }
        }

        if (petSlots.size() != permissions.size()) {
            invalidState = true;
            PetsPlugin.getInstance().getLogger().warning("The number of slots and permissions do not match. This menu will be view-only (selection_menu in menus.yml)");
        }

        toggleLevelUpMessagesTemplate = ConfigUtils.getItemStack(config.getConfigurationSection("selection_menu.items.toggle_level_up_messages"), "menus.yml", PetsPlugin.getInstance().getLogger());
        levelUpToggleSlot = Numbers.constrain(config.getInt("selection_menu.items.toggle_level_up_messages.slot"), 0, inventorySize-1);
        levelUpToggleEnabled = Colors.conv(config.getString("selection_menu.items.toggle_level_up_messages.placeholder.enabled"));
        levelUpToggleDisabled = Colors.conv(config.getString("selection_menu.items.toggle_level_up_messages.placeholder.disabled"));

        isLevelUpToggleEnabled = true;
        if (petSlots.contains(levelUpToggleSlot)) {
            PetsPlugin.getInstance().getLogger().warning("The level up toggle slot is a pet slot. It will not appear");
            isLevelUpToggleEnabled = false;
        }
    }

    /**
     * Fills the inventory with pet items belonging to this player
     * @param inventory The inventory to edit
     * @param loadedPlayer The player whose data to use
     */
    private void fillInventory(Inventory inventory, LoadedPlayer loadedPlayer) {
        PetHolder petHolder = loadedPlayer.getPetHolder();

        int numSlots = getNumPermissionsMet(loadedPlayer);
        permissionsMetMap.put(loadedPlayer.getUuid(), numSlots);
        List<Integer> effectiveSlots = petSlots.subList(0, numSlots);

        for (int i = 0; i < inventorySize; i++) {
            if (!effectiveSlots.contains(i)) inventory.setItem(i, filler);
            else inventory.setItem(i, null);
        }

        if (isLevelUpToggleEnabled) {
            setLevelUpToggleItem(inventory, loadedPlayer);
        }

        SelectedPet[] arr = petHolder.getEntries();
        for (int i = 0; i < Math.min(numSlots, arr.length) ; i++) {
            SelectedPet selectedPet = arr[i];
            if (selectedPet == null) continue;

            Pet pet = petManager.getFromSelectedPet(selectedPet);
            if (pet != null) {
                inventory.setItem(petSlots.get(i), pet.getItemStack(loadedPlayer.getUuid(), selectedPet.getXp(), selectedPet.getLastUseTime()));
            }
            else {
                // Selected pet has invalid ID
                ItemStack itemStack = brokenPetItem.clone();
                ItemMeta meta = itemStack.getItemMeta();
                assert meta != null;
                meta.setLore(Colors.conv(Collections.singletonList("&7id=" + selectedPet.getPetID())));
                itemStack.setItemMeta(meta);

                // Adding data to the invalid item is necessary to cause the data to persist
                PDCUtils.setPetId(itemStack, selectedPet.getPetID());
                PDCUtils.setOwner(itemStack, loadedPlayer.getUuid());
                PDCUtils.setXP(itemStack, selectedPet.getXp());
                PDCUtils.setUseTime(itemStack, selectedPet.getLastUseTime());
                PDCUtils.setRandomKey(itemStack);

                inventory.setItem(petSlots.get(i), itemStack);
            }
        }
    }

    /**
     * Generates the level up messages toggle item and places it in this inventory
     * @param loadedPlayer The LoadedPlayer
     */
    private void setLevelUpToggleItem(Inventory inventory, LoadedPlayer loadedPlayer) {
        ItemStack itemStack = toggleLevelUpMessagesTemplate.clone();

        Map<String, String> map = new HashMap<>(1);
        map.put("{status}", loadedPlayer.areLevelUpMessagesOn() ? levelUpToggleEnabled : levelUpToggleDisabled);

        ItemStackUtils.replacePlaceholders(itemStack, map);
        inventory.setItem(levelUpToggleSlot, itemStack);
    }

    @Override
    public void openInventory(Player player) {
        // Since saving relies on the inventory close event, it is possible to bypass it some instances (mods/clients)
        // This check ensures that the menu saves itself if the player has closed the menu without sending an inventory close packet
        if (permissionsMetMap.containsKey(player.getUniqueId())) {
            player.closeInventory();
        }

        LoadedPlayer loadedPlayer = playerManager.getLoadedPlayer(player);
        if (loadedPlayer == null) return;

        Inventory inventory = Bukkit.createInventory(player, inventorySize, inventoryName);
        fillInventory(inventory, loadedPlayer);

        player.openInventory(inventory);
        inventoryManager.onMenuOpen(player, this);
        loadedPlayer.getProfileViewers().addViewer(player.getUniqueId(), menuType);
    }

    public void adminOpenInventory(Player admin, LoadedPlayer loadedPlayer) {
        Inventory inventory = Bukkit.createInventory(admin, inventorySize, loadedPlayer.getName() + "'s " + inventoryName);
        fillInventory(inventory, loadedPlayer);

        admin.openInventory(inventory);
        inventoryManager.onAdminMenuOpen(admin, loadedPlayer, this);
        loadedPlayer.getProfileViewers().addViewer(admin.getUniqueId(), menuType);
    }

    /**
     * Opens a view only copy of a player's inventory
     * @param player The player to open the inventory for
     * @param loadedPlayer The player whose data is showing
     */
    public void openViewOnlyInventory(Player player, LoadedPlayer loadedPlayer) {
        Inventory inventory = Bukkit.createInventory(player, inventorySize, loadedPlayer.getName() + "'s " + inventoryName);
        fillInventory(inventory, loadedPlayer);

        player.openInventory(inventory);
        inventoryManager.onMenuOpen(player, this);
        viewOnlyPlayers.add(player.getUniqueId());
    }

    @Override
    public void closeInventory(Player player) {
        // If this player is in view only mode, no saving needs to take place
        if (viewOnlyPlayers.remove(player.getUniqueId())) return;

        int numSlots = permissionsMetMap.remove(player.getUniqueId());

        // Don't allow the menu to save if there is an error present
        if (invalidState) return;

        LoadedPlayer loadedPlayer = playerManager.getLoadedPlayer(player);
        if (loadedPlayer == null) return;

        PetHolder petHolder = loadedPlayer.getPetHolder();

        Inventory inventory = player.getOpenInventory().getTopInventory();
        List<Integer> effectiveSlots = petSlots.subList(0, numSlots);
        ItemStack[] currentItems = new ItemStack[PetHolder.getMaxPets()];

        for (int i = 0; i < Math.min(numSlots, currentItems.length); i++) {
            currentItems[i] = inventory.getItem(effectiveSlots.get(i));
        }

        SelectedPet[] oldSelectedPets = petHolder.getEntries().clone();
        System.out.println("OLD: " + Arrays.toString(oldSelectedPets));
        if (petHolder.updateStoredContents(currentItems)) {
            petManager.onPetUpdate(player, oldSelectedPets, petHolder.getEntries());
        }
        System.out.println("NEW: " + Arrays.toString(petHolder.getEntries()));
        petHolder.onInventoryClose();
        loadedPlayer.getProfileViewers().removeViewer(player.getUniqueId());

        cleanInventoryItems(player);
    }

    public void adminCloseInventory(Player admin, LoadedPlayer loadedPlayer) {
        int numSlots = getNumOfflinePermissionsMet();

        // Don't allow the menu to save if there is an error present
        if (invalidState) return;

        PetHolder petHolder = loadedPlayer.getPetHolder();

        Inventory inventory = admin.getOpenInventory().getTopInventory();
        List<Integer> effectiveSlots = petSlots.subList(0, numSlots);
        ItemStack[] currentItems = new ItemStack[PetHolder.getMaxPets()];

        for (int i = 0; i < Math.min(numSlots, currentItems.length); i++) {
            currentItems[i] = inventory.getItem(effectiveSlots.get(i));
        }

        SelectedPet[] oldSelectedPets = petHolder.getEntries().clone();
        if (petHolder.updateStoredContents(currentItems)) {
            if (loadedPlayer.getPlayer() != null) petManager.onPetUpdate(loadedPlayer.getPlayer(), oldSelectedPets, petHolder.getEntries());
        }
        petHolder.onInventoryClose();
        loadedPlayer.getProfileViewers().removeViewer(admin.getUniqueId());

        cleanInventoryItems(admin);
    }

    /**
     * Pushes any outstanding updates if this menu is still open.
     * This will only cause changes for the unlocked viewer of the menu.
     * This will block the menu from future saving.
     * @param player The player
     * @param loadedPlayer The player whose data this is
     */
    public void pushUpdates(Player player, LoadedPlayer loadedPlayer) {
        if (loadedPlayer == null) return;

        int numSlots = getNumPermissionsMet(loadedPlayer);

        // Don't allow the menu to save if there is an error present
        if (invalidState || blockSaving) return;

        // Only allow the "first" viewer to save their data
        if (loadedPlayer.getProfileViewers().isMenuLocked(player.getUniqueId())) return;
        blockSaving = true;

        PetHolder petHolder = loadedPlayer.getPetHolder();

        Inventory inventory = player.getOpenInventory().getTopInventory();
        List<Integer> effectiveSlots = petSlots.subList(0, numSlots);
        ItemStack[] currentItems = new ItemStack[PetHolder.getMaxPets()];

        for (int i = 0; i < Math.min(numSlots, currentItems.length); i++) {
            currentItems[i] = inventory.getItem(effectiveSlots.get(i));
        }

        // Further shutdown methods will handle the saving aspect
        petHolder.updateStoredContents(currentItems);
        cleanInventoryItems(player);
    }

    /**
     * Removes any time data from items in the player's inventory.
     * This is to be called when a player closes this menu to remove the data from any newly removed pets.
     * @param player The player
     */
    private void cleanInventoryItems(Player player) {
        Inventory inventory = player.getInventory();
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null) PDCUtils.removeTimeKey(itemStack);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent e) {
        // Cancel the click and set to false if it is a valid click
        e.setCancelled(true);

        if (viewOnlyPlayers.contains(e.getWhoClicked().getUniqueId())) return;

        boolean clickedTopMenu = e.getClickedInventory() == e.getView().getTopInventory();

        if (invalidState) return;

        // Don't allow this type of click because it's annoying to validate for
        if (e.getClick() == ClickType.NUMBER_KEY) return;

        // Don't allow the player to drop items since it can bypass the lock mechanism
        if (e.getClick() == ClickType.DROP || e.getClick() == ClickType.CONTROL_DROP) return;

        Player player = (Player) e.getWhoClicked();
        LoadedPlayer loadedPlayer = playerManager.getLoadedPlayer(player);
        if (loadedPlayer == null) return;

        // Check if the player clicked some other item in the menu
        if (clickedTopMenu) {
            if (isLevelUpToggleEnabled && e.getSlot() == levelUpToggleSlot) {
                loadedPlayer.toggleLevelUpMessages();
                setLevelUpToggleItem(e.getView().getTopInventory(), loadedPlayer);
                Sounds.generalClickSound.playSound(player);
                return;
            }
        }

        if (loadedPlayer.getProfileViewers().isMenuLocked(player.getUniqueId())) {
            e.setCancelled(true);
            if (e.getClick() != ClickType.SHIFT_LEFT) { // Ignore spammy shift + double left click
                player.sendMessage(lockedMenuMessage);
                Sounds.generalFailSound.playSound(player);
            }
            return;
        }

        // Make sure the player is interacting with or holding a pet
        ItemStack clickedItem = e.getCurrentItem();
        Material clickedMaterial = clickedItem == null ? Material.AIR : clickedItem.getType();
        ItemStack cursorItem = e.getCursor();
        boolean isClickedItemPet = clickedItem != null && PDCUtils.isPet(clickedItem);
        boolean isCursorItemPet = cursorItem != null && PDCUtils.isPet(cursorItem);

        // Allow the click through if the item is a pet
        // or the held item is a pet and the clicked item is AIR
        if (!(isClickedItemPet || (isCursorItemPet && clickedMaterial.isAir()))) return;

        if (clickedItem == null) {
            e.setCancelled(false);
            return;
        }

        // Check to see if the petID exists
        String petID = PDCUtils.getPetId(clickedItem);
        if (petID == null) {
            return;
        }
        else if (petManager.isInvalidPet(petID)) {
            player.sendMessage(Messages.invalidPetSelect.replace("{id}", petID));
            Sounds.generalFailSound.playSound(player);
            return;
        }

        Pet pet = petManager.getPet(petID);
        if (pet == null) return;

        // Lastly, cancel any click done to a locked pet
        // Only cancel the click when the pet is in the top menu
        if (clickedTopMenu) {
            long millisRemaining = pet.getLockDurationMilliseconds() - PDCUtils.getTimeSinceLastUse(clickedItem);

            if (millisRemaining > 0) {
                e.getWhoClicked().sendMessage(lockedPetMessage.replace("{time}", Numbers.getTimeFormatted((int) (millisRemaining / 1000))));
                Sounds.generalFailSound.playSound(player);
                return;
            }
        }
        else {
            // Stop the player from moving a pet they don't have permission for only when it is in their inventory
            if (!pet.hasPermission(player)) {
                pet.sendPermissionDeniedMessage(player);
                Sounds.generalFailSound.playSound(player);
                return;
            }
            // Or stop the update if the player is not the owner
            else if (petManager.isCheckingPetOwner()) {
                UUID ownerUUID = PDCUtils.getOwnerUUID(clickedItem);
                if (ownerUUID == null || !ownerUUID.equals(player.getUniqueId())) {
                    player.sendMessage(Messages.notPetOwnerOnSelect);
                    Sounds.generalFailSound.playSound(player);
                    return;
                }
            }
        }

        e.setCancelled(false);
    }

    public void handleAdminClick(InventoryClickEvent e, LoadedPlayer loadedPlayer) {
        // Cancel the click and set to false if it is a valid click
        e.setCancelled(true);

        boolean clickedTopMenu = e.getClickedInventory() == e.getView().getTopInventory();

        if (invalidState) return;

        // Don't allow this type of click because it's annoying to validate for
        if (e.getClick() == ClickType.NUMBER_KEY) return;

        // Don't allow the player to drop items since it can bypass the lock mechanism
        if (e.getClick() == ClickType.DROP || e.getClick() == ClickType.CONTROL_DROP) return;

        Player admin = (Player) e.getWhoClicked();
        if (loadedPlayer.getProfileViewers().isMenuLocked(admin.getUniqueId())) {
            e.setCancelled(true);
            if (e.getClick() != ClickType.SHIFT_LEFT) { // Ignore spammy shift + double left click
                admin.sendMessage(lockedMenuMessage);
                Sounds.generalFailSound.playSound(admin);
            }
            return;
        }

        // Make sure the player is interacting with or holding a pet
        ItemStack clickedItem = e.getCurrentItem();
        Material clickedMaterial = clickedItem == null ? Material.AIR : clickedItem.getType();
        ItemStack cursorItem = e.getCursor();
        boolean isClickedItemPet = clickedItem != null && PDCUtils.isPet(clickedItem);
        boolean isCursorItemPet = cursorItem != null && PDCUtils.isPet(cursorItem);

        // Allow the click through if the item is a pet
        // or the held item is a pet and the clicked item is AIR
        if (!(isClickedItemPet || (isCursorItemPet && clickedMaterial.isAir()))) return;

        if (clickedItem == null) {
            e.setCancelled(false);
            return;
        }

        // Check to see if the petID exists
        String petID = PDCUtils.getPetId(clickedItem);
        if (petID == null) {
            return;
        }
        else if (petManager.isInvalidPet(petID)) {
            if (e.isRightClick()) {
                e.setCancelled(false);
            }
            else {
                admin.sendMessage(Messages.invalidPetSelect.replace("{id}", petID));
                admin.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + " - Admins can bypass invalid items with a right-click");
                Sounds.generalFailSound.playSound(admin);
            }
            return;
        }

        Pet pet = petManager.getPet(petID);
        if (pet == null) return;

        // Lastly, cancel any click done to a locked pet
        // Only cancel the click when the pet is in the top menu
        if (clickedTopMenu) {
            long millisRemaining = pet.getLockDurationMilliseconds() - PDCUtils.getTimeSinceLastUse(clickedItem);

            // Allow admins to bypass the GPU lock with a right-click
            if (millisRemaining > 0 && !e.isRightClick()) {
                admin.sendMessage(lockedPetMessage.replace("{time}", Numbers.getTimeFormatted((int) (millisRemaining / 1000))));
                admin.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + " - Admins can bypass this lock with a right-click");
                Sounds.generalFailSound.playSound((Player) e.getWhoClicked());
                e.setCancelled(true);
                return;
            }
        }

        e.setCancelled(false);
    }

    private int getNumPermissionsMet(Player player) {
        int total = 0;
        for (String permission : permissions) {
            if (player.hasPermission(permission)) total++;
        }
        return total;
    }

    private int getNumPermissionsMet(LoadedPlayer lp) {
        return lp.getPlayer() != null ? getNumPermissionsMet(lp.getPlayer()) : getNumOfflinePermissionsMet();
    }

    // Offline permissions are a pain, so just give admins access to all of a player's pet slots.
    // They can be stupid and have pets deleted, but that's not my problem anymore
    private int getNumOfflinePermissionsMet() {
        return permissions.size();
    }

    /**
     * Refreshes the menu when the menu becomes unlocked for this player
     * @param player The viewer
     * @param loadedPlayer The player whose data to show
     */
    public void refreshMenu(Player player, LoadedPlayer loadedPlayer) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        fillInventory(inventory, loadedPlayer);
    }

    public void unblockSaving() {
        blockSaving = false;
    }
}

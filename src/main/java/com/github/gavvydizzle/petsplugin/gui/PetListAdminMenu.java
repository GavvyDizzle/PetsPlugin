package com.github.gavvydizzle.petsplugin.gui;

import com.github.gavvydizzle.petsplugin.pets.Pet;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.utils.Sounds;
import com.github.mittenmc.serverutils.gui.pages.PagesMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Admin only menu that lists all loaded pets
 */
public class PetListAdminMenu extends PagesMenu<Pet> {

    private final PetManager petManager;

    public PetListAdminMenu(PetManager petManager) {
        super("Pets List");

        this.petManager = petManager;
        reloadContents();
    }

    /**
     * Reloads the contents of this menu.
     * This method should be called after all pets have been reloaded.
     */
    public void reloadContents() {
        super.setItems(petManager.getLoadedPets());
    }

    @Override
    public void onPageUp(Player player) {
        super.onPageUp(player);
        Sounds.generalClickSound.playSound(player);
    }

    @Override
    public void onPageDown(Player player) {
        super.onPageDown(player);
        Sounds.generalClickSound.playSound(player);
    }

    /**
     * Handles what happens when the player clicks a custom item.
     * @param e The original click event
     * @param player The player
     * @param item The clicked item
     */
    @Override
    public void onItemClick(InventoryClickEvent e, Player player, Pet item) {
        super.onItemClick(e, player, item);

        player.sendMessage(ChatColor.GREEN + "You received a new " + item.getId() + " pet");
        Sounds.generalClickSound.playSound(player);
    }
}

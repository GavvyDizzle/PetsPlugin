package com.github.gavvydizzle.petsplugin.player;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.pets.SelectedPet;
import com.github.gavvydizzle.petsplugin.utils.PDCUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Keeps track of the pets equipped by each player
 */
public class PetHolder {

    private static final int MAX_PETS = 9;

    private SelectedPet[] pets;
    private final LoadedPlayer loadedPlayer;
    private boolean isDirty;

    public PetHolder(LoadedPlayer loadedPlayer, @Nullable SelectedPet[] pets) {
        this.loadedPlayer = loadedPlayer;
        this.pets = pets == null ? new SelectedPet[MAX_PETS] : pets;
    }

    /**
     * Updates all items if they have changed
     * @param current The set of items from the inventory
     * @return If the contents have changed
     */
    public boolean updateStoredContents(ItemStack[] current) {
        boolean ret = false;

        for (int i = 0; i < MAX_PETS; i++) {
            SelectedPet newSelectedPet = PDCUtils.createSelectedPet(current[i]);

            if (!Objects.equals(newSelectedPet, pets[i])) {
                pets[i] = newSelectedPet;
                isDirty = true;
                ret = true;

                // If the pet does not have a last use time attached, set it to now
                if (current[i] != null && !PDCUtils.hasTimeKey(current[i])) {
                    SelectedPet pet = pets[i];
                    if (pet != null) {
                        pet.setLastUseTime();
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Should be called when the inventory gets closed.
     * This will push any outstanding updates to the database.
     */
    public void onInventoryClose() {
        if (isDirty) {
            Bukkit.getServer().getScheduler().runTaskAsynchronously(PetsPlugin.getInstance(), () -> {
                PetsPlugin.getInstance().getData().saveSelectedPets(loadedPlayer);
                isDirty = false;
            });
        }
    }

    /**
     * Removes all items from the in-memory portion of this inventory
     */
    public void clearContents() {
        pets = new SelectedPet[MAX_PETS];
    }

    public SelectedPet[] getEntries() {
        return pets;
    }

    public static int getMaxPets() {
        return MAX_PETS;
    }

    public boolean isDirty() {
        return isDirty;
    }
}

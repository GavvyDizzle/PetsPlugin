package com.github.gavvydizzle.petsplugin.player;

import com.github.gavvydizzle.petsplugin.gui.view.ProfileViewers;
import com.github.gavvydizzle.petsplugin.pets.SelectedPet;
import com.github.mittenmc.serverutils.PlayerNameCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class LoadedPlayer {

    @Nullable
    private Player player;
    private final UUID uuid;
    private final PetHolder petHolder;
    private final ProfileViewers profileViewers;

    private boolean areLevelUpMessagesOn;

    public LoadedPlayer(UUID uuid, @Nullable SelectedPet[] selectedPets, boolean areLevelUpMessagesOn) {
        this.player = null;
        this.areLevelUpMessagesOn = areLevelUpMessagesOn;
        this.uuid = uuid;
        this.petHolder = new PetHolder(this, selectedPets);
        profileViewers = new ProfileViewers(this);
    }

    /**
     * Messages the player if they are online
     * @param messages The messages to send
     */
    public void sendMessage(String ... messages) {
        if (player != null) player.sendMessage(messages);
    }

    /**
     * Resets a player's data by setting their coin amounts to 0 and removing all items
     */
    public void resetPlayer() {
        petHolder.clearContents();
    }

    /**
     * @return The player's name whether they are online or offline
     */
    public String getName() {
        if (player != null) return player.getName();
        return PlayerNameCache.get(uuid);
    }

    public void logon() {
        player = Bukkit.getPlayer(uuid);
    }

    public void logout() {
        player = null;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean areLevelUpMessagesOn() {
        return areLevelUpMessagesOn;
    }

    public void toggleLevelUpMessages() {
        areLevelUpMessagesOn = !areLevelUpMessagesOn;
    }

    /**
     * Requests the player from the server.
     * Since the player may not be online, this is not safe to use unless you know the player is online
     * @return The player object
     */
    @Nullable
    public Player getPlayer() {
        return player;
    }

    public PetHolder getPetHolder() {
        return petHolder;
    }

    public ProfileViewers getProfileViewers() {
        return profileViewers;
    }

}

package com.github.gavvydizzle.petsplugin.storage;

import com.github.gavvydizzle.petsplugin.pets.SelectedPet;
import com.github.gavvydizzle.petsplugin.player.LoadedPlayer;
import com.github.gavvydizzle.petsplugin.player.PetHolder;
import com.github.mittenmc.serverutils.UUIDConverter;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class PlayerData extends PluginDataHolder {

    private final static String profileTableName = "profile_data";
    private final static String petsTableName = "selected_pets";

    private final static String LOAD_PROFILE_DATA = "SELECT * FROM " + profileTableName + " WHERE uuid=?;";
    private final static String LOAD_SELECTED_PETS = "SELECT * FROM " + petsTableName + " WHERE uuid=?;";

    private final static String SAVE_PROFILE_DATA = "REPLACE INTO " + profileTableName + "(uuid, level_up_messages) VALUES(?,?);";
    private final static String SAVE_SELECTED_PET = "REPLACE INTO " + petsTableName + "(uuid, slot, petID, xp, timestamp) VALUES(?,?,?,?,?);";

    private final static String DELETE_SELECTED_PET = "DELETE FROM " + petsTableName + " WHERE uuid=? AND slot=?;";
    private final static String DELETE_ALL_DATA = "DELETE FROM " + petsTableName;


    /**
     * Create a new {@link PluginDataHolder} with a datasource to server connections and a plugin for logging.
     *
     * @param plugin plugin for logging
     * @param source source to provide connections.
     */
    public PlayerData(Plugin plugin, DataSource source) {
        super(plugin, source);
    }

    /**
     * Gets the player's profile info and selected pets.
     * @param player The player
     * @return A player's data
     */
    @Nullable
    public LoadedPlayer getPlayerInfo(OfflinePlayer player) {
        Connection conn;
        try {
            conn = conn();
        } catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return null;
        }

        boolean levelUpMessages;
        SelectedPet[] selectedPets = new SelectedPet[PetHolder.getMaxPets()];

        try {
            PreparedStatement stmt = conn.prepareStatement(LOAD_PROFILE_DATA);
            stmt.setBytes(1, UUIDConverter.convert(player.getUniqueId()));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                levelUpMessages = rs.getBoolean("level_up_messages");
            }
            else {
                levelUpMessages = true;
            }

        } catch (SQLException e) {
            logSQLError("Failed to load profile data.", e);
            return null;
        }

        try {
            PreparedStatement stmt = conn.prepareStatement(LOAD_SELECTED_PETS);
            stmt.setBytes(1, UUIDConverter.convert(player.getUniqueId()));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int slot = rs.getInt("slot");
                if (slot < 0 || slot >= selectedPets.length) continue;

                selectedPets[slot] = new SelectedPet(rs.getString("petID"), rs.getDouble("xp"), rs.getLong("timestamp"));
            }

        } catch (SQLException e) {
            logSQLError("Failed to load stored pets data.", e);
            return null;
        }

        return new LoadedPlayer(player.getUniqueId(), selectedPets, levelUpMessages);
    }

    /**
     * Saves a player's profile data.
     * @param loadedPlayer The LoadedPlayer
     */
    public void saveProfileData(LoadedPlayer loadedPlayer) {
        Connection conn;
        try {
            conn = conn();
        } catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(SAVE_PROFILE_DATA);
            ps.setBytes(1, UUIDConverter.convert(loadedPlayer.getUuid()));
            ps.setBoolean(2, loadedPlayer.areLevelUpMessagesOn());
            ps.execute();

        } catch (SQLException e) {
            logSQLError("Could not save profile data", e);
        }
    }

    /**
     * Saves all players' profile data.
     * @param loadedPlayers A list of LoadedPlayers
     */
    public boolean saveProfileData(Collection<LoadedPlayer> loadedPlayers) {
        Connection conn;
        try {
            conn = conn();
        } catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return false;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(SAVE_PROFILE_DATA);
            for (LoadedPlayer loadedPlayer : loadedPlayers) {
                ps.setBytes(1, UUIDConverter.convert(loadedPlayer.getUuid()));
                ps.setBoolean(2, loadedPlayer.areLevelUpMessagesOn());
                ps.addBatch();
            }
            ps.executeBatch();

        } catch (SQLException e) {
            logSQLError("Could not save profile data", e);
            return false;
        }

        return true;
    }

    /**
     * Save a player's selected pets. If a pet is null, then their entry will be removed.
     * If the pet's id is "null" then nothing will change.
     *
     * @param lp The LoadedPlayer
     */
    public void saveSelectedPets(@NotNull LoadedPlayer lp) {
        PetHolder petHolder = lp.getPetHolder();
        if (!lp.getPetHolder().isDirty()) return;

        Connection conn;
        try {
            conn = conn();
        } catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return;
        }

        try {
            PreparedStatement saveStatement = conn.prepareStatement(SAVE_SELECTED_PET);
            PreparedStatement deleteStatement = conn.prepareStatement(DELETE_SELECTED_PET);

            SelectedPet[] arr = petHolder.getEntries();
            for (int i = 0; i < arr.length; i++) {
                SelectedPet selectedPet = arr[i];

                if (selectedPet == null) {
                    deleteStatement.setBytes(1, UUIDConverter.convert(lp.getUuid()));
                    deleteStatement.setInt(2, i);
                    deleteStatement.addBatch();
                }
                else if (!selectedPet.getPetID().equals("null")) { // The null ID is reserved for errors and should not be overwritten
                    saveStatement.setBytes(1, UUIDConverter.convert(lp.getUuid()));
                    saveStatement.setInt(2, i);
                    saveStatement.setString(3, selectedPet.getPetID());
                    saveStatement.setDouble(4, selectedPet.getXp());
                    saveStatement.setLong(5, selectedPet.getLastUseTime());
                    saveStatement.addBatch();
                }
            }

            saveStatement.executeBatch();
            deleteStatement.executeBatch();

        } catch (SQLException e) {
            logSQLError("Could not save selected pet", e);
        }
    }

    /**
     * Save all selected pets. If the pet's id is "null" then nothing will change.
     *
     * @param loadedPlayers A list of LoadedPlayers
     * @return If the data saved successfully
     */
    public boolean saveSelectedPets(Collection<LoadedPlayer> loadedPlayers) {
        if (loadedPlayers.isEmpty()) return true;

        Connection conn;
        try {
            conn = conn();
        } catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return false;
        }

        try {
            PreparedStatement saveStatement = conn.prepareStatement(SAVE_SELECTED_PET);
            PreparedStatement deleteStatement = conn.prepareStatement(DELETE_SELECTED_PET);

            for (LoadedPlayer lp : loadedPlayers) {
                PetHolder petHolder = lp.getPetHolder();
                if (!petHolder.isDirty()) continue;

                SelectedPet[] arr = petHolder.getEntries();
                for (int i = 0; i < arr.length; i++) {
                    SelectedPet selectedPet = arr[i];

                    if (selectedPet == null) {
                        deleteStatement.setBytes(1, UUIDConverter.convert(lp.getUuid()));
                        deleteStatement.setInt(2, i);
                        deleteStatement.addBatch();
                    }
                    else if (!selectedPet.getPetID().equals("null")) { // The null ID is reserved for errors and should not be overwritten
                        saveStatement.setBytes(1, UUIDConverter.convert(lp.getUuid()));
                        saveStatement.setInt(2, i);
                        saveStatement.setString(3, selectedPet.getPetID());
                        saveStatement.setDouble(4, selectedPet.getXp());
                        saveStatement.setLong(5, selectedPet.getLastUseTime());
                        saveStatement.addBatch();
                    }
                }
            }

            saveStatement.executeBatch();
            deleteStatement.executeBatch();
            return true;

        } catch (SQLException e) {
            logSQLError("Could not save all selected pets", e);
            return false;
        }
    }

    /**
     * Deletes all data from the database
     * @return If the data deleted successfully
     */
    public boolean deleteAllData() {
        Connection conn;
        try {
            conn = conn();
        } catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return false;
        }

        try {
            PreparedStatement stmt = conn.prepareStatement(DELETE_ALL_DATA);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            logSQLError("Failed to delete all data", e);
            return false;
        }
    }
}
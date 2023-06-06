package com.github.gavvydizzle.petsplugin.storage;

import com.github.gavvydizzle.petsplugin.pets.SelectedPet;
import com.github.gavvydizzle.petsplugin.utils.Pair;
import com.github.mittenmc.serverutils.UUIDConverter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class PlayerData extends PluginDataHolder {

    private final static String tableName = "selected_pets";

    private final static String INSERT_OR_UPDATE_SELECTED_PET = "INSERT INTO " + tableName + "(uuid, petID, xp) VALUES(?,?,?) " +
            "ON DUPLICATE KEY UPDATE petID=?, xp=?";
    private final static String LOAD_SELECTED_PET = "SELECT * FROM " + tableName + " WHERE uuid = ?";
    private final static String DELETE_SELECTED_PET = "DELETE FROM " + tableName + " WHERE uuid = ?";
    private final static String DELETE_ALL_DATA = "DELETE FROM " + tableName;


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
     * Gets the player's saved selected pet
     *
     * @param player The player
     * @return The player's selected pet and a status ID<p>
     *     0 - Loaded successfully<p>
     *     1 - Database error
     */
    public Pair<SelectedPet, Integer> getSelectedPet(Player player) {
        Connection conn;
        try {
            conn = conn();
        } catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return new Pair<>(null, 1);
        }

        try {
            PreparedStatement stmt = conn.prepareStatement(LOAD_SELECTED_PET);
            stmt.setBytes(1, UUIDConverter.convert(player.getUniqueId()));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Pair<>(new SelectedPet(rs.getString(2), rs.getLong(3)), 0);
            } else {
                return new Pair<>(null, 0);
            }
        } catch (SQLException e) {
            logSQLError("Failed to load stored pet.", e);
            return new Pair<>(null, 1);
        }
    }

    /**
     * Save a player's selected pet. If the pet is null, then their entry will be removed.
     * If the pet's id is "null" then nothing will change.
     *
     * @param uuid        The player's uuid
     * @param selectedPet Their SelectedPet
     */
    public void savePlayerInfo(UUID uuid, @Nullable SelectedPet selectedPet) {
        if (selectedPet != null && selectedPet.getPetID().equalsIgnoreCase("null")) return;

        Connection conn;
        try {
            conn = conn();
        } catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return;
        }

        if (selectedPet != null) {
            try {
                PreparedStatement stmt = conn.prepareStatement(INSERT_OR_UPDATE_SELECTED_PET);
                stmt.setBytes(1, UUIDConverter.convert(uuid));
                stmt.setString(2, selectedPet.getPetID());
                stmt.setLong(3, selectedPet.getXp());
                stmt.setString(4, selectedPet.getPetID());
                stmt.setLong(5, selectedPet.getXp());
                stmt.execute();

            } catch (SQLException e) {
                logSQLError("Could not save selected pet", e);
            }
        } else {
            try {
                PreparedStatement stmt = conn.prepareStatement(DELETE_SELECTED_PET);
                stmt.setBytes(1, UUIDConverter.convert(uuid));
                stmt.execute();

            } catch (SQLException e) {
                logSQLError("Could not delete selected pet", e);
            }
        }
    }

    /**
     * Save all selected pets. If the pet's id is "null" then nothing will change.
     *
     * @param selectedPetHashMap The map of UUIDs and SelectedPets
     * @return If the data saved successfully
     */
    public boolean savePlayerInfo(HashMap<UUID, SelectedPet> selectedPetHashMap) {
        if (selectedPetHashMap.isEmpty()) {
            return true;
        }

        Connection conn;
        try {
            conn = conn();
        } catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return false;
        }

        try {
            PreparedStatement stmt = conn.prepareStatement(INSERT_OR_UPDATE_SELECTED_PET);
            for (UUID uuid : selectedPetHashMap.keySet()) {
                SelectedPet selectedPet = selectedPetHashMap.get(uuid);
                if (selectedPet == null || selectedPet.getPetID().equalsIgnoreCase("null")) continue;

                stmt.setBytes(1, UUIDConverter.convert(uuid));
                stmt.setString(2, selectedPet.getPetID());
                stmt.setLong(3, selectedPet.getXp());
                stmt.setString(4, selectedPet.getPetID());
                stmt.setLong(5, selectedPet.getXp());
                stmt.addBatch();
            }
            stmt.executeBatch();
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
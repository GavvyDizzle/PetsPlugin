package com.github.gavvydizzle.petsplugin;

import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.commands.CommandConfirmationManager;
import com.github.gavvydizzle.petsplugin.commands.PlayerCommandManager;
import com.github.gavvydizzle.petsplugin.gui.InventoryManager;
import com.github.gavvydizzle.petsplugin.pets.PetManager;
import com.github.gavvydizzle.petsplugin.pets.xp.ExperienceManager;
import com.github.gavvydizzle.petsplugin.player.MyRegionManager;
import com.github.gavvydizzle.petsplugin.player.PlayerManager;
import com.github.gavvydizzle.petsplugin.storage.Configuration;
import com.github.gavvydizzle.petsplugin.storage.DataSourceProvider;
import com.github.gavvydizzle.petsplugin.storage.DbSetup;
import com.github.gavvydizzle.petsplugin.storage.PlayerData;
import com.github.gavvydizzle.petsplugin.utils.Messages;
import com.github.gavvydizzle.petsplugin.utils.Sounds;
import com.github.mittenmc.serverutils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

public final class PetsPlugin extends JavaPlugin {

    private static PetsPlugin instance;
    private PlayerData data;
    private static ConfigManager configManager;
    private PlayerManager playerManager;
    private PetManager petManager;
    private ExperienceManager experienceManager;
    private InventoryManager inventoryManager;
    private CommandConfirmationManager commandConfirmationManager;
    private boolean isRewardsInventoryLoaded;

    private DataSource dataSource;
    private boolean mySQLSuccessful;

    @Override
    public void onLoad() {
        generateDefaultConfig();
        Configuration configuration = new Configuration(this);
        mySQLSuccessful = true;

        try {
            dataSource = DataSourceProvider.initMySQLDataSource(this, configuration.getDatabase());
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not establish database connection", e);
            mySQLSuccessful = false;
        }

        try {
            DbSetup.initDb(this, dataSource);
        } catch (SQLException | IOException e) {
            getLogger().log(Level.SEVERE, "Could not init database.", e);
            mySQLSuccessful = false;
        }

        MyRegionManager.initFlags();
    }

    @Override
    public void onEnable() {
        if (!mySQLSuccessful) {
            getLogger().log(Level.SEVERE, "Database connection failed. Disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupConfigFiles();

        instance = this;
        isRewardsInventoryLoaded = getServer().getPluginManager().getPlugin("RewardsInventory") != null;
        data = new PlayerData(this, dataSource);


        playerManager = new PlayerManager(instance, data);
        petManager = new PetManager(instance, playerManager);
        inventoryManager = new InventoryManager(playerManager, petManager);
        experienceManager = new ExperienceManager(instance, playerManager, petManager);
        experienceManager.validateExperienceEntries(Bukkit.getConsoleSender());

        getServer().getPluginManager().registerEvents(playerManager, this);
        getServer().getPluginManager().registerEvents(petManager, this);
        getServer().getPluginManager().registerEvents(inventoryManager, this);
        getServer().getPluginManager().registerEvents(experienceManager, this);

        commandConfirmationManager = new CommandConfirmationManager();

        try {
            new AdminCommandManager(Objects.requireNonNull(getCommand("petsadmin")), commandConfirmationManager, playerManager, petManager, inventoryManager, data);
        } catch (NullPointerException e) {
            getLogger().severe("The admin command name was changed in the plugin.yml file. Please make it \"petsadmin\" and restart the server. You can change the aliases but NOT the command name.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            new PlayerCommandManager(Objects.requireNonNull(getCommand("pets")), inventoryManager);
        } catch (NullPointerException e) {
            getLogger().severe("The player command name was changed in the plugin.yml file. Please make it \"pets\" and restart the server. You can change the aliases but NOT the command name.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Messages.reloadMessages();
        Sounds.reload();

        // Save all configs after everything is done loading
        configManager.saveAll();
    }

    @Override
    public void onDisable() {
        if (inventoryManager != null) {
            inventoryManager.forceUpdateOpenSelectionMenus();
            inventoryManager.closeAllInventories();
        }

        if (petManager != null) petManager.onShutdown();
        if (playerManager != null) playerManager.onShutdown();

        HandlerList.unregisterAll(this);
    }

    private void generateDefaultConfig() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);

        config.addDefault("database.host", "TODO");
        config.addDefault("database.port", 3306);
        config.addDefault("database.user", "TODO");
        config.addDefault("database.password", "TODO");
        config.addDefault("database.database", "TODO");
        config.addDefault("checkPetOwner", false);
        saveConfig();
    }

    private void setupConfigFiles() {
        configManager = new ConfigManager(this, Set.of("commands", "menus", "messages", "sounds", "xp"));
    }


    public static PetsPlugin getInstance() {
        return instance;
    }

    public PlayerData getData() {
        return data;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public PetManager getPetManager() {
        return petManager;
    }

    public ExperienceManager getExperienceManager() {
        return experienceManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public CommandConfirmationManager getCommandConfirmationManager() {
        return commandConfirmationManager;
    }

    public boolean isRewardsInventoryLoaded() {
        return isRewardsInventoryLoaded;
    }

}

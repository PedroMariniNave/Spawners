package com.zpedroo.voltzspawners;

import com.zpedroo.voltzspawners.commands.SpawnersCmd;
import com.zpedroo.voltzspawners.hooks.ProtocolLibHook;
import com.zpedroo.voltzspawners.hooks.VaultHook;
import com.zpedroo.voltzspawners.hooks.WorldGuardHook;
import com.zpedroo.voltzspawners.listeners.EntityListeners;
import com.zpedroo.voltzspawners.listeners.SpawnerListeners;
import com.zpedroo.voltzspawners.listeners.PlayerChatListener;
import com.zpedroo.voltzspawners.listeners.PlayerGeneralListeners;
import com.zpedroo.voltzspawners.managers.SpawnerManager;
import com.zpedroo.voltzspawners.mysql.DBConnection;
import com.zpedroo.voltzspawners.tasks.SpawnerTask;
import com.zpedroo.voltzspawners.tasks.QuotationTask;
import com.zpedroo.voltzspawners.tasks.SaveTask;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import com.zpedroo.voltzspawners.utils.item.Items;
import com.zpedroo.voltzspawners.utils.menu.Menus;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class VoltzSpawners extends JavaPlugin {

    private static VoltzSpawners instance;
    public static VoltzSpawners get() { return instance; }

    public void onEnable() {
        instance = this;
        new FileUtils(this);

        if (!isMySQLEnabled(getConfig())) {
            getLogger().log(Level.SEVERE, "MySQL are disabled! You need to enable it.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        new DBConnection(getConfig());
        new VaultHook().hook();
        new WorldGuardHook();
        new ProtocolLibHook();
        new SpawnerTask(this);
        new SaveTask(this);
        new QuotationTask(this);
        new Menus();
        new Items();
        new NumberFormatter(getConfig());

        registerCommands();
        registerListeners();
    }

    public void onDisable() {
        if (!isMySQLEnabled(getConfig())) return;

        try {
            SpawnerManager.getInstance().saveAll();
            DBConnection.getInstance().closeConnection();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "An error ocurred while trying to save data!");
            ex.printStackTrace();
        }

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!entity.hasMetadata("MobAmount")) continue;

                entity.remove();
            }

            world.save();
        }
    }

    private void registerCommands() {
        getCommand("spawners").setExecutor(new SpawnersCmd());
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new EntityListeners(), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerGeneralListeners(), this);
        getServer().getPluginManager().registerEvents(new SpawnerListeners(), this);
    }

    private Boolean isMySQLEnabled(FileConfiguration file) {
        if (!file.contains("MySQL.enabled")) return false;

        return file.getBoolean("MySQL.enabled");
    }
}
package com.zpedroo.voltzspawners.managers;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.managers.cache.DataCache;
import com.zpedroo.voltzspawners.mysql.DBConnection;
import com.zpedroo.voltzspawners.objects.Bonus;
import com.zpedroo.voltzspawners.objects.Manager;
import com.zpedroo.voltzspawners.objects.PlacedSpawner;
import com.zpedroo.voltzspawners.objects.Spawner;
import com.zpedroo.voltzspawners.utils.FileUtils;
import com.zpedroo.voltzspawners.utils.builder.ItemBuilder;
import com.zpedroo.voltzspawners.enums.Permission;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class DataManager {

    private static DataManager instance;
    public static DataManager getInstance() { return instance; }

    private DataCache dataCache;

    public DataManager() {
        instance = this;
        this.dataCache = new DataCache();
        this.loadConfigSpawners();
        this.loadConfigBonuses();
        VoltzSpawners.get().getServer().getScheduler().runTaskLaterAsynchronously(VoltzSpawners.get(), this::loadPlacedSpawners, 20L);
        VoltzSpawners.get().getServer().getScheduler().runTaskLaterAsynchronously(VoltzSpawners.get(), this::loadTopSpawners, 20L);
    }

    private void loadConfigSpawners() {
        File folder = new File(VoltzSpawners.get().getDataFolder(), "/spawners");
        File[] files = folder.listFiles((file, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File fl : files) {
            if (fl == null) continue;

            FileConfiguration file = YamlConfiguration.loadConfiguration(fl);

            EntityType entity = EntityType.valueOf(file.getString("Spawner-Settings.entity"));
            String entityName = ChatColor.translateAlternateColorCodes('&', file.getString("Spawner-Settings.entity-name"));
            ItemStack item = ItemBuilder.build(file, "Spawner-Settings.item").build();
            Material block = Material.valueOf(file.getString("Spawner-Settings.spawner-block"));
            String type = fl.getName().replace(".yml", "");
            String typeTranslated = file.getString("Spawner-Settings.type-translated");
            String displayName = ChatColor.translateAlternateColorCodes('&', file.getString("Spawner-Settings.display-name"));
            int delay = file.getInt("Spawner-Settings.drops.delay");
            BigInteger amount = NumberFormatter.getInstance().filter(file.getString("Spawner-Settings.drops.amount"));
            BigInteger dropsValue = NumberFormatter.getInstance().filter(file.getString("Spawner-Settings.drops.price"));
            BigInteger dropsPreviousValue = NumberFormatter.getInstance().filter(file.getString("Spawner-Settings.drops.previous"));
            BigInteger dropsMinimumValue = NumberFormatter.getInstance().filter(file.getString("Spawner-Settings.drops.min"));
            BigInteger dropsMaximumValue = NumberFormatter.getInstance().filter(file.getString("Spawner-Settings.drops.max"));
            BigInteger maxStack = NumberFormatter.getInstance().filter(file.getString("Spawner-Settings.max-stack"));
            String permission = file.getString("Spawner-Settings.permission", null);
            List<String> commands = file.getStringList("Spawner-Settings.commands");

            Spawner spawner = new Spawner(entity, entityName, item, block, type, typeTranslated, displayName, delay, amount, dropsValue, dropsPreviousValue, dropsMinimumValue, dropsMaximumValue, maxStack, permission, commands);

            if (dropsValue.signum() <= 0) {
                SpawnerManager.updatePrice(spawner);
            }

            cache(spawner);
        }
    }

    private void loadConfigBonuses() {
        FileUtils.Files file = FileUtils.Files.CONFIG;
        for (String str : FileUtils.get().getSection(file, "Bonus")) {
            String permission = FileUtils.get().getString(file, "Bonus." + str + ".permission");
            double bonusPercentage = FileUtils.get().getDouble(file, "Bonus." + str + ".bonus");

            cache(new Bonus(permission, bonusPercentage));
        }
    }

    private void loadPlacedSpawners() {
        dataCache.setPlacedSpawners(DBConnection.getInstance().getDBManager().getPlacedSpawners());
    }

    private void loadTopSpawners() {
        dataCache.setTopSpawners(DBConnection.getInstance().getDBManager().getCache().getTopSpawners());
    }

    public void updateTopSpawners() {
        dataCache.setTopSpawners(getTopSpawnersOrdered());
    }

    public void saveAll() {
        new HashSet<>(dataCache.getDeletedSpawners()).forEach(spawnerLocation -> {
            DBConnection.getInstance().getDBManager().deleteSpawner(spawnerLocation);
        });

        dataCache.getDeletedSpawners().clear();

        new HashSet<>(dataCache.getPlacedSpawners().values()).forEach(spawner -> {
            if (spawner == null) return;
            if (!spawner.isQueueUpdate()) return;

            DBConnection.getInstance().getDBManager().saveSpawner(spawner);
            spawner.setUpdate(false);
        });
    }

    private void cache(Spawner spawner) {
        dataCache.getSpawners().put(spawner.getType().toUpperCase(), spawner);
    }

    private void cache(Bonus bonus) {
        dataCache.getBonuses().add(bonus);
    }

    private Map<UUID, BigInteger> getTopSpawnersOrdered() {
        Map<UUID, BigInteger> playerSpawners = new HashMap<>(dataCache.getPlacedSpawnersByUUID().size());

        new HashSet<>(dataCache.getPlacedSpawnersByUUID().values()).forEach(spawners -> {
            new HashSet<>(spawners).forEach(spawner -> {
                playerSpawners.put(spawner.getOwnerUUID(), spawner.getStack().add(playerSpawners.getOrDefault(spawner.getOwnerUUID(), BigInteger.ZERO)));
            });
        });

        return orderMapByValue(playerSpawners, 10);
    }

    private Map<UUID, BigInteger> orderMapByValue(Map<UUID, BigInteger> map, Integer limit) {
        return map.entrySet().stream().sorted((value1, value2) -> value2.getValue().compareTo(value1.getValue())).limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (value1, value2) -> value1, LinkedHashMap::new));
    }

    public String serializeManagers(List<Manager> managers) {
        if (managers == null || managers.isEmpty()) return "";

        StringBuilder serialized = new StringBuilder(32);

        for (Manager manager : managers) {
            serialized.append(manager.getUUID().toString()).append("#");

            for (Permission permission : manager.getPermissions()) {
                serialized.append(permission.toString()).append("#");
            }

            serialized.append(",");
        }

        return serialized.toString();
    }

    public List<Manager> deserializeManagers(String managers) {
        if (managers == null || managers.isEmpty()) return new ArrayList<>(5);

        List<Manager> ret = new ArrayList<>(64);
        String[] split = managers.split(",");

        for (String str : split) {
            if (str == null) break;

            String[] managersSplit = str.split("#");

            List<Permission> permissions = new ArrayList<>(5);
            if (managersSplit.length > 1) {
                for (int i = 1; i < managersSplit.length; ++i) {
                    permissions.add(Permission.valueOf(managersSplit[i]));
                }
            }

            ret.add(new Manager(UUID.fromString(managersSplit[0]), permissions));
        }

        return ret;
    }

    public DataCache getCache() {
        return dataCache;
    }

    public PlacedSpawner getPlacedSpawner(Location location) {
        if (dataCache.getPlacedSpawners() == null) return null;

        return dataCache.getPlacedSpawners().get(location);
    }

    public Spawner getSpawner(String type) {
        return dataCache.getSpawners().get(type.toUpperCase());
    }
}
package com.zpedroo.voltzspawners.managers;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.managers.cache.DataCache;
import com.zpedroo.voltzspawners.mysql.DBConnection;
import com.zpedroo.voltzspawners.objects.Manager;
import com.zpedroo.voltzspawners.objects.PlayerSpawner;
import com.zpedroo.voltzspawners.objects.Spawner;
import com.zpedroo.voltzspawners.utils.builder.ItemBuilder;
import com.zpedroo.voltzspawners.utils.enums.Permission;
import org.bukkit.Bukkit;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.zpedroo.voltzspawners.utils.config.Settings.MAX_PRICE;
import static com.zpedroo.voltzspawners.utils.config.Settings.MIN_PRICE;

public class DataManager {

    private static DataManager instance;
    public static DataManager getInstance() { return instance; }

    private DataCache dataCache;

    public DataManager() {
        instance = this;
        this.dataCache = new DataCache();
        this.loadConfigSpawners();
        VoltzSpawners.get().getServer().getScheduler().runTaskLaterAsynchronously(VoltzSpawners.get(), this::loadPlacedSpawners, 20L);
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
            Integer delay = file.getInt("Spawner-Settings.drops.delay");
            BigInteger amount = new BigInteger(file.getString("Spawner-Settings.drops.amount"));
            BigInteger dropsValue = new BigInteger(file.getString("Spawner-Settings.drops.price"));
            BigInteger dropsPreviousValue = new BigInteger(file.getString("Spawner-Settings.drops.previous"));
            BigInteger maxStack = new BigInteger(file.getString("Spawner-Settings.max-stack"));
            String permission = file.getString("Spawner-Settings.place-permission", "NULL");
            List<String> commands = file.getStringList("Spawner-Settings.commands");

            if (dropsValue.signum() <= 0) {
                dropsValue = new BigInteger(String.format("%.0f", ThreadLocalRandom.current().nextDouble(MIN_PRICE.doubleValue(), MAX_PRICE.doubleValue())));
                try {
                    file.set("Spawner-Settings.drops.price", dropsValue.longValue());
                    file.save(fl);
                } catch (Exception ex) {
                    // ignore
                }
            }

            cache(new Spawner(entity, entityName, item, block, type, typeTranslated, displayName, delay, amount, dropsValue, dropsPreviousValue, maxStack, permission, commands));
        }
    }

    private void loadPlacedSpawners() {
        dataCache.setPlayerSpawners(DBConnection.getInstance().getDBManager().getPlacedSpawners());
    }

    public void updateTopSpawners() {
        dataCache.setTopSpawners(getTopSpawnersOrdered());
    }

    public void saveAll() {
        new HashSet<>(dataCache.getDeletedSpawners()).forEach(spawner -> {
            DBConnection.getInstance().getDBManager().deleteSpawner(serializeLocation(spawner));
        });

        dataCache.getDeletedSpawners().clear();

        new HashSet<>(dataCache.getPlayerSpawners().values()).forEach(spawner -> {
            if (spawner == null) return;

            spawner.removeEntities();
            if (!spawner.isQueueUpdate()) return;

            DBConnection.getInstance().getDBManager().saveSpawner(spawner);
            spawner.setQueueUpdate(false);
        });
    }

    private void cache(Spawner spawner) {
        dataCache.getSpawners().put(spawner.getType().toUpperCase(), spawner);
    }

    private Map<UUID, BigInteger> getTopSpawnersOrdered() {
        Map<UUID, BigInteger> playerSpawners = new HashMap<>(dataCache.getPlayerSpawnersByUUID().size());

        new HashSet<>(dataCache.getPlayerSpawnersByUUID().values()).forEach(spawners -> {
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

    public String serializeLocation(Location location) {
        if (location == null) return null;

        StringBuilder serialized = new StringBuilder(4);
        serialized.append(location.getWorld().getName());
        serialized.append("#" + location.getX());
        serialized.append("#" + location.getY());
        serialized.append("#" + location.getZ());

        return serialized.toString();
    }

    public Location deserializeLocation(String location) {
        if (location == null) return null;

        String[] locationSplit = location.split("#");
        double x = Double.parseDouble(locationSplit[1]);
        double y = Double.parseDouble(locationSplit[2]);
        double z = Double.parseDouble(locationSplit[3]);

        return new Location(Bukkit.getWorld(locationSplit[0]), x, y, z);
    }


    public DataCache getCache() {
        return dataCache;
    }

    public PlayerSpawner getSpawner(Location location) {
        return dataCache.getPlayerSpawners().get(location);
    }

    public Spawner getSpawner(String type) {
        return dataCache.getSpawners().get(type.toUpperCase());
    }
}
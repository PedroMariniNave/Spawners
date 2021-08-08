package com.zpedroo.voltzspawners.managers;

import com.zpedroo.voltzspawners.FileUtils;
import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.hooks.WorldGuardHook;
import com.zpedroo.voltzspawners.spawner.Spawner;
import com.zpedroo.voltzspawners.spawner.PlayerSpawner;
import com.zpedroo.voltzspawners.spawner.cache.SpawnerDataCache;
import com.zpedroo.voltzspawners.mysql.DBConnection;
import com.zpedroo.voltzspawners.objects.Manager;
import com.zpedroo.voltzspawners.utils.builder.ItemBuilder;
import com.zpedroo.voltzspawners.utils.enums.Permission;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zpedroo.voltzspawners.utils.config.Messages.*;
import static com.zpedroo.voltzspawners.utils.config.Settings.*;

public class SpawnerManager {

    private static SpawnerManager instance;
    public static SpawnerManager getInstance() { return instance; }

    private SpawnerDataCache spawnerDataCache;

    public SpawnerManager() {
        instance = this;
        this.spawnerDataCache = new SpawnerDataCache();
        this.loadConfigSpawners();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (!entity.getType().equals(EntityType.DROPPED_ITEM)) continue;

                        entity.remove();
                    }
                }

                loadPlacedSpawners();
                updateTopSpawners();
            }
        }.runTaskLaterAsynchronously(VoltzSpawners.get(), 100L);
    }

    private void loadConfigSpawners() {
        File folder = new File(VoltzSpawners.get().getDataFolder(), "/spawners");
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
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

    public void updatePrices(Boolean forced) {
        for (Spawner spawner : getDataCache().getSpawners().values()) {
            File folder = new File(VoltzSpawners.get().getDataFolder(), "/spawners");
            File[] files = folder.listFiles((d, name) -> name.equals(spawner.getType() + ".yml"));

            if (files == null) return;

            YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(files[0]);
            BigInteger newValue = new BigInteger(String.format("%.0f", ThreadLocalRandom.current().nextDouble(MIN_PRICE.doubleValue(), MAX_PRICE.doubleValue())));

            try {
                yamlConfig.set("Spawner-Settings.drops.price", newValue.longValue());
                yamlConfig.set("Spawner-Settings.drops.previous", spawner.getDropsValue().longValue());
                yamlConfig.save(files[0]);
            } catch (Exception ex) {
                // ignore
            }

            spawner.setDropsPreviousValue(spawner.getDropsValue());
            spawner.setDropsValue(newValue);
        }

        for (String msg : NEW_QUOTATION) {
            if (msg == null) break;

            Bukkit.broadcastMessage(msg);
        }

        if (forced) return;

        long nextUpdate = NEXT_UPDATE + TimeUnit.HOURS.toMillis(24);
        if (NEXT_UPDATE == 0) nextUpdate = getEndOfDay().getTime();

        FileUtils.get().getFile(FileUtils.Files.CONFIG).get().set("Settings.next-update", nextUpdate);
        FileUtils.get().getFile(FileUtils.Files.CONFIG).save();

        NEXT_UPDATE = nextUpdate;
    }

    private void loadPlacedSpawners() {
        getDataCache().setPlayerSpawners(DBConnection.getInstance().getDBManager().getPlacedSpawners());
    }

    public void updateTopSpawners() {
        getDataCache().setTopSpawners(getTopSpawners());
    }

    public void saveAll() {
        new HashSet<>(getDataCache().getDeletedSpawners()).forEach(spawner -> {
            DBConnection.getInstance().getDBManager().deleteSpawner(serializeLocation(spawner));
        });

        getDataCache().getDeletedSpawners().clear();

        new HashSet<>(getDataCache().getPlayerSpawners().values()).forEach(spawner -> {
            if (spawner == null) return;

            spawner.removeEntities();
            if (!spawner.isQueueUpdate()) return;

            DBConnection.getInstance().getDBManager().saveSpawner(spawner);
            spawner.setQueueUpdate(false);
        });
    }

    private void cache(Spawner spawner) {
        getDataCache().getSpawners().put(spawner.getType().toUpperCase(), spawner);
    }

    public Map<UUID, BigInteger> getTopSpawners() {
        Map<UUID, BigInteger> playerSpawners = new HashMap<>(getDataCache().getPlayerSpawnersByUUID().size());

        new HashSet<>(getDataCache().getPlayerSpawnersByUUID().values()).forEach(spawners -> {
            new HashSet<>(spawners).forEach(spawner -> {
                playerSpawners.put(spawner.getOwnerUUID(), spawner.getStack().add(playerSpawners.getOrDefault(spawner.getOwnerUUID(), BigInteger.ZERO)));
            });
        });

        return orderTop(playerSpawners, 10);
    }

    public Object[] getNearMachines(Player player, Block block, BigInteger addAmount, String type) {
        int radius = STACK_RADIUS;
        if (radius <= 0) return null;

        int initialX = block.getX(), initialY = block.getY(), initialZ = block.getZ();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (initialX == x && initialY == y && initialZ == z) continue;
                    if (!WorldGuardHook.getInstance().canBuild(player, new Location(block.getWorld(), x, y, z))) continue;

                    Block blocks = block.getRelative(x, y, z);
                    if (blocks.getType().equals(Material.AIR)) continue;

                    PlayerSpawner spawner = getSpawner(blocks.getLocation());
                    if (spawner == null) continue;
                    if (!StringUtils.equals(type, spawner.getSpawner().getType())) continue;
                    if (spawner.hasReachStackLimit()) continue;

                    BigInteger overLimit = BigInteger.ZERO;
                    if (spawner.getSpawner().getMaxStack().signum() > 0 && spawner.getStack().add(addAmount).compareTo(spawner.getSpawner().getMaxStack()) > 0) {
                        overLimit = spawner.getStack().add(addAmount).subtract(spawner.getSpawner().getMaxStack());
                    }

                    return new Object[] { spawner, overLimit };
                }
            }
        }

        return null;
    }

    public SpawnerDataCache getDataCache() {
        return spawnerDataCache;
    }

    public PlayerSpawner getSpawner(Location location) {
        if (!getDataCache().getPlayerSpawners().containsKey(location)) return null;

        return getDataCache().getPlayerSpawners().get(location);
    }

    public Spawner getSpawner(String type) {
        return getDataCache().getSpawners().get(type.toUpperCase());
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

    private Date getEndOfDay() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 23, 59, 59);
        return calendar.getTime();
    }

    private Map<UUID, BigInteger> orderTop(Map<UUID, BigInteger> map, Integer limit) {
        List<Map.Entry<UUID, BigInteger> > list = new LinkedList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<UUID, BigInteger> ret = new HashMap<>(limit);
        for (Map.Entry<UUID, BigInteger> entry : list) {
            if (ret.size() >= limit) break;

            ret.put(entry.getKey(), entry.getValue());
        }
        return ret;
    }
}

package com.zpedroo.voltzspawners.managers;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.hooks.WorldGuardHook;
import com.zpedroo.voltzspawners.objects.PlayerSpawner;
import com.zpedroo.voltzspawners.objects.Spawner;
import com.zpedroo.voltzspawners.utils.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zpedroo.voltzspawners.utils.config.Messages.*;
import static com.zpedroo.voltzspawners.utils.config.Settings.*;

public class SpawnerManager {

    private static SpawnerManager instance;
    public static SpawnerManager getInstance() { return instance; }

    public SpawnerManager() {
        instance = this;
    }

    public void clearAll() {
        new HashSet<>(DataManager.getInstance().getCache().getPlayerSpawners().values()).forEach(spawner -> {
            spawner.removeEntities();
            spawner.getHologram().remove();
        });
    }

    public void updatePrices(Boolean forced) {
        for (Spawner spawner : DataManager.getInstance().getCache().getSpawners().values()) {
            File folder = new File(VoltzSpawners.get().getDataFolder(), "/spawners");
            File[] files = folder.listFiles((file, name) -> name.equals(spawner.getType() + ".yml"));

            if (files == null || files.length <= 0) return;

            File file = files[0];
            FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
            BigInteger newValue = new BigInteger(String.format("%.0f", ThreadLocalRandom.current().nextDouble(MIN_PRICE.doubleValue(), MAX_PRICE.doubleValue())));

            try {
                fileConfig.set("Spawner-Settings.drops.price", newValue.longValue());
                fileConfig.set("Spawner-Settings.drops.previous", spawner.getDropsValue().longValue());
                fileConfig.save(file);
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

    public Object[] getNearSpawners(Player player, Block block, BigInteger addAmount, String type) {
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

                    PlayerSpawner spawner = DataManager.getInstance().getSpawner(blocks.getLocation());
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

    private Date getEndOfDay() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 23, 59, 59);
        return calendar.getTime();
    }
}

package com.zpedroo.voltzspawners.managers;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.objects.PlacedSpawner;
import com.zpedroo.voltzspawners.objects.Spawner;
import com.zpedroo.voltzspawners.utils.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zpedroo.voltzspawners.utils.config.Messages.*;
import static com.zpedroo.voltzspawners.utils.config.Settings.*;

public class SpawnerManager {

    public static void clearAll() {
        DataManager.getInstance().getCache().getPlacedSpawners().values().stream().filter(placedSpawner ->
                placedSpawner.getHologram() != null).forEach(spawner -> {
                    spawner.getHologram().removeHologramAndItem();
                    spawner.removeEntities();
                });
    }

    public static void updatePrices(boolean forced) {
        for (Spawner spawner : DataManager.getInstance().getCache().getSpawners().values()) {
            updatePrice(spawner);
        }

        for (String msg : NEW_QUOTATION) {
            Bukkit.broadcastMessage(msg);
        }

        if (forced) return;

        long nextUpdate = NEXT_UPDATE + TimeUnit.HOURS.toMillis(24);
        if (NEXT_UPDATE == 0) nextUpdate = getEndOfDay().getTime();

        FileUtils.get().getFile(FileUtils.Files.CONFIG).get().set("Settings.next-update", nextUpdate);
        FileUtils.get().getFile(FileUtils.Files.CONFIG).save();

        NEXT_UPDATE = nextUpdate;
    }

    public static void updatePrice(Spawner spawner) {
        File folder = new File(VoltzSpawners.get().getDataFolder(), "/spawners");
        File[] files = folder.listFiles((file, name) -> name.equals(spawner.getType() + ".yml"));

        if (files == null || files.length <= 0) return;

        File file = files[0];
        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);

        double minPrice = spawner.getDropsMinimumValue().doubleValue();
        double maxPrice = spawner.getDropsMaximumValue().doubleValue();

        BigInteger newPrice = new BigInteger(String.format("%.0f", ThreadLocalRandom.current().nextDouble(minPrice, maxPrice)));

        try {
            fileConfig.set("Spawner-Settings.drops.price", newPrice.toString());
            fileConfig.set("Spawner-Settings.drops.previous", spawner.getDropsValue().toString());
            fileConfig.save(file);
        } catch (Exception ex) {
            // ignore
        }

        spawner.setDropsPreviousValue(spawner.getDropsValue());
        spawner.setDropsValue(newPrice);
    }

    public static Object[] getNearSpawners(Player player, Block block, BigInteger addAmount, String type) {
        int radius = STACK_RADIUS;
        if (radius <= 0) return null;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block blocks = block.getRelative(x, y, z);
                    if (blocks.getType().equals(Material.AIR)) continue;

                    PlacedSpawner spawner = DataManager.getInstance().getPlacedSpawner(blocks.getLocation());
                    if (spawner == null) continue;
                    if (!StringUtils.equals(type, spawner.getSpawner().getType())) continue;
                    if (spawner.hasReachStackLimit()) continue;
                    if (!spawner.canInteract(player)) continue;

                    BigInteger overLimit = BigInteger.ZERO;
                    if (spawner.getSpawner().getMaximumStack().signum() > 0 && spawner.getStack().add(addAmount).compareTo(spawner.getSpawner().getMaximumStack()) > 0) {
                        overLimit = spawner.getStack().add(addAmount).subtract(spawner.getSpawner().getMaximumStack());
                    }

                    return new Object[] { spawner, overLimit };
                }
            }
        }

        return null;
    }

    private static Date getEndOfDay() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 23, 59, 59);
        return calendar.getTime();
    }
}

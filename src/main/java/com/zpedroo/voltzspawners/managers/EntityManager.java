package com.zpedroo.voltzspawners.managers;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.spawner.PlayerSpawner;
import com.zpedroo.voltzspawners.tasks.EnderDragonTask;
import com.zpedroo.voltzspawners.utils.config.Settings;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;

import java.math.BigInteger;
import java.util.Random;

public class EntityManager {

    public static void spawn(PlayerSpawner spawner, BigInteger amount) {
        int radius = Settings.STACK_RADIUS * 2;

        for (Entity near : spawner.getLocation().getWorld().getNearbyEntities(spawner.getLocation(), radius, radius, radius)) {
            if (near == null || !near.getType().equals(spawner.getSpawner().getEntity())) continue;
            if (!near.hasMetadata("Spawner")) continue;

            final BigInteger stack = new BigInteger(near.getMetadata("MobAmount").get(0).asString());
            BigInteger newStack = stack.add(amount);
            String serialized = SpawnerManager.getInstance().serializeLocation(spawner.getLocation());

            near.setMetadata("MobAmount", new FixedMetadataValue(VoltzSpawners.get(), newStack.toString()));
            near.setMetadata("Spawner", new FixedMetadataValue(VoltzSpawners.get(), serialized));

            near.setCustomName(StringUtils.replaceEach(spawner.getSpawner().getEntityName(), new String[]{
                    "{stack}"
            }, new String[]{
                    NumberFormatter.getInstance().format(newStack)
            }));
            spawner.addEntity(near);
            return;
        }

        int tryLimit = 20;

        int minRange = 1;
        int maxRange = radius / 2;

        Random random = new Random();
        double x = spawner.getLocation().getX() + random.nextDouble() * (maxRange - minRange) + 0.5D;
        double y = spawner.getLocation().getY() + 5D; // fix spawn bugs
        double z = spawner.getLocation().getZ() + random.nextDouble() * (maxRange - minRange) + 0.5D;

        Location location = null;

        if (spawner.getSpawner().getEntity() != EntityType.ENDER_DRAGON) {
            location = new Location(spawner.getLocation().getWorld(), x, y, z, 180f, 0f);
        } else {
            location = new Location(spawner.getLocation().getWorld(), x, y, z);
        }

        while (!canSpawn(location)) {
            if (--tryLimit <= 0) return;

            location.setY(location.getY() - 1);
        }

        LivingEntity entity = (LivingEntity) spawner.getLocation().getWorld().spawnEntity(location, spawner.getSpawner().getEntity());
        entity.setMetadata("MobAmount", new FixedMetadataValue(VoltzSpawners.get(), amount.toString()));
        entity.setMetadata("Spawner", new FixedMetadataValue(VoltzSpawners.get(), SpawnerManager.getInstance().serializeLocation(spawner.getLocation())));
        entity.setCustomName(StringUtils.replaceEach(spawner.getSpawner().getEntityName(), new String[]{
                "{stack}"
        }, new String[]{
                NumberFormatter.getInstance().format(amount)
        }));

        spawner.addEntity(entity);

        entity.setSilent(true);
        entity.setRemoveWhenFarAway(false);

        if (entity instanceof EnderDragon) {
            entity.damage(1);
            new EnderDragonTask(VoltzSpawners.get(), entity);
            return;
        }

        entity.setAI(false);
    }

    public static void removeStack(Entity entity, BigInteger amount, PlayerSpawner playerSpawner) {
        String spawner = entity.getMetadata("Spawner").get(0).asString();
        BigInteger stack = new BigInteger(entity.getMetadata("MobAmount").get(0).asString());
        BigInteger newStack = stack.subtract(amount);
        if (newStack.signum() <= 0) {
            entity.remove();
            return;
        }

        entity.setMetadata("MobAmount", new FixedMetadataValue(VoltzSpawners.get(), newStack.toString()));
        entity.setMetadata("Spawner", new FixedMetadataValue(VoltzSpawners.get(), spawner));
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', StringUtils.replaceEach(playerSpawner.getSpawner().getEntityName(), new String[]{
                "{stack}"
        }, new String[]{
                NumberFormatter.getInstance().format(newStack)
        })));
    }

    private static Boolean canSpawn(Location location) {
        Block block = location.getBlock().getRelative(BlockFace.DOWN);

        return !block.getType().equals(Material.AIR) && !block.getType().toString().contains("SLAB");
    }
}
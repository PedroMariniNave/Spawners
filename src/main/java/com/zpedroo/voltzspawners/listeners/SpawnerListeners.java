package com.zpedroo.voltzspawners.listeners;

import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.managers.SpawnerManager;
import com.zpedroo.voltzspawners.objects.PlacedSpawner;
import com.zpedroo.voltzspawners.objects.Spawner;
import com.zpedroo.voltzspawners.utils.config.Messages;
import com.zpedroo.voltzspawners.utils.config.Settings;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import com.zpedroo.voltzspawners.utils.item.Items;
import com.zpedroo.voltzspawners.utils.menu.Menus;
import de.tr7zw.nbtapi.NBTItem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnerListeners implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getItemInHand() == null || event.getItemInHand().getType().equals(Material.AIR)) return;

        ItemStack item = event.getItemInHand().clone();
        NBTItem nbt = new NBTItem(item);
        if (nbt.hasKey("SpawnersEnergy") || nbt.hasKey("SpawnersInfiniteEnergy") || nbt.hasKey("SpawnersInfiniteRepair") || nbt.hasKey("SpawnersRepair")) event.setCancelled(true);
        if (!nbt.hasKey("SpawnersAmount")) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (!player.hasPermission("spawner.admin")) {
            Location location = new Location(
                    block.getWorld().getName(),
                    block.getX(),
                    block.getY(),
                    block.getZ()
            );
            Plot plot = Plot.getPlot(location);
            if (plot == null) return;
            if (!plot.isAdded(player.getUniqueId())) return;
        }

        Spawner spawner = DataManager.getInstance().getSpawner(nbt.getString("SpawnersType").toUpperCase());
        if (spawner == null) return;
        if (spawner.getPermission() != null && !player.hasPermission(spawner.getPermission())) {
            event.setCancelled(true);
            player.sendMessage(Messages.SPAWNER_PERMISSION);
            return;
        }

        BigInteger stack = new BigInteger(nbt.getString("SpawnersAmount"));
        BigInteger integrity = new BigInteger(nbt.getString("SpawnersIntegrity"));
        Object[] objects = SpawnerManager.getNearSpawners(player, block, stack, spawner.getType());
        PlacedSpawner placedSpawner = objects != null ? (PlacedSpawner) objects[0] : null;
        BigInteger overLimit = null;

        if (placedSpawner != null) {
            event.setCancelled(true);

            overLimit = (BigInteger) objects[1];
            placedSpawner.addStack(stack.subtract(overLimit));
        } else {
            int radius = Settings.STACK_RADIUS;

            if (radius > 0) {
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Block blocks = block.getRelative(x, y, z);
                            if (blocks.getType().equals(Material.AIR)) continue;

                            placedSpawner = DataManager.getInstance().getPlacedSpawner(blocks.getLocation());
                            if (placedSpawner == null) continue;

                            event.setCancelled(true);
                            player.sendMessage(Messages.NEAR_SPAWNER);
                            return;
                        }
                    }
                }
            }

            List<PlacedSpawner> spawnersWithSameType = DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId())
                    .stream().filter(playerSpawner -> playerSpawner.getSpawner().getType().equals(spawner.getType())).collect(Collectors.toList());

            if (spawnersWithSameType.size() >= 1) {
                event.setCancelled(true);
                player.sendMessage(Messages.ONLY_ONE_SPAWNER);
                return;
            }

            if (spawner.getMaximumStack().signum() > 0 && stack.compareTo(spawner.getMaximumStack()) > 0) {
                overLimit = stack.subtract(spawner.getMaximumStack());
            } else overLimit = BigInteger.ZERO;

            block.setType(spawner.getBlock());
            block.getState().update(true);

            placedSpawner = new PlacedSpawner(block.getLocation(), player.getUniqueId(), stack.subtract(overLimit), BigInteger.ZERO, BigInteger.ZERO, integrity, spawner, new ArrayList<>(), false, false, false);
            placedSpawner.cache();
            placedSpawner.setUpdate(true);
        }

        item.setAmount(1);
        player.getInventory().removeItem(item);

        if (overLimit.compareTo(BigInteger.ZERO) >= 1) player.getInventory().addItem(spawner.getItem(overLimit, integrity));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (event.getItem() == null || event.getItem().getType().equals(Material.AIR)) return;

        PlacedSpawner spawner = null;
        if (event.getClickedBlock() != null) spawner = DataManager.getInstance().getPlacedSpawner(event.getClickedBlock().getLocation());
        if (spawner != null) return;

        ItemStack item = event.getItem().clone();
        NBTItem nbt = new NBTItem(item);

        Player player = event.getPlayer();
        List<PlacedSpawner> spawners = DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId());

        if (nbt.hasKey("SpawnersEnergy")) {

            event.setCancelled(true);

            if (spawners.isEmpty()) {
                player.sendMessage(Messages.ZERO_SPAWNERS_ENERGY);
                return;
            }

            Integer spawnersWithInfiniteEnergy = 0;
            for (PlacedSpawner placedSpawner : spawners) {
                if (placedSpawner.hasInfiniteEnergy()) ++spawnersWithInfiniteEnergy;
            }

            Integer spawnersAmount = spawners.size();
            BigInteger amount = new BigInteger(nbt.getString("SpawnersEnergy"));
            if (player.isSneaking()) {
                amount = amount.multiply(BigInteger.valueOf(item.getAmount()));
            } else {
                item.setAmount(1);
            }

            BigInteger toAdd = amount.divide(BigInteger.valueOf(spawnersAmount - spawnersWithInfiniteEnergy));
            while (toAdd.compareTo(BigInteger.ONE) < 0) {
                toAdd = amount.divide(BigInteger.valueOf(--spawnersAmount));
            }

            for (int i = 0; i < spawnersAmount; ++i) {
                PlacedSpawner placedSpawner = spawners.get(i);
                if (!placedSpawner.hasInfiniteEnergy()) {
                    placedSpawner.addEnergy(toAdd);
                }
            }

            player.getInventory().removeItem(item);
            player.playSound(player.getLocation(), Sound.LAVA_POP, 0.5f, 10f);
            player.sendMessage(StringUtils.replaceEach(Messages.SUCCESSFUL_ENERGIZED, new String[]{
                    "{spawners}",
                    "{energy}"
            }, new String[]{
                    NumberFormatter.getInstance().formatDecimal(spawnersAmount.doubleValue() - spawnersWithInfiniteEnergy.doubleValue()),
                    NumberFormatter.getInstance().format(toAdd)
            }));
            return;
        }

        if (nbt.hasKey("SpawnersRepair")) {

            event.setCancelled(true);

            if (spawners.isEmpty()) {
                player.sendMessage(Messages.ZERO_SPAWNERS_REPAIR);
                return;
            }

            Integer spawnersAmount = spawners.size();
            BigInteger percentage = new BigInteger(nbt.getString("SpawnersRepair"));
            if (player.isSneaking()) {
                percentage = percentage.multiply(BigInteger.valueOf(item.getAmount()));
            } else {
                item.setAmount(1);
            }

            BigInteger toAdd = percentage.divide(BigInteger.valueOf(spawnersAmount));
            BigInteger amountRepaired = BigInteger.ZERO;
            while (toAdd.compareTo(BigInteger.ONE) < 0) {
                toAdd = percentage.divide(BigInteger.valueOf(--spawnersAmount));
            }

            BigInteger overLimit = BigInteger.ZERO;

            for (int i = 0; i < spawnersAmount; ++i) {
                PlacedSpawner placedSpawner = spawners.get(i);
                BigInteger excess = BigInteger.ZERO;
                if (placedSpawner.getIntegrity().compareTo(BigInteger.valueOf(100)) >= 0 || placedSpawner.hasInfiniteIntegrity() || placedSpawner.getIntegrity().add(toAdd).compareTo(BigInteger.valueOf(100)) > 0) {
                    excess = toAdd.subtract(BigInteger.valueOf(100 - placedSpawner.getIntegrity().intValue()));
                    overLimit = overLimit.add(excess);
                }

                if (toAdd.subtract(excess).signum() <= 0) continue;

                BigInteger toRepair = toAdd.subtract(excess);
                amountRepaired = amountRepaired.add(toRepair);

                placedSpawner.addIntegrity(toRepair);
            }

            if (overLimit.divide(toAdd).compareTo(BigInteger.valueOf(spawnersAmount)) == 0) {
                player.sendMessage(Messages.ZERO_SPAWNERS_REPAIR);
                return;
            }

            player.getInventory().removeItem(item);
            player.playSound(player.getLocation(), Sound.ANVIL_USE, 0.5f, 0.5f);
            player.sendMessage(StringUtils.replaceEach(Messages.SUCCESSFUL_REPAIRED, new String[]{
                    "{spawners}",
                    "{repair}"
            }, new String[]{
                    NumberFormatter.getInstance().formatDecimal(spawnersAmount.doubleValue()),
                    NumberFormatter.getInstance().format(amountRepaired.divide(BigInteger.valueOf(spawnersAmount)))
            }));
            if (overLimit.signum() > 0) player.getInventory().addItem(Items.getInstance().getRepair(overLimit));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawnerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType().equals(Material.AIR)) return;

        PlacedSpawner spawner = DataManager.getInstance().getPlacedSpawner(block.getLocation());
        if (spawner == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        if (!spawner.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        if (event.getItem() != null && event.getItem().getType() != Material.AIR) {
            ItemStack item = event.getItem().clone();
            NBTItem nbt = new NBTItem(item);

            if (nbt.hasKey("SpawnersInfiniteEnergy")) {
                if (spawner.hasInfiniteEnergy()) return;

                spawner.setInfiniteEnergy(true);
                item.setAmount(1);

                player.getInventory().removeItem(item);
                player.playSound(player.getLocation(), Sound.LAVA_POP, 0.5f, 10f);
                return;
            }

            if (nbt.hasKey("SpawnersInfiniteRepair")) {
                if (spawner.hasInfiniteIntegrity()) return;

                spawner.setInfiniteIntegrity(true);
                item.setAmount(1);

                player.getInventory().removeItem(item);
                player.playSound(player.getLocation(), Sound.ANVIL_USE, 0.5f, 0.5f);
                return;
            }

            if (nbt.hasKey("SpawnersEnergy")) {
                if (spawner.hasInfiniteEnergy()) return;

                BigInteger amount = new BigInteger(nbt.getString("SpawnersEnergy"));

                spawner.addEnergy(amount);
                item.setAmount(1);

                player.getInventory().removeItem(item);
                player.playSound(player.getLocation(), Sound.LAVA_POP, 0.5f, 10f);
                return;
            }

            if (nbt.hasKey("SpawnersRepair")) {
                if (spawner.getIntegrity().compareTo(BigInteger.valueOf(100)) >= 0 || spawner.hasInfiniteIntegrity()) return;

                BigInteger percentage = new BigInteger(nbt.getString("SpawnersRepair"));
                BigInteger overLimit = BigInteger.ZERO;

                if (spawner.getIntegrity().add(percentage).compareTo(BigInteger.valueOf(100)) > 0) {
                    overLimit = percentage.subtract(BigInteger.valueOf(100 - spawner.getIntegrity().intValue()));
                }

                spawner.addIntegrity(percentage.subtract(overLimit));
                item.setAmount(1);

                player.getInventory().removeItem(item);
                player.playSound(player.getLocation(), Sound.ANVIL_USE, 0.5f, 0.5f);
                if (overLimit.signum() > 0) player.getInventory().addItem(Items.getInstance().getRepair(overLimit));
                return;
            }
        }

        Menus.getInstance().openSpawnerMenu(player, spawner);
        player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 0.5f, 2f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        PlacedSpawner spawner = DataManager.getInstance().getPlacedSpawner(block.getLocation());
        if (spawner == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!spawner.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        spawner.delete();

        ItemStack itemInHand = event.getPlayer().getItemInHand().clone();
        BigInteger toGive = spawner.getStack();

        boolean correctPickaxe = false;

        if (itemInHand.getType() != Material.AIR) {
            NBTItem nbt = new NBTItem(itemInHand);
            if (nbt.hasKey("SpawnersPickaxe")) correctPickaxe = true;
        }

        if (!correctPickaxe) {
            toGive = spawner.getStack().subtract(spawner.getStack().multiply(BigInteger.valueOf(Settings.TAX_REMOVE_STACK)).divide(BigInteger.valueOf(100)));
        }

        if (toGive.compareTo(spawner.getStack()) < 0) {
            player.sendMessage(StringUtils.replaceEach(Messages.INCORRECT_PICKAXE, new String[]{
                    "{lost}"
            }, new String[]{
                    NumberFormatter.getInstance().format(spawner.getStack().subtract(toGive))
            }));
        }

        if (spawner.hasInfiniteEnergy()) player.getInventory().addItem(Items.getInstance().getInfiniteEnergy());
        if (spawner.hasInfiniteIntegrity()) player.getInventory().addItem(Items.getInstance().getInfiniteRepair());
        if (spawner.getDrops().signum() > 0) spawner.sellDrops(player);

        player.getInventory().addItem(spawner.getSpawner().getItem(toGive, spawner.getIntegrity()));

        if (spawner.getEnergy().signum() <= 0) return;

        player.getInventory().addItem(Items.getInstance().getEnergy(spawner.getEnergy()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreakByInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType().equals(Material.AIR)) return;

        PlacedSpawner spawner = DataManager.getInstance().getPlacedSpawner(block.getLocation());
        if (spawner == null) return;

        Player player = event.getPlayer();
        if (player.getItemInHand() == null || player.getItemInHand().getType().equals(Material.AIR)) return;

        ItemStack item = player.getItemInHand();
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey("SpawnersPickaxe")) return;

        event.setCancelled(true);

        if (!spawner.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        spawner.delete();

        if (spawner.hasInfiniteEnergy()) player.getInventory().addItem(Items.getInstance().getInfiniteEnergy());
        if (spawner.hasInfiniteIntegrity()) player.getInventory().addItem(Items.getInstance().getInfiniteRepair());
        if (spawner.getDrops().signum() > 0) spawner.sellDrops(player);

        player.getInventory().addItem(spawner.getSpawner().getItem(spawner.getStack(), spawner.getIntegrity()));

        if (spawner.getEnergy().signum() <= 0) return;

        player.getInventory().addItem(Items.getInstance().getEnergy(spawner.getEnergy()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDespawn(ItemDespawnEvent event) {
        if (!event.getEntity().hasMetadata("***")) return;

        event.setCancelled(true);
    }
}
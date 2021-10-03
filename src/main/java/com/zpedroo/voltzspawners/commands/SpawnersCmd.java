package com.zpedroo.voltzspawners.commands;

import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.objects.Spawner;
import com.zpedroo.voltzspawners.managers.SpawnerManager;
import com.zpedroo.voltzspawners.utils.config.Messages;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import com.zpedroo.voltzspawners.utils.item.Items;
import com.zpedroo.voltzspawners.utils.menu.Menus;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;

public class SpawnersCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        Player target = null;
        BigInteger amount = null;

        if (args.length > 0) {
            switch (args[0].toUpperCase()) {
                case "TOP":
                    if (player == null) return true;

                    Menus.getInstance().openTopSpawnersMenu(player);
                    return true;
                case "GIVE":
                    if (!sender.hasPermission("spawners.admin")) break;

                    if (args.length < 4) {
                        sender.sendMessage(Messages.SPAWNER_USAGE);
                        return true;
                    }

                    Spawner spawner = DataManager.getInstance().getSpawner(args[2]);

                    if (spawner == null) {
                        sender.sendMessage(Messages.INVALID_SPAWNER);
                        return true;
                    }

                    amount = NumberFormatter.getInstance().filter(args[3]);
                    if (amount.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    target.getInventory().addItem(spawner.getItem(amount, 100));
                    return true;
                case "ENERGY":
                    if (!sender.hasPermission("spawners.admin")) break;

                    if (args.length < 3) {
                        sender.sendMessage(Messages.SPAWNER_USAGE);
                        return true;
                    }

                    amount = NumberFormatter.getInstance().filter(args[2]);
                    if (amount.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    target.getInventory().addItem(Items.getInstance().getEnergy(amount));
                    return true;
                case "INFINITE_ENERGY":
                    if (!sender.hasPermission("spawners.admin")) break;

                    if (args.length < 3) {
                        sender.sendMessage(Messages.SPAWNER_USAGE);
                        return true;
                    }

                    amount = NumberFormatter.getInstance().filter(args[2]);
                    if (amount.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    if (amount.compareTo(BigInteger.valueOf(2304)) > 0) amount = BigInteger.valueOf(2304);

                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    ItemStack item = Items.getInstance().getInfiniteEnergy();
                    item.setAmount(amount.intValue());

                    target.getInventory().addItem(item);
                    return true;
                case "PICKAXE":
                    if (!sender.hasPermission("spawners.admin")) break;

                    if (args.length < 3) {
                        sender.sendMessage(Messages.SPAWNER_USAGE);
                        return true;
                    }

                    amount = NumberFormatter.getInstance().filter(args[2]);
                    if (amount.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    for (int i = 0; i < amount.intValue(); ++i) {
                        target.getInventory().addItem(Items.getInstance().getPickaxe());
                    }
                    return true;
                case "REPAIR":
                    if (!sender.hasPermission("spawners.admin")) break;

                    if (args.length < 3) {
                        sender.sendMessage(Messages.SPAWNER_USAGE);
                        return true;
                    }

                    BigInteger percentage = NumberFormatter.getInstance().filter(args[2]);
                    if (percentage.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    target.getInventory().addItem(Items.getInstance().getRepair(percentage.intValue()));
                    return true;
                case "INFINITE_REPAIR":
                    if (!sender.hasPermission("spawners.admin")) break;

                    if (args.length < 3) {
                        sender.sendMessage(Messages.SPAWNER_USAGE);
                        return true;
                    }

                    amount = NumberFormatter.getInstance().filter(args[2]);
                    if (amount.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    if (amount.compareTo(BigInteger.valueOf(2304)) > 0) amount = BigInteger.valueOf(2304);

                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    item = Items.getInstance().getInfiniteRepair();
                    item.setAmount(amount.intValue());

                    target.getInventory().addItem(item);
                    return true;
                case "GIFT":
                    if (!sender.hasPermission("spawners.admin")) break;

                    if (args.length < 2) {
                        sender.sendMessage(Messages.SPAWNER_USAGE);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    target.getInventory().addItem(Items.getInstance().getPresent());
                    return true;
                case "UPDATE":
                    if (!sender.hasPermission("spawners.admin")) break;

                    SpawnerManager.getInstance().updatePrices(true);
                    return true;
            }
        }

        if (player == null) return true;

        Menus.getInstance().openMainMenu(player);
        player.playSound(player.getLocation(), Sound.ENTITY_HORSE_SADDLE, 0.5f, 10f);
        return false;
    }
}
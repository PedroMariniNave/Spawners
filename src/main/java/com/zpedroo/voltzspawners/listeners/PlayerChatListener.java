package com.zpedroo.voltzspawners.listeners;

import com.zpedroo.voltzspawners.hooks.VaultHook;
import com.zpedroo.voltzspawners.spawner.Spawner;
import com.zpedroo.voltzspawners.objects.Manager;
import com.zpedroo.voltzspawners.objects.PlayerChat;
import com.zpedroo.voltzspawners.spawner.PlayerSpawner;
import com.zpedroo.voltzspawners.utils.config.Messages;
import com.zpedroo.voltzspawners.utils.enums.Action;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import com.zpedroo.voltzspawners.utils.menu.Menus;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import static com.zpedroo.voltzspawners.utils.config.Messages.*;
import static com.zpedroo.voltzspawners.utils.config.Settings.*;

public class PlayerChatListener implements Listener {

    private static HashMap<Player, PlayerChat> playerChat;

    static {
        playerChat = new HashMap<>(16);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!playerChat.containsKey(event.getPlayer())) return;

        event.setCancelled(true);

        PlayerChat playerChat = getPlayerChat().remove(event.getPlayer());
        Player player = playerChat.getPlayer();
        String msg = event.getMessage();
        Action action = playerChat.getAction();

        PlayerSpawner playerSpawner = playerChat.getPlayerSpawner();
        Spawner spawner = playerChat.getSpawner();
        switch (action) {
            case BUY_SPAWNER -> {
                BigInteger price = playerChat.getPrice();
                BigInteger money = new BigInteger(String.format("%.0f", VaultHook.getMoney(player)));
                BigInteger amount = null;
                if (StringUtils.equals(msg, "*")) {
                    amount = money.divide(price);

                    if (amount.signum() <= 0) {
                        player.sendMessage(BUY_ALL_ZERO);
                        return;
                    }

                    VaultHook.removeMoney(player, price.multiply(amount).doubleValue());
                    player.getInventory().addItem(spawner.getItem(amount, 100));

                    for (String purchasedMsg : Messages.SUCCESSFUL_PURCHASED) {
                        if (purchasedMsg == null) continue;

                        player.sendMessage(StringUtils.replaceEach(purchasedMsg, new String[]{
                                "{spawner}",
                                "{amount}",
                                "{price}"
                        }, new String[]{
                                spawner.getDisplayName(),
                                NumberFormatter.getInstance().format(amount),
                                NumberFormatter.getInstance().format(price.multiply(amount))
                        }));
                    }

                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 100f);
                    return;
                }

                amount = NumberFormatter.getInstance().filter(msg);
                if (amount.signum() <= 0) {
                    player.sendMessage(INVALID_AMOUNT);
                    return;
                }

                if (money.compareTo(price.multiply(amount)) < 0) {
                    player.sendMessage(INSUFFICIENT_MONEY);
                    return;
                }

                VaultHook.removeMoney(player, price.multiply(amount).doubleValue());
                player.getInventory().addItem(spawner.getItem(amount, 100));

                for (String purchasedMsg : Messages.SUCCESSFUL_PURCHASED) {
                    if (purchasedMsg == null) continue;

                    player.sendMessage(StringUtils.replaceEach(purchasedMsg, new String[]{
                            "{spawner}",
                            "{amount}",
                            "{price}"
                    }, new String[]{
                            spawner.getDisplayName(),
                            NumberFormatter.getInstance().format(amount),
                            NumberFormatter.getInstance().format(price.multiply(amount))
                    }));
                }

                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 100f);
            }
            case ADD_FRIEND -> {
                Player target = Bukkit.getPlayer(msg);
                player.sendMessage(WAIT);
                if (target == null) {
                    player.sendMessage(OFFLINE_PLAYER);
                    return;
                }

                Manager manager = playerSpawner.getManager(target.getUniqueId());
                if (playerSpawner.getOwnerUUID().equals(target.getUniqueId()) || manager != null) {
                    player.sendMessage(HAS_PERMISSION);
                    return;
                }

                playerSpawner.getManagers().add(new Manager(target.getUniqueId(), new ArrayList<>(5)));
                playerSpawner.setQueueUpdate(true);
                Menus.getInstance().openManagersMenu(player, playerSpawner);
            }
            case REMOVE_STACK -> {
                BigInteger stack = playerSpawner.getStack();
                BigInteger tax = BigInteger.valueOf(TAX_REMOVE_STACK);
                if (StringUtils.equals(msg, "*")) {
                    if (stack.compareTo(tax) < 0) {
                        player.sendMessage(StringUtils.replaceEach(REMOVE_STACK_MIN, new String[]{
                                "{tax}"
                        }, new String[]{
                                String.valueOf(tax)
                        }));
                        return;
                    }

                    BigInteger toGive = stack.subtract(stack.multiply(tax).divide(BigInteger.valueOf(100)));

                    playerSpawner.removeStack(stack);
                    player.getInventory().addItem(playerSpawner.getSpawner().getItem(toGive, playerSpawner.getIntegrity()));
                    player.sendMessage(StringUtils.replaceEach(REMOVE_STACK_SUCCESSFUL, new String[]{
                            "{lost}"
                    }, new String[]{
                            NumberFormatter.getInstance().format(stack.subtract(toGive))
                    }));
                    return;
                }

                BigInteger toRemove = NumberFormatter.getInstance().filter(msg);
                if (toRemove.signum() <= 0) {
                    player.sendMessage(INVALID_AMOUNT);
                    return;
                }

                if (toRemove.compareTo(stack) > 0) toRemove = stack;
                if (toRemove.compareTo(tax) < 0) {
                    player.sendMessage(StringUtils.replaceEach(REMOVE_STACK_MIN, new String[]{
                            "{tax}"
                    }, new String[]{
                            String.valueOf(tax)
                    }));
                    return;
                }

                BigInteger toGive = toRemove.subtract(toRemove.multiply(tax).divide(BigInteger.valueOf(100)));
                playerSpawner.removeStack(toRemove);
                player.getInventory().addItem(playerSpawner.getSpawner().getItem(toGive, playerSpawner.getIntegrity()));
                player.sendMessage(StringUtils.replaceEach(REMOVE_STACK_SUCCESSFUL, new String[]{
                        "{lost}"
                }, new String[]{
                        NumberFormatter.getInstance().format(toRemove.subtract(toGive))
                }));
            }
        }
    }

    public static HashMap<Player, PlayerChat> getPlayerChat() {
        return playerChat;
    }
}
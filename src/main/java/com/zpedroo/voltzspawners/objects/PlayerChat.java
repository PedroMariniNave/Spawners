package com.zpedroo.voltzspawners.objects;

import com.zpedroo.voltzspawners.spawner.Spawner;
import com.zpedroo.voltzspawners.spawner.PlayerSpawner;
import com.zpedroo.voltzspawners.utils.enums.Action;
import org.bukkit.entity.Player;

import java.math.BigInteger;

public class PlayerChat {

    private Player player;
    private PlayerSpawner playerSpawner;
    private Spawner spawner;
    private BigInteger price;
    private Action action;

    public PlayerChat(Player player, PlayerSpawner playerSpawner, Action action) {
        this.player = player;
        this.playerSpawner = playerSpawner;
        this.action = action;
    }

    public PlayerChat(Player player, Spawner spawner, BigInteger price, Action action) {
        this.player = player;
        this.spawner = spawner;
        this.price = price;
        this.action = action;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerSpawner getPlayerSpawner() {
        return playerSpawner;
    }


    public Spawner getSpawner() {
        return spawner;
    }

    public BigInteger getPrice() {
        return price;
    }

    public Action getAction() {
        return action;
    }
}
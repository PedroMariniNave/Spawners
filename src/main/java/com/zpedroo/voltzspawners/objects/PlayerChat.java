package com.zpedroo.voltzspawners.objects;

import com.zpedroo.multieconomy.objects.Currency;
import com.zpedroo.voltzspawners.enums.PlayerAction;
import org.bukkit.entity.Player;

import java.math.BigInteger;

public class PlayerChat {

    private Player player;
    private PlacedSpawner placedSpawner;
    private Spawner spawner;
    private BigInteger price;
    private Currency currency;
    private PlayerAction playerAction;

    public PlayerChat(Player player, BigInteger price, Currency currency, PlayerAction playerAction) {
        this(player, null, null, price, currency, playerAction);
    }

    public PlayerChat(Player player, PlacedSpawner placedSpawner, PlayerAction playerAction) {
        this(player, placedSpawner, null, null, null, playerAction);
    }

    public PlayerChat(Player player, Spawner spawner, Currency currency, BigInteger price, PlayerAction playerAction) {
        this(player, null, spawner, price, currency, playerAction);
    }

    public PlayerChat(Player player, PlacedSpawner placedSpawner, Spawner spawner, BigInteger price, Currency currency, PlayerAction playerAction) {
        this.player = player;
        this.placedSpawner = placedSpawner;
        this.spawner = spawner;
        this.price = price;
        this.currency = currency;
        this.playerAction = playerAction;
    }

    public Player getPlayer() {
        return player;
    }

    public PlacedSpawner getPlacedSpawner() {
        return placedSpawner;
    }

    public Spawner getSpawner() {
        return spawner;
    }

    public BigInteger getPrice() {
        return price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public PlayerAction getAction() {
        return playerAction;
    }
}
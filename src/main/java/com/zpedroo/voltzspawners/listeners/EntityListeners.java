package com.zpedroo.voltzspawners.listeners;

import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.managers.EntityManager;
import com.zpedroo.voltzspawners.objects.PlayerSpawner;
import com.zpedroo.voltzspawners.utils.config.Settings;
import com.zpedroo.voltzspawners.utils.config.Titles;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;

public class EntityListeners implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        LivingEntity entity = (LivingEntity) event.getEntity();

        if (!(event.getDamager() instanceof Player)) return;
        if (!entity.hasMetadata("MobAmount")) return;

        String serialized = entity.getMetadata("Spawner").get(0).asString();
        PlayerSpawner spawner = DataManager.getInstance().getSpawner(DataManager.getInstance().deserializeLocation(serialized));
        if (spawner == null) return;
        if (!spawner.canInteract(player) && !spawner.isPublic()) {
            event.setCancelled(true);
            return;
        }
        if (entity.getHealth() - event.getDamage() > 0) return;

        event.setDamage(0);
        entity.setHealth(entity.getMaxHealth());

        BigInteger stack = new BigInteger(entity.getMetadata("MobAmount").get(0).asString());
        BigInteger drops = setLooting(spawner.getSpawner().getAmount().multiply(stack), getLootingBonuses(player.getItemInHand()));

        spawner.addDrops(drops);

        EntityManager.removeStack(entity, stack, spawner);

        if (spawner.isPublic()) {
            spawner.sellDrops(player);
            return;
        }

        player.sendTitle(StringUtils.replaceEach(Titles.WHEN_KILL_TITLE, new String[]{
                "{mobs}",
                "{drops}"
        }, new String[]{
                NumberFormatter.getInstance().format(stack),
                NumberFormatter.getInstance().format(drops)
        }), StringUtils.replaceEach(Titles.WHEN_KILL_SUBTITLE, new String[]{
                "{mobs}",
                "{drops}"
        }, new String[]{
                NumberFormatter.getInstance().format(stack),
                NumberFormatter.getInstance().format(drops)
        }));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(EntityDeathEvent event) {
        if (!event.getEntity().hasMetadata("MobAmount")) return;

        event.getDrops().clear();
        event.getEntity().remove();
    }

    private Integer getLootingBonuses(ItemStack item) {
        if (item == null || item.getType().equals(Material.AIR) || !item.hasItemMeta()) return 0;

        return item.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
    }

    private BigInteger setLooting(BigInteger value, int looting) {
        if (looting <= 0) return value;

        return value.add(value.multiply(new BigInteger(Settings.LOOTING_BONUS).divide(BigInteger.valueOf(100L))).multiply(BigInteger.valueOf(looting)));
    }
}
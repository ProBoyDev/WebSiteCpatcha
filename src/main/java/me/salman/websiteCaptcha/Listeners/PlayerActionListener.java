package me.salman.websiteCaptcha.Listeners;

import me.salman.websiteCaptcha.Main;
import me.salman.websiteCaptcha.Manager.VerificationManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.ChatColor;

public class PlayerActionListener implements Listener {
    private final VerificationManager verificationManager;

    public PlayerActionListener(VerificationManager verificationManager) {
        this.verificationManager = verificationManager;
    }

    private String formatMessage(String message) {
        String msg = Main.getInstance().getConfig().getString("messages." + message);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @EventHandler
    public void onPlayerBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!verificationManager.isPlayerVerified(player.getUniqueId()) && !Main.getInstance().getConfig().getBoolean("options.blockBreaking")) {
            player.sendMessage(formatMessage("block_breaking"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!verificationManager.isPlayerVerified(player.getUniqueId()) && !Main.getInstance().getConfig().getBoolean("options.blockPlacing")) {
            player.sendMessage(formatMessage("block_placing"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!verificationManager.isPlayerVerified(player.getUniqueId()) && !Main.getInstance().getConfig().getBoolean("options.itemDrop")) {
            player.sendMessage(formatMessage("item_dropping"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!verificationManager.isPlayerVerified(player.getUniqueId()) && !Main.getInstance().getConfig().getBoolean("options.itemPickup")) {
            player.sendMessage(formatMessage("item_picking_up"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!verificationManager.isPlayerVerified(player.getUniqueId()) && !Main.getInstance().getConfig().getBoolean("options.inventoryClick")) {
            player.sendMessage(formatMessage("inventory_clicking"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity damaged = event.getEntity();

        if (damaged instanceof Player) {
            Player player = (Player) damaged;

            if (!verificationManager.isPlayerVerified(player.getUniqueId())) {
                if (damager instanceof Player && !Main.getInstance().getConfig().getBoolean("options.damage.attack")) {
                    player.sendMessage(formatMessage("attack"));
                    event.setCancelled(true);
                    return;
                }

                if (!(damager instanceof Player) && !Main.getInstance().getConfig().getBoolean("options.damage.receive")) {
                    player.sendMessage(formatMessage("receive_damage"));
                    event.setCancelled(true);
                }
            }
        }
    }
}

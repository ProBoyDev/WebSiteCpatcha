package me.salman.websiteCaptcha.Listeners;

import me.salman.websiteCaptcha.Main;
import me.salman.websiteCaptcha.Manager.VerificationManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {
    private final VerificationManager verificationManager;

    public PlayerChatListener(VerificationManager verificationManager) {
        this.verificationManager = verificationManager;
    }

    private String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!verificationManager.isPlayerVerified(player.getUniqueId())
                && Main.getInstance().getConfig().getBoolean("options.prevent_chat")) {
            event.setCancelled(true);
            player.sendMessage(formatMessage(Main.getInstance().getConfig().getString("messages.no_chat_permission")));
        }
    }
}

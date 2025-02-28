package me.salman.websiteCaptcha.Listeners;

import me.salman.websiteCaptcha.Manager.VerificationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class PlayerMoveListener implements Listener {
    private final VerificationManager verificationManager;

    public PlayerMoveListener(VerificationManager verificationManager) {
        this.verificationManager = verificationManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        boolean preventMovement = verificationManager.getPlugin().getConfig().getBoolean("options.prevent_unverified_player_movement", true);

        if (preventMovement && !verificationManager.isPlayerVerified(playerUUID)) {
            event.setCancelled(true);
//            verificationManager.getPlugin().getLogger().info("Prevented movement for unverified player " + playerUUID);
        } else {
//            verificationManager.getPlugin().getLogger().info("Allowed movement for verified player " + playerUUID);
        }
    }
}
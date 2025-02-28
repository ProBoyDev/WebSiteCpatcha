package me.salman.websiteCaptcha.Manager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryManager {
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

    public void hideInventory(Player player) {
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
        player.getInventory().clear();
    }

    public void restoreInventory(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (savedInventories.containsKey(playerUUID)) {
            player.getInventory().setContents(savedInventories.get(playerUUID));
            savedInventories.remove(playerUUID);
        }
    }

    public boolean isInventoryHidden(UUID playerUUID) {
        return savedInventories.containsKey(playerUUID);
    }
}


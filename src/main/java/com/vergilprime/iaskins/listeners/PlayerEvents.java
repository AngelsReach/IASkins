package com.vergilprime.iaskins.listeners;

import com.vergilprime.iaskins.IASkins;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class PlayerEvents implements Listener {
	IASkins plugin;

	public PlayerEvents(IASkins plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		player.getInventory().forEach(item -> {
			CustomStack stack = CustomStack.byItemStack(item);
			if (stack == null) {
				return;
			}
			// If the custom item is a skinned item
			String skinName = plugin.skinsReversed.get(stack.getNamespacedID());
			if (skinName != null) {
				plugin.skinsController.addLostSkin(player, skinName);
				// Remove the skin from the item
				plugin.skinsController.unskin(item);
			} else if (plugin.skinsController.skins.containsKey(stack.getNamespacedID())) {
				plugin.skinsController.addLostSkin(player, stack.getNamespacedID());
				// Remove the skin item from the player's inventory
			}
		});
	}

	@EventHandler
	public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		if (plugin.skinsController.lostSkins.containsKey(uuid)) {
			player.sendMessage("You died and lost your skins. Use whatever assbrained command we come up with to retieve your lost skins.");
		}
	}
}

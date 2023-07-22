package com.vergilprime.iaskins.listeners;

import com.vergilprime.iaskins.IASkins;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class PlayerEvents implements Listener {
	IASkins plugin;
	// Declare a HashMap to store the cooldowns
	private HashMap<UUID, Long> cooldowns = new HashMap<>();

	public PlayerEvents(IASkins plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		plugin.playerController.rescueSkins(player, event.getDrops());
	}

	@EventHandler
	public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		plugin.playerController.restoreSkins(player);
	}

	//TODO: We're only using ItemDamageEvent instead of ItemBreakEvent because ItemsAdder items don't fire ItemBreakEvent.
/*	@EventHandler
	public void OnPlayerItemDamageEvent(PlayerItemDamageEvent event) {
		if (!event.isCancelled()) {
			Player player = event.getPlayer();
			ItemStack itemStack = event.getItem();
			CustomStack customStack = CustomStack.byItemStack(itemStack);
			if (customStack != null) {
				if (customStack.getDurability() - event.getDamage() <= 0) {
					if (plugin.skinsController.isSkinned(itemStack)) {
						plugin.skinsController.giveSkin(player, plugin.skinsController.getSkinId(itemStack), true);
					}
				}
			}
		}
	}*/
	@EventHandler
	public void OnPlayerItemDamageEvent(PlayerItemDamageEvent event) {
		if (!event.isCancelled()) {
			Player player = event.getPlayer();
			ItemStack itemStack = event.getItem();
			CustomStack customStack = CustomStack.byItemStack(itemStack);
			if (customStack != null) {
				if (customStack.getDurability() - event.getDamage() <= 0) {
					if (plugin.skinsController.isSkinned(itemStack)) {
						// Get the current time in milliseconds
						long currentTime = System.currentTimeMillis();
						// Get the last time the player received a skin, or 0 if none
						long lastTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
						// Check if the current time is more than 1 second after the last time
						if (currentTime - lastTime > 1000) {
							// Give the player a skin and update the cooldown
							plugin.skinsController.giveSkin(player, plugin.skinsController.getSkinId(itemStack), true);
							cooldowns.put(player.getUniqueId(), currentTime);
						}
					}
				}
			}
		}
	}
}

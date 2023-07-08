package com.vergilprime.iaskins.listeners;

import com.vergilprime.iaskins.IASkins;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerEvents implements Listener {
	IASkins plugin;

	public PlayerEvents(IASkins plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		plugin.playerController.rescueSkins(player);
	}

	@EventHandler
	public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		plugin.playerController.restoreSkins(player);
	}

	@EventHandler
	public void onPlayerJoinEvent(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			plugin.playerController.skinMainHand(player);
		}
	}
}

package com.vergilprime.iaskins.listeners;

import com.vergilprime.iaskins.IASkins;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerEvents implements Listener {
	IASkins plugin;

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

	//@EventHandler
	//public void onPlayerItemBreakEvent(PlayerItemBreakEvent event) {
	//	Player player = event.getPlayer();
	//	if (plugin.skinsController.isSkinned(event.getBrokenItem())) {
	//		String skinname = plugin.skinsController.unskin(event.getBrokenItem()).getSkin();
	//		plugin.skinsController.giveOrStoreSkin(player, skinname, true);
	//	}
	//}
}

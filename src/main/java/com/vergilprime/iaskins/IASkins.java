package com.vergilprime.iaskins;

import com.vergilprime.iaskins.controllers.PlayerController;
import com.vergilprime.iaskins.controllers.SkinsController;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.mineacademy.fo.plugin.SimplePlugin;


public final class IASkins extends SimplePlugin {
	public PlayerController playerController;
	public SkinsController skinsController;

	public FileConfiguration skinsConfig = new YamlConfiguration();


	@Override
	public void onPluginStart() {
		// Do we need to register the onItemsAdderLoadDataEvent or something?
	}

	@EventHandler
	public void onItemsAdderLoadDataEvent(ItemsAdderLoadDataEvent event) {
		playerController = new PlayerController(this);
		skinsController = new SkinsController(this);
	}

}

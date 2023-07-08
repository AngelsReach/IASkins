package com.vergilprime.iaskins;

import com.vergilprime.iaskins.commands.ApplySkinCommand;
import com.vergilprime.iaskins.commands.LostSkinsCommand;
import com.vergilprime.iaskins.commands.UnskinCommand;
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
	public boolean debug = true;

	public FileConfiguration skinsConfig = new YamlConfiguration();


	@Override
	public void onPluginStart() {
		// Do we need to register the onItemsAdderLoadDataEvent or something?

	}

	@Override
	protected void onReloadablesStart() {
		playerController = new PlayerController(this);
		registerCommand(new ApplySkinCommand(this));
		registerCommand(new UnskinCommand(this));
		registerCommand(new LostSkinsCommand(this));
	}

	@EventHandler
	public void onItemsAdderLoadDataEvent(ItemsAdderLoadDataEvent event) {
		skinsController = new SkinsController(this);
	}

}

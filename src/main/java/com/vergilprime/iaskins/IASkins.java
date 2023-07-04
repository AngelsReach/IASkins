package com.vergilprime.iaskins;

import com.vergilprime.iaskins.controllers.PlayerController;
import com.vergilprime.iaskins.controllers.SkinsController;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;


public final class IASkins extends SimplePlugin {
	public PlayerController playerController;
	public SkinsController skinsController;

	public FileConfiguration skinsConfig = new YamlConfiguration();


	@Override
	public void onPluginStart() {
	}

	@EventHandler
	public void onItemsAdderLoadDataEvent(ItemsAdderLoadDataEvent event) {
		//skins = skinsConfig.get < Map < String, Map < String, String >>> ("skins");
		try {
			skinsConfig.load("skins.yml");
		} catch (Exception e) {
			this.getLogger().log(Level.SEVERE, "Some file bullshit", e);
		}
		//skins.yml the file that holds the info we need.
		for (String skinName : skinsConfig.getKeys(false)) {
			ConfigurationSection section = skinsConfig.getConfigurationSection(skinName);
			if (section == null) {
				continue;
			}
			Map<String, String> skinPairs = new HashMap<>();
			for (String itemType : section.getKeys(false)) {
				skinPairs.put(itemType, section.getString(itemType));
				skinsController.skinsReversed.put(section.getString(itemType), skinName);
			}
			skinsController.skins.put(skinName, skinPairs);
		}

	}

}

package com.vergilprime.iaskins.controllers;

import com.vergilprime.iaskins.IASkins;
import com.vergilprime.iaskins.utils.ItemSkinPair;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;

public class SkinsController {

	private final IASkins plugin;
	// Map of skins loaded from config when this object is created
	public final Map<String, Map<String, String>> skins;
	// A map of the skinned items to their skin in reverse
	public final Map<String, String> skinsReversed;
	// A map of the player's UUID to a list of skins they lost
	final Map<UUID, List<String>> lostSkins;

	public FileConfiguration skinsConfig = new YamlConfiguration();

	// Load the skins and lost skins from the config. Tied to ItemsAdderLoadDataEvent so
	// everything should be reloaded when ItemsAdder reloads.
	public SkinsController(IASkins plugin) {
		this.plugin = plugin;
		this.skins = new HashMap<>();
		this.skinsReversed = new HashMap<>();
		this.lostSkins = new HashMap<>();
		loadSkins();
		loadLostSkins();
	}

	// Apply the skin provided to the item provided. Returns new item with skin applied.
	public ItemStack applySkin(ItemStack item, String skin) {
		String itemID = CustomStack.byItemStack(item).getNamespacedID();
		if (itemID == null) {
			itemID = item.getType().toString();
		}
		ItemStack newItem = CustomStack.getInstance(skins.get(skin).get(itemID)).getItemStack();
		newItem = copyData(item, newItem);
		return newItem;
	}

	// Gives a skin item to the player OR stashes in their enderchest OR stores in lost skins.
	void giveOrStoreSkin(Player player, String skin, Boolean silent) {
		Map<Integer, ItemStack> leftovers = giveSkin(player, skin, silent);
		if (!leftovers.isEmpty()) {
			storeSkin(player, skin, silent);
		}
	}

	// Gives a skin item to the player OR stashes in their enderchest OR returns leftovers.
	Map<Integer, ItemStack> giveSkin(Player player, String skin, Boolean silent) {
		ItemStack skinItem = CustomStack.getInstance(skins.get(skin).get("skin")).getItemStack();
		Inventory inventory = player.getInventory();
		Map<Integer, ItemStack> leftovers = inventory.addItem(skinItem);
		if (!leftovers.isEmpty()) {
			inventory = player.getEnderChest();
			leftovers = inventory.addItem(skinItem);
			if (leftovers.isEmpty() && !silent) {
				player.sendMessage("Your skin was stashed in your enderchest.");
			}
		} else {
			if (!silent) {
				player.sendMessage("Skin deposited in your inventory.");
			}
		}
		return leftovers;
	}

	// Stores a skin in the aether for the player to recover later.
	// Happens if they die or strip a skin from an item with no room for it.
	void storeSkin(Player player, String skin, Boolean silent) {
		Inventory inventory = player.getEnderChest();
		ItemStack skinItem = CustomStack.getInstance(skins.get(skin).get("skin")).getItemStack();
		Map<Integer, ItemStack> leftovers = inventory.addItem(skinItem);
		if (leftovers.isEmpty() && !silent) {
			player.sendMessage("Your skin was stashed in your enderchest.");
		} else {
			UUID playerUUID = player.getUniqueId();
			lostSkins.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(skin);
			saveLostSkins();
			if (!silent) {
				player.sendMessage("Your skin was stored in the aether, use /lostskins to recover skins when you have room.");
			}
		}

	}

	// Gets the namespace ID from ItemsAdder if the item is a skin item.
	public Optional<String> getSkinId(ItemStack stack) {
		CustomStack customStack = CustomStack.byItemStack(stack);
		if (customStack == null) {
			return Optional.empty();
		}
		// If the custom item is a skinned item
		String skinName = skinsReversed.get(customStack.getNamespacedID());
		return Optional.ofNullable(skinName);
	}

	public void addLostSkin(Player player, String skinName) {
		UUID uuid = player.getUniqueId();
		List<String> playerLostSkins = lostSkins.get(uuid);
		if (playerLostSkins != null) {
			playerLostSkins.add(skinName);
		} else {
			lostSkins.put(uuid, Collections.singletonList(skinName));
		}
	}

	// Maybe deprecated
	public void removeLostSkin(Player player, String skinName) {
		UUID uuid = player.getUniqueId();
		List<String> playerLostSkins = lostSkins.get(uuid);
		if (playerLostSkins != null) {
			playerLostSkins.remove(skinName);
			if (playerLostSkins.isEmpty()) {
				lostSkins.remove(uuid);
			}
		}
	}

	// Save all lost skins to the yaml file
	public void saveLostSkins() {
		YamlConfiguration lostSkinYaml = new YamlConfiguration();
		lostSkins.forEach((uuid, list) -> lostSkinYaml.set(uuid.toString(), list));
		try {
			lostSkinYaml.save("lostSkins.yml");
		} catch (IOException e) {
			plugin.getLogger().severe("lostSkins.yml couldn't be saved.");
			e.printStackTrace();
		}
	}

	// Load all lost skins from the yaml file. Used on creation of a new SkinsController.
	public void loadLostSkins() {
		YamlConfiguration lostSkinYaml = new YamlConfiguration();
		try {
			lostSkinYaml.load("lostSkins.yml");
		} catch (IOException | InvalidConfigurationException e) {
			plugin.getLogger().severe("lostSkins.yml couldn't be loaded.");
			e.printStackTrace();
		}

		if (lostSkinYaml.getKeys(false) != null) {
			lostSkinYaml.getKeys(false).forEach(key -> {
				UUID uuid;
				try {
					uuid = UUID.fromString(key);
				} catch (IllegalArgumentException e) {
					return;
				}
				List<String> skinList = lostSkinYaml.getStringList(key);
				if (!skinList.isEmpty()) {
					lostSkins.put(uuid, skinList);
				}
			});
		}
	}

	// Load all skins from the yaml file. Used on creation of a new SkinsController.
	public void loadSkins() {
		YamlConfiguration skinYaml = new YamlConfiguration();
		try {
			skinYaml.load("skins.yml");
		} catch (IOException | InvalidConfigurationException e) {
			plugin.getLogger().severe("skins.yml couldn't be loaded.");
			e.printStackTrace();
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
				skinsReversed.put(section.getString(itemType), skinName);
			}
			skins.put(skinName, skinPairs);
		}
	}

	// Given a skinned item, return the unskinned item and the namespace ID of the skin.
	public ItemSkinPair unskin(ItemStack item) {
		if (item == null || !isSkinned(item)) {
			return null;
		}

		Optional<String> skinnedName = getSkinId(item);
		if (!skinnedName.isPresent()) {
			return null;
		}

		// Create new item without ItemsAdder
		String unskinnedName = skinsReversed.get(skinnedName.get());
		ItemStack newItem;
		if (!CustomStack.isInRegistry(unskinnedName)) {
			unskinnedName = item.getType().name();
			newItem = new ItemStack(item.getType());
		} else {
			newItem = CustomStack.getInstance(unskinnedName).getItemStack();
		}
		newItem = copyData(item, newItem);
		// Replace the original item (main hand) with the new item
		return new ItemSkinPair(newItem, unskinnedName);
	}

	// Given an old item and a new item, copy title, lore, enchants, attributes and damage to the new item.
	private ItemStack copyData(ItemStack item, ItemStack newItem) {
		CustomStack customStack = CustomStack.byItemStack(item);
		if (customStack != null) {
			CustomStack defaultStack = CustomStack.getInstance(customStack.getNamespacedID());
			if (!Objects.equals(customStack.getDisplayName(), defaultStack.getDisplayName())) {
				ItemMeta itemMeta = newItem.getItemMeta();
				if (itemMeta != null) {
					itemMeta.setDisplayName(customStack.getDisplayName());
					newItem.setItemMeta(itemMeta);
				}
			}
		} else {
			ItemMeta itemMeta = newItem.getItemMeta();
			if (itemMeta != null && item.getItemMeta().hasDisplayName()) {
				itemMeta.setDisplayName(item.getItemMeta().getDisplayName());
				newItem.setItemMeta(itemMeta);
			}
		}

		try {
			ItemMeta itemMeta = newItem.getItemMeta();
			if (itemMeta != null) {
				itemMeta.setLore(item.getItemMeta().getLore());
				newItem.setItemMeta(itemMeta);
			}
		} catch (NullPointerException ignored) {
		}

		try {
			ItemMeta itemMeta = newItem.getItemMeta();
			if (itemMeta != null) {
				itemMeta.setAttributeModifiers(item.getItemMeta().getAttributeModifiers());
				newItem.setItemMeta(itemMeta);
			}
		} catch (NullPointerException ignored) {
		}

		try {
			newItem.addEnchantments(item.getEnchantments());
		} catch (NullPointerException ignored) {
		}

		try {
			newItem.addEnchantments(item.getEnchantments());
		} catch (NullPointerException ignored) {
		}

		newItem.setDurability(item.getDurability()); // TODO: ItemStack.setDurability is deprecated.

		return newItem;
	}

	// Is this item a configured skin?
	public boolean isSkin(ItemStack item) {
		return getSkinId(item).isPresent();
	}

	// Is this item a skinned item?
	public boolean isSkinned(ItemStack item) {
		return getSkinId(item).isPresent();
	}

	// Given a skinned item, return the namespace ID of the skin. If the item is not a skinned item, return null.
	private Optional<String> getSkinnedId(ItemStack item) {
		CustomStack customStack = CustomStack.byItemStack(item);
		if (customStack == null) {
			return Optional.empty();
		}
		// If the custom item is a skinned item
		String skinnedName = skinsReversed.get(customStack.getNamespacedID());
		return Optional.ofNullable(skinnedName);
	}
}

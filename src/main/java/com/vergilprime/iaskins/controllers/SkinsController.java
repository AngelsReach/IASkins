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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;

public class SkinsController {

	private final IASkins plugin;
	// Where all the skins are configured

	public FileConfiguration skinsConfig = new YamlConfiguration();
	// Where the lost skins are stored
	private final YamlConfiguration lostSkinsYml = new YamlConfiguration();
	// Map of skins loaded from config when this object is created
	public final Map<String, Map<String, String>> skins;
	// A map of the skinned items to their skin in reverse
	public final Map<String, String> skinsReversed;
	// A map of the player's UUID to a list of skins they lost
	final Map<UUID, List<String>> lostSkins;


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
		boolean debug = plugin.debug;
		if (debug) plugin.getServer().broadcastMessage("SkinID = " + skin);
		CustomStack customStack = CustomStack.byItemStack(item);
		String itemID;
		if (customStack == null) {
			if (debug) plugin.getServer().broadcastMessage("ItemStack provided is not an ItemsAdder item.");
			itemID = item.getType().toString().toLowerCase();
		} else {
			itemID = customStack.getId();
		}
		if (debug) plugin.getServer().broadcastMessage("ItemID = " + itemID);
		String skinnedId = skins.get(skin).get(itemID);
		ItemStack newItem = CustomStack.getInstance(skinnedId).getItemStack();
		if (debug) plugin.getServer().broadcastMessage("newItem created.");
		newItem = copyData(item, newItem);
		if (debug) plugin.getServer().broadcastMessage("newItem data copied from old item.");
		return newItem;
	}

	// Gives a skin item to the player OR stashes in their enderchest OR stores in lost skins.
	public void giveOrStoreSkin(Player player, String skin, Boolean silent) {
		Map<Integer, ItemStack> leftovers = giveSkin(player, skin, silent);
		if (!leftovers.isEmpty()) {
			storeSkin(player, skin, silent);
		}
	}

	// Gives a skin item to the player OR stashes in their enderchest OR returns leftovers.
	Map<Integer, ItemStack> giveSkin(Player player, String skin, Boolean silent) {
		ItemStack skinItem = CustomStack.getInstance(skin).getItemStack();
		Inventory inventory = player.getInventory();
		Map<Integer, ItemStack> leftovers = inventory.addItem(skinItem);
		// if (!leftovers.isEmpty()) {
		// inventory = player.getEnderChest();
		// leftovers = inventory.addItem(skinItem);
		// if (leftovers.isEmpty() && !silent) {
		// player.sendMessage("Your skin was stashed in your enderchest.");
		// }
		// } else {
		if (!silent) {
			player.sendMessage("Skin deposited in your inventory.");
		}
		// }
		return leftovers;
	}

	// Stores a skin in the aether for the player to recover later.
	// Happens if they die or strip a skin from an item with no room for it.
	void storeSkin(Player player, String skin, Boolean silent) {
		// Inventory inventory = player.getEnderChest();
		// ItemStack skinItem = CustomStack.getInstance(skins.get(skin).get("skin")).getItemStack();
		// Map<Integer, ItemStack> leftovers = inventory.addItem(skinItem);
		// if (leftovers.isEmpty() && !silent) {
		// player.sendMessage("Your skin was stashed in your enderchest.");
		// } else {
		UUID playerUUID = player.getUniqueId();
		lostSkins.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(skin);
		saveLostSkins();
		if (!silent) {
			player.sendMessage("Your skin was stored in the aether, use /lostskins to recover skins when you have room.");
		}
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

	// Load all skins from the yaml file. Used on creation of a new SkinsController.
	public void loadSkins() {
		plugin.saveResource("skins.yml", false);
		String path = plugin.getDataFolder() + "/skins.yml";
		try {
			skinsConfig.load(path);
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

	// Load all lost skins from the yaml file. Used on creation of a new SkinsController.
	public void loadLostSkins() {
		plugin.saveResource("lostSkins.yml", false);
		String path = plugin.getDataFolder() + "/lostSkins.yml";
		try {
			lostSkinsYml.load(path);

			if (lostSkinsYml.getKeys(false) != null) {
				lostSkinsYml.getKeys(false).forEach(key -> {
					UUID uuid;
					try {
						uuid = UUID.fromString(key);
					} catch (IllegalArgumentException e) {
						return;
					}
					List<String> lostSkinList = lostSkinsYml.getStringList(key);
					if (!lostSkinList.isEmpty()) {
						lostSkins.put(uuid, lostSkinList);
					}
				});
			}
		} catch (IOException | InvalidConfigurationException e) {
			plugin.getLogger().severe("lostSkins.yml couldn't be loaded.");
			e.printStackTrace();
		}
	}

	// Save all lost skins to the yaml file
	public void saveLostSkins() {
		String path = plugin.getDataFolder() + "/lostSkins.yml";
		lostSkins.forEach((uuid, list) -> lostSkinsYml.set(uuid.toString(), list));
		try {
			lostSkinsYml.save(path);
		} catch (IOException e) {
			plugin.getLogger().severe("lostSkins.yml couldn't be saved.");
			e.printStackTrace();
		}
	}

	// Given a skinned item, return the unskinned item and the namespace ID of the skin.
	public ItemSkinPair unskin(ItemStack item) {
		if (item == null || !isSkinned(item)) {
			return null;
		}

		String skinnedName = getSkinnedId(item);
		if (skinnedName == null) {
			return null;
		}

		// Create new item without ItemsAdder
		String unskinnedName = item.getType().toString();
		String skinName = skinsReversed.get(skinnedName);
		ItemStack newItem;
		if (!CustomStack.isInRegistry(unskinnedName)) {
			unskinnedName = item.getType().name();
			newItem = new ItemStack(item.getType());
		} else {
			newItem = CustomStack.getInstance(unskinnedName).getItemStack();
		}
		newItem = copyData(item, newItem);
		// Replace the original item (main hand) with the new item
		return new ItemSkinPair(newItem, skinName);
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
			newItem.addUnsafeEnchantments(item.getEnchantments());
		} catch (NullPointerException ignored) {
		}

		Damageable damageableMeta = (Damageable) item.getItemMeta();
		int damage = damageableMeta.getDamage();

		CustomStack newCustomStack = CustomStack.byItemStack(newItem);
		newCustomStack.setDurability(newCustomStack.getMaxDurability() - damage);

		return newCustomStack.getItemStack();
	}

	// Is this item a configured skin?
	public boolean isSkin(ItemStack stack) {
		CustomStack customStack = CustomStack.byItemStack(stack);
		if (customStack == null) {
			return false;
		}
		String namespacedId = customStack.getNamespacedID();
		return skins.containsKey(namespacedId);
	}

	// Is this item a skinned item?
	public boolean isSkinned(ItemStack stack) {
		CustomStack customStack = CustomStack.byItemStack(stack);
		if (customStack == null) {
			return false;
		}
		String namespacedId = customStack.getNamespacedID();
		return skinsReversed.containsKey(namespacedId);
	}


	// Gets the namespace ID from ItemsAdder if the item is a skin item.
	public String getSkinId(ItemStack stack) {
		if (isSkin(stack)) {
			return CustomStack.byItemStack(stack).getNamespacedID();
		}
		return null;
	}

	// Given a skinned item, return the namespace ID of the skin. If the item is not a skinned item, return null.
	private String getSkinnedId(ItemStack stack) {
		if (isSkinned(stack)) {
			return CustomStack.byItemStack(stack).getNamespacedID();
		}
		return null;
	}
}

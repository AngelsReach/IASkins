package com.vergilprime.iaskins.controllers;

import com.vergilprime.iaskins.IASkins;
import com.vergilprime.iaskins.utils.ItemSkinPair;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;

public class SkinsController {

	private final IASkins plugin;
	public final Map<String, Map<String, String>> skins;
	public final Map<String, String> skinsReversed;
	private final Map<UUID, List<String>> lostSkins;

	public SkinsController(IASkins plugin) {
		this.plugin = plugin;
		this.skins = new HashMap<>();
		this.skinsReversed = new HashMap<>();
		this.lostSkins = new HashMap<>();
		loadLostSkins();
	}

	// https://itemsadder.devs.beer/developers/java-api/old-api
	public void applySkin(Player player) {
		ItemStack mainhand = player.getInventory().getItemInMainHand();
		ItemStack offhand = player.getInventory().getItemInOffHand();

		// If the player is holding a skin in their off hand
		if (getSkinId(offhand).isPresent() && !isSkinned(mainhand)) {
			// Get the skin id
			String skinId = getSkinId(offhand).get();
			// Get the result of applying the skin to the main hand item
			ItemStack newItem = applySkin(mainhand, skinId);

			// Replace the original item (main hand) with the new item
			player.getInventory().setItemInMainHand(newItem);
			// Remove the skin (off hand)
			offhand.setAmount(offhand.getAmount() - 1);
		}
	}

	public ItemStack applySkin(ItemStack item, String skin) {
		String itemID = CustomStack.byItemStack(item).getNamespacedID();
		if (itemID == null) {
			itemID = item.getType().toString();
		}
		ItemStack newItem = CustomStack.getInstance(skins.get(skin).get(itemID)).getItemStack();
		newItem = copyData(item, newItem);
		return newItem;
	}

	public void unskinMainHand(Player player) {
		ItemStack mainhand = player.getInventory().getItemInMainHand();
		if (isSkinned(mainhand)) {
			ItemSkinPair itemSkinPair = unskin(mainhand);
			player.getInventory().setItemInMainHand(itemSkinPair.getItem());
			giveOrStoreSkin(player, itemSkinPair.getSkin());
		}
	}

	private void giveOrStoreSkin(Player player, String skin) {
		ItemStack skinItem = CustomStack.getInstance(skins.get(skin).get("skin")).getItemStack();
		Inventory inventory = player.getInventory();
		Map<Integer, ItemStack> leftovers = inventory.addItem(skinItem);
		if (!leftovers.isEmpty()) {
			player.sendMessage("Your skin was stashed in your enderchest.");
			inventory = player.getEnderChest();
			leftovers = inventory.addItem(skinItem);
		} else {
			player.sendMessage("Your skin was successfully removed.");
		}
		if (!leftovers.isEmpty()) {
			player.sendMessage("Your skin was stored in the aether, use /lostskins to recover skins when you have room.");
			storeSkin(player, skin);
		}
	}

	void storeSkin(Player player, String skin) {
		UUID playerUUID = player.getUniqueId();
		lostSkins.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(skin);
		saveLostSkins();
	}

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

	public boolean isSkin(ItemStack item) {
		return getSkinId(item).isPresent();
	}

	public boolean isSkinned(ItemStack item) {
		return getSkinId(item).isPresent();
	}

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

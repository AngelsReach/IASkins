package com.vergilprime.iaskins.controllers;

import com.vergilprime.iaskins.IASkins;
import com.vergilprime.iaskins.utils.ItemSkinPair;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SkinsController {
	IASkins plugin;

	public SkinsController(IASkins plugin) {
		this.plugin = plugin;
	}

	// https://itemsadder.devs.beer/developers/java-api/old-api
	public void applySkin(Player player) {
		ItemStack mainhand = player.getInventory().getItemInMainHand();
		ItemStack offhand = player.getInventory().getItemInOffHand();
		Map<String, Map<String, String>> skins = plugin.skins;

		// If the player is holding a skin in their off hand
		CustomStack skinCustomStack = CustomStack.byItemStack(offhand);
		if (skinCustomStack == null) {
			return;
		}
		// Get the list of items that can be skinned with that skin
		String skinName = skinCustomStack.getNamespacedID();

		if (!skins.containsKey(skinName)) {
			// item not configured as a skin item
			return;
		}

		Map<String, String> skinPairs = skins.get(skinName);

		CustomStack customStack = CustomStack.byItemStack(mainhand);

		String itemType;
		if (customStack != null) {
			itemType = customStack.getNamespacedID();
		} else {
			itemType = mainhand.getType().toString();
		}

		if (!skinPairs.containsKey(itemType)) {
			// item not configured to be skinned with that skin
			return;
		}

		// get the itempair that matches the item and skin;
		String skinnedType = skinPairs.get(itemType);

		// Create new item with ItemsAdder
		CustomStack newCustomStack = CustomStack.getInstance(skinnedType);

		// Copy damage, title, lore, and enchants of the original item to the new item.

		// If the original item is a custom item
		ItemStack newCustomItemStack;
		if (customStack != null) {
			// If the displayname of the original item is not default for that itemsadder item
			CustomStack defaultStack = CustomStack.getInstance(itemType);
			if (!customStack.getDisplayName().equals(defaultStack.getDisplayName())) {
				newCustomStack.setDisplayName(customStack.getDisplayName());
			}

			ItemStack customItemStack = customStack.getItemStack();
			newCustomItemStack = newCustomStack.getItemStack();

			// Apply any enchants to the new item that were on the original item
			newCustomItemStack.addEnchantments(customItemStack.getEnchantments());

			// Apply any lore to the new item that was on the original item
			if (customItemStack.getItemMeta().hasLore()) {
				newCustomItemStack.getItemMeta().setLore(customItemStack.getItemMeta().getLore());
			}

			// Apply any damage to the new item that was on the original item
			newCustomItemStack.setDurability(customItemStack.getDurability());
		} else {
			// If the original item had a displayname
			if (mainhand.hasItemMeta() && mainhand.getItemMeta().hasDisplayName()) {
				newCustomStack.setDisplayName(mainhand.getItemMeta().getDisplayName());
			}


			newCustomItemStack = newCustomStack.getItemStack();

			// Apply any enchants to the new item that were on the original item
			newCustomStack.getItemStack().addEnchantments(mainhand.getEnchantments());

			// Apply any lore to the new item that was on the original item
			if (mainhand.hasItemMeta() && mainhand.getItemMeta().hasLore()) {
				newCustomItemStack.getItemMeta().setLore(mainhand.getItemMeta().getLore());
			}

			ItemMeta oldMeta = mainhand.getItemMeta();
			ItemMeta newMeta = newCustomItemStack.getItemMeta();

			// Apply any damage to the new item that was on the original item
			if (oldMeta instanceof Damageable && newMeta instanceof Damageable) {
				((Damageable) newMeta).setDamage(((Damageable) oldMeta).getDamage());
			}
		}

		// Replace the original item (main hand) with the new item
		player.getInventory().setItemInMainHand(newCustomItemStack);
		// Remove the skin (off hand)
		player.getInventory().setItemInOffHand(null);
	}

	public String removeSkinFromMainHand(Player player) {
		ItemStack mainhand = player.getInventory().getItemInMainHand();

		CustomStack customStack = CustomStack.byItemStack(mainhand);

		if (customStack == null) {
			return null;
		}

		String skinnedType = customStack.getNamespacedID();

		// If the player is holding a custom item in their main hand
		Optional<String> skin = getSkin(mainhand);
		if (skin.isPresent()) {
			// Create new item without ItemsAdder
			ItemStack newitem = new ItemStack(mainhand.getType());
			// Copy damage, title, lore, and enchants of the original item to the new item.
			newitem.setItemMeta(mainhand.getItemMeta());
			// Replace the original item (main hand) with the new item
			player.getInventory().setItemInMainHand(newitem);
			player.getInventory().addItem(CustomStack.getInstance(skin.get()).getItemStack());
			return skin.get();
		}
		return null;
	}

	// could be moved into a Utility class and made static to call it from anywhere instead of repeating Code -yaya
	public Optional<String> getSkin(ItemStack stack) {
		CustomStack customStack = CustomStack.byItemStack(stack);
		if (customStack == null)
			return Optional.empty();
		// If the custom item is a skinned item
		String skinName = plugin.skinsReversed.get(customStack.getNamespacedID());
		if (skinName != null) {
			return Optional.of(skinName);
		}
		return Optional.empty();
	}

	public void addLostSkin(Player player, String skinName) {
		UUID uuid = player.getUniqueId();
		if (!plugin.lostSkins.containsKey(uuid)) {
			plugin.lostSkins.put(uuid, new ArrayList<>());
		}
		plugin.lostSkins.get(uuid).add(skinName);
	}

	public void removeLostSkin(Player player, String skinName) {
		UUID uuid = player.getUniqueId();
		if (!plugin.lostSkins.containsKey(uuid)) {
			return;
		}
		plugin.lostSkins.get(uuid).remove(skinName);
		if (plugin.lostSkins.get(uuid).isEmpty()) {
			plugin.lostSkins.remove(uuid);
		}
	}

	// Save lost skins to a yml file
	public void saveLostSkins() {

	}

	public ItemSkinPair removeSkinFromItem(ItemStack customItem) {
		// If the player is holding a custom item in their main hand
		Optional<String> skin = getSkin(customItem);
		if (skin.isPresent()) {
			// Create new item without ItemsAdder
			ItemStack newitem = new ItemStack(customItem.getType());
			// Copy damage, title, lore, and enchants of the original item to the new item.
			newitem.setItemMeta(customItem.getItemMeta());
			// Replace the original item (main hand) with the new item
			return new ItemSkinPair(newitem, skin);
		}
		return null;
	}
}

package com.vergilprime.iaskins.controllers;

import com.vergilprime.iaskins.IASkins;
import com.vergilprime.iaskins.utils.ItemSkinPair;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerController {
	SkinsController skinsController;

	public PlayerController(IASkins plugin) {
		skinsController = plugin.skinsController;
	}

	// Happens after death. If the player has a skin item in their inventory, it is removed and added to their enderchest. If enderchest is full, the item is added after the player respawns.
	void rescueSkins(Player player) {
		// For every item in a player's inventory
		PlayerInventory inventory = player.getInventory();
		ItemStack[] contents = inventory.getContents();
		ItemStack[] armorContents = inventory.getArmorContents();
		ItemStack[] extraContents = inventory.getExtraContents();
		ItemStack offhand = inventory.getItemInOffHand();

		contents = rescueSkins(player, contents);
		armorContents = rescueSkins(player, armorContents);
		extraContents = rescueSkins(player, extraContents);
		offhand = rescueSkin(player, offhand);

		player.getInventory().setContents(contents);
		player.getInventory().setArmorContents(armorContents);
		player.getInventory().setExtraContents(extraContents);
		player.getInventory().setItemInOffHand(offhand);

	}

	ItemStack[] rescueSkins(Player player, ItemStack[] contents) {
		for (int i = 0; i < contents.length; i++)
			contents[i] = rescueSkin(player, contents[i]);

		return contents;
	}

	private ItemStack rescueSkin(Player player, ItemStack item) {
		if (skinsController.isSkin(item)) {
			skinsController.rescueSkin(player, CustomStack.byItemStack(item).getNamespacedID());
			return null;
		} else if (skinsController.isSkinned(item)) {
			ItemSkinPair itemSkinPair = skinsController.unskin(item);
			skinsController.rescueSkin(player, itemSkinPair.skin);
			return itemSkinPair.item;
		}
		return null;
	}
}

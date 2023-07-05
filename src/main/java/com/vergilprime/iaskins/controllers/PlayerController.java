package com.vergilprime.iaskins.controllers;

import com.vergilprime.iaskins.IASkins;
import com.vergilprime.iaskins.utils.ItemSkinPair;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// This class contains methods for player-specific actions like restoring skins after death.
public class PlayerController {
	SkinsController skinsController;

	public PlayerController(IASkins plugin) {
		skinsController = plugin.skinsController;
	}

	// Apply the skin in the player's off hand to the item in their main hand
	// Should be called by a command, a player interact event, and/or called by a custom UI event.
	public void skinMainHand(Player player) {
		ItemStack mainhand = player.getInventory().getItemInMainHand();
		ItemStack offhand = player.getInventory().getItemInOffHand();

		// If the player is holding a skin in their off hand
		if (skinsController.getSkinId(offhand).isPresent() && !skinsController.isSkinned(mainhand)) {
			// Get the skin id
			String skinId = skinsController.getSkinId(offhand).get();
			// Get the result of applying the skin to the main hand item
			ItemStack newItem = skinsController.applySkin(mainhand, skinId);

			// Replace the original item (main hand) with the new item
			player.getInventory().setItemInMainHand(newItem);
			// Remove the skin (off hand)
			offhand.setAmount(offhand.getAmount() - 1);
		}
	}

	// Remove the skin from the item in the player's main hand
	// Should be called by a command, a player interact event, and/or called by a custom UI event.
	public void unskinMainHand(Player player) {
		ItemStack mainhand = player.getInventory().getItemInMainHand();
		if (skinsController.isSkinned(mainhand)) {
			ItemSkinPair itemSkinPair = skinsController.unskin(mainhand);
			player.getInventory().setItemInMainHand(itemSkinPair.getItem());
			skinsController.giveOrStoreSkin(player, itemSkinPair.getSkin(), false);
		}
	}

	// In the event of player death, each item must be stripped of their skin. The item should drop, and the skin should be stored for later.
	public void rescueSkins(Player player) {
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

	// In the event of player death, each item must be stripped of their skin. The item should drop, and the skin should be stored for later.
	ItemStack[] rescueSkins(Player player, ItemStack[] contents) {
		for (int i = 0; i < contents.length; i++)
			contents[i] = rescueSkin(player, contents[i]);

		return contents;
	}

	// In the event of player death, each item must be stripped of their skin. The item should drop, and the skin should be stored for later.
	private ItemStack rescueSkin(Player player, ItemStack item) {
		if (skinsController.isSkin(item)) {
			skinsController.storeSkin(player, CustomStack.byItemStack(item).getNamespacedID(), true);
			return null;
		} else if (skinsController.isSkinned(item)) {
			ItemSkinPair itemSkinPair = skinsController.unskin(item);
			skinsController.storeSkin(player, itemSkinPair.getSkin(), true);
			return itemSkinPair.item;
		}
		return null;
	}

	// Happens after respawn. If the player has lost skins, they are restored.
	public void restoreSkins(Player player) {
		UUID uuid = player.getUniqueId();
		List<String> playerLostSkins = skinsController.lostSkins.get(uuid);
		if (playerLostSkins != null) {
			for (Integer i = 0; i < playerLostSkins.size(); i++) {
				Map<Integer, ItemStack> leftovers = skinsController.giveSkin(player, playerLostSkins.get(i), true);
				if (leftovers.isEmpty()) {
					playerLostSkins.remove(i);
					i--;
				}// TODO: Optimization: Break if inventory is full.
			}
			if (playerLostSkins.isEmpty()) {
				player.sendMessage("Your lost skins have been restored.");
				skinsController.lostSkins.remove(uuid);
			} else {
				player.sendMessage("Some of your lost skins have been restored. Use /iaskins restore to restore the rest when you have more room.");
			}
			skinsController.saveLostSkins();
		}
	}
}

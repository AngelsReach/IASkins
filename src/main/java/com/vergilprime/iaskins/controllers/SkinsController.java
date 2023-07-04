package com.vergilprime.iaskins.controllers;

import com.vergilprime.iaskins.IASkins;
import com.vergilprime.iaskins.utils.ItemSkinPair;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class SkinsController {
	IASkins plugin;

	public Map<String, Map<String, String>> skins;
	public Map<UUID, List<String>> lostSkins;

	public SkinsController(IASkins plugin) {
		this.plugin = plugin;
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
			player.getInventory().getItemInOffHand().setAmount(player.getInventory().getItemInOffHand().getAmount() - 1);
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
			player.getInventory().setItemInMainHand(itemSkinPair.item);
			giveSkin(player, itemSkinPair.skin);
		}
	}

	private void giveSkin(Player player, String skin) {
		HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(CustomStack.getInstance(skins.get(skin).get("skin")).getItemStack());
		if (!leftovers.isEmpty()) {
			leftovers = player.getEnderChest().addItem(CustomStack.getInstance(skins.get(skin).get("skin")).getItemStack());
		}
		if (!leftovers.isEmpty()) {
			storeSkin(player, skin);
		}
	}

	public void rescueSkin(Player player, String skin) {
		HashMap<Integer, ItemStack> leftovers = player.getEnderChest().addItem(CustomStack.getInstance(skins.get(skin).get("skin")).getItemStack());
		if (!leftovers.isEmpty()) {
			storeSkin(player, skin);
		}
	}

	private void storeSkin(Player player, String skin) {
		if (!lostSkins.containsKey(player.getUniqueId())) {
			lostSkins.put(player.getUniqueId(), new ArrayList<>());
		}
		lostSkins.get(player.getUniqueId()).add(skin);
		saveLostSkins();
	}

	private void restoreSkins(Player player) {
		// TODO: Restore lost skins. Any that don't fit in inv or enderchest are stored in lost skins again.
	}

	// could be moved into a Utility class and made static to call it from anywhere instead of repeating Code -yaya
	public Optional<String> getSkinId(ItemStack stack) {
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
		if (!lostSkins.containsKey(uuid)) {
			lostSkins.put(uuid, new ArrayList<>());
		}
		lostSkins.get(uuid).add(skinName);
	}

	public void removeLostSkin(Player player, String skinName) {
		UUID uuid = player.getUniqueId();
		if (!lostSkins.containsKey(uuid)) {
			return;
		}
		lostSkins.get(uuid).remove(skinName);
		if (lostSkins.get(uuid).isEmpty()) {
			lostSkins.remove(uuid);
		}
	}

	public void saveLostSkins() {
		YamlConfiguration lostSkinYaml = new YamlConfiguration();
		lostSkins.forEach((uuid, list) -> lostSkinYaml.set(uuid.toString(), list));
		try
		{
			lostSkinYaml.save("lostSkins.yml");
		}
		catch (IOException e)
		{
			plugin.getLogger().log(Level.SEVERE, "lostSkins.yml couldn't be saved.", e);
		}
	}
	
	public void loadLostSkins()
	{
		YamlConfiguration lostSkinYaml = new YamlConfiguration();
		try
		{
			lostSkinYaml.load("lostSkins.yml");
		}
		catch (IOException e)
		{
			plugin.getLogger().log(Level.SEVERE, "lostSkins.yml couldn't be loaded.", e);
		}
		catch (InvalidConfigurationException e)
		{
			plugin.getLogger().log(Level.SEVERE, "lostSkins.yml is fucked up.", e);
		}
		if(lostSkins == null)
			lostSkins = new HashMap<>();
		//Players that don't have an entry in the file won't get wiped when loading currently, but I find that scenario unlikely;
		//If that behavior is still wanted though, just remove the null check above
		//Or if you want to be more elaborate you could take the set of keys and check if an element isn't in lostSkins and if so remove the entry
		//-yaya
		lostSkinYaml.getKeys(false).forEach(s -> {
			UUID key = UUID.fromString(s);
			lostSkins.put(key, lostSkinYaml.getStringList(s));
		});
	}

	public ItemSkinPair unskin(ItemStack item) {
		// If the player is holding a custom item in their main hand
		if (item == null)
			return null;
		if (!isSkinned(item))
			return null;
		Optional<String> skin = getSkinnedId(item);
		if (!skin.isPresent())
			return null;

		// Create new item without ItemsAdder
		ItemStack newItem = new ItemStack(item.getType());
		// Copy damage, title, lore, and enchants of the original item to the new item.
		// TODO: ignore displayname if it's default
		// if (item.getItemMeta().getDisplayName().equals(CustomStack.getInstance(skin.get()).getItemStack().getItemMeta().getDisplayName()){
		// 		newItem.getItemMeta().setDisplayName(item.getItemMeta().getDisplayName());
		// }
		newItem = copyData(item, newItem);
		// Replace the original item (main hand) with the new item
		return new ItemSkinPair(newItem, skin.get());
	}

	private ItemStack copyData(ItemStack item, ItemStack newItem) {
		try {
			newItem.getItemMeta().setLore(item.getItemMeta().getLore());
		} catch (NullPointerException e) {
			// ignore
		}

		try {
			newItem.getItemMeta().setAttributeModifiers(item.getItemMeta().getAttributeModifiers());
		} catch (NullPointerException e) {
			// ignore
		}

		try {
			newItem.addEnchantments(item.getEnchantments());
		} catch (NullPointerException e) {
			// ignore
		}

		try {
			newItem.addEnchantments(item.getEnchantments());
		} catch (NullPointerException e) {
			// ignore
		}

		// TODO: ItemStack.setDurability is deprecated.
		newItem.setDurability(item.getDurability());

		return newItem;
	}

	public boolean isSkin(ItemStack item) {
		return getSkinId(item).isPresent();
	}

	public boolean isSkinned(ItemStack item) {
		return getSkinnedId(item).isPresent();
	}

	private Optional<String> getSkinnedId(ItemStack item) {
		CustomStack customStack = CustomStack.byItemStack(item);
		if (customStack == null)
			return Optional.empty();
		// If the custom item is a skinned item
		String skinnedName = plugin.skinsReversed.get(customStack.getNamespacedID());
		if (skinnedName != null) {
			return Optional.of(customStack.getNamespacedID());
		}
		return Optional.empty();
	}
}

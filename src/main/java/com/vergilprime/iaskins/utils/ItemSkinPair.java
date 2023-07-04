package com.vergilprime.iaskins.utils;

import org.bukkit.inventory.ItemStack;

public class ItemSkinPair {
	public ItemStack item;
	public String skin;

	public ItemSkinPair(ItemStack item, String skin) {
		this.item = item;
		this.skin = skin;
	}

	public String getSkin() {
		return skin;
	}

	public ItemStack getItem() {
		return item;
	}
}

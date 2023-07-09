package com.vergilprime.iaskins.listeners;

import com.vergilprime.iaskins.IASkins;
import com.vergilprime.iaskins.controllers.SkinsController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryEvents implements Listener
{
	IASkins plugin;
	
	public InventoryEvents(IASkins plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onSmithItem(SmithItemEvent event)
	{
		SkinsController controller = plugin.skinsController;
		ItemStack inputStack = event.getInventory().getItem(1);
		ItemStack resultStack = event.getInventory().getResult();
		if(!controller.isSkinned(inputStack) || resultStack == null)
			return;
		String skin = controller.skinsReversed.get(controller.getSkinId(inputStack));
		String itemID = resultStack.getType().toString().toLowerCase();
		if(controller.skins.get(skin).containsKey(itemID))
			event.getInventory().setResult(controller.applySkin(resultStack, skin));
		else
		{
			plugin.skinsController.giveSkin((Player)event.getWhoClicked(), skin, true);
			controller.unskin(inputStack);
		}
	}
}

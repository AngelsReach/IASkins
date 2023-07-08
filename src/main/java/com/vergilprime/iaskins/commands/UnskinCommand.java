package com.vergilprime.iaskins.commands;

import com.vergilprime.iaskins.IASkins;
import org.bukkit.entity.Player;
import org.mineacademy.fo.command.SimpleCommand;

public final class UnskinCommand extends SimpleCommand {
	IASkins plugin;

	public UnskinCommand(IASkins plugin) {
		super("unskin");
		setDescription("Remove a skin from the item in your hand");
		setPermission("iaskins.commands.unskin");
		setUsage("/unskin");

		this.plugin = plugin;
	}

	@Override
	public void onCommand() {
		if (args.length > 0)
			returnTell("This command does not take any arguments");

		if (!hasPerm("iaskins.commands.unskin"))
			returnTell("You do not have permission to use this command");

		checkConsole();

		Player player = (Player) sender;
		boolean success = plugin.playerController.unskinMainHand(player);

		if (success) {
			returnTell("Successfully removed skin from item in hand");
		} else {
			returnTell("Failed to remove skin from item in hand");
		}
	}
}

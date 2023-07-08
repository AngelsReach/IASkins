package com.vergilprime.iaskins.commands;

import com.vergilprime.iaskins.IASkins;
import org.bukkit.entity.Player;
import org.mineacademy.fo.command.SimpleCommand;

public final class ApplySkinCommand extends SimpleCommand {
	IASkins plugin;

	public ApplySkinCommand(IASkins plugin) {
		super("applyskin/skin");
		setDescription("Apply a skin to the item in your hand");
		setPermission("iaskins.commands.applyskin");
		setUsage("/applyskin or /skin");

		this.plugin = plugin;
	}

	@Override
	public void onCommand() {
		if (args.length > 0)
			returnTell("This command does not take any arguments");

		if (!hasPerm("iaskins.commands.applyskin"))
			returnTell("You do not have permission to use this command");

		checkConsole();

		Player player = (Player) sender;
		boolean success = plugin.playerController.skinMainHand(player);

		if (success) {
			returnTell("Successfully applied skin to item in hand");
		} else {
			returnTell("Failed to apply skin to item in hand");
		}
	}
}

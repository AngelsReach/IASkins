package com.vergilprime.iaskins.commands;

import com.vergilprime.iaskins.IASkins;
import org.bukkit.entity.Player;
import org.mineacademy.fo.command.SimpleCommand;

public final class LostSkinsCommand extends SimpleCommand {
	IASkins plugin;

	public LostSkinsCommand(IASkins plugin) {
		super("lostskins");
		setDescription("Retrieve all skins that have been lost");
		setPermission("iaskins.commands.lostskins");
		setUsage("/lostskins");

		this.plugin = plugin;
	}

	@Override
	protected void onCommand() {
		if (args.length > 0)
			returnTell("This command does not take any arguments");

		if (!hasPerm("iaskins.commands.lostskins"))
			returnTell("You do not have permission to use this command");

		checkConsole();

		Player player = (Player) sender;
		boolean success = plugin.playerController.restoreSkins(player);

		if (success) {
			returnTell("Successfully restored lost skins");
		} else {
			returnTell("Failed to restore lost skins");
		}

	}
}

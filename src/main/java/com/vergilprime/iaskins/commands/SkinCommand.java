package com.vergilprime.iaskins.commands;

import com.vergilprime.iaskins.IASkins;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkinCommand implements CommandExecutor {
	IASkins plugin;

	public SkinCommand(IASkins plugin) {
		this.plugin = plugin;
		plugin.getCommand("skin").setExecutor(this);
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player) {

		} else {
			sender.sendMessage("You must be a player to use this command");
		}
		return false;
	}
}

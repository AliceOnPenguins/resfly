package com.alice.resfly;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class ResFlyCommand implements CommandExecutor {

  private final ResFlyPlugin plugin;

  public ResFlyCommand(ResFlyPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Tento příkaz může použít pouze hráč.");
      return true;
    }

    if (!player.hasPermission("resfly.command")) {
      player.sendMessage(ChatColor.RED + "Nemáš oprávnění k tomuto příkazu.");
      return true;
    }

    UUID uuid = player.getUniqueId();

    if (plugin.getToggledOn().contains(uuid)) {
      // turning off
      plugin.getToggledOn().remove(uuid);
      player.sendMessage(ChatColor.RED + "ResFly vypnuto.");

      if (plugin.getActiveFly().contains(uuid)) {
        ResFlyListener.disableFlight(plugin, player);
      }
    } else {
      // turning on
      plugin.getToggledOn().add(uuid);
      player.sendMessage(ChatColor.GREEN + "ResFly zapnuto.");

      // activate immediately if enabled and they are in a flyable res
      if (ResFlyListener.isInsideFlyableResidence(player)) {
        ResFlyListener.enableFlight(plugin, player);
      }
    }

    return true;
  }
}

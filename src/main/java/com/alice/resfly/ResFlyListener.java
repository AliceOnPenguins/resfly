package com.alice.resfly;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public final class ResFlyListener implements Listener {

  private final ResFlyPlugin plugin;

  public ResFlyListener(ResFlyPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMove(PlayerMoveEvent event) {
    // recheck only when playe moves to a new block for optimization
    if (event.getFrom().getBlockX() == event.getTo().getBlockX()
        && event.getFrom().getBlockY() == event.getTo().getBlockY()
        && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
      return;
    }
    refresh(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onTeleport(PlayerTeleportEvent event) {
    // run on next tick
    Bukkit.getScheduler().runTask(plugin, () -> refresh(event.getPlayer()));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {
    refresh(event.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    plugin.getActiveFly().remove(uuid);
    plugin.getFallImmune().remove(uuid);
  }

  // cancel fall damage
  @EventHandler(priority = EventPriority.HIGH)
  public void onEntityDamage(EntityDamageEvent event) {
    if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
      return;
    }
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }
    if (plugin.getFallImmune().remove(player.getUniqueId())) {
      event.setCancelled(true);
    }
  }

  private void refresh(Player player) {
    UUID uuid = player.getUniqueId();

    if (!plugin.getToggledOn().contains(uuid)) {
      if (plugin.getActiveFly().contains(uuid)) {
        disableFlight(plugin, player);
      }
      return;
    }

    boolean shouldFly = isInsideFlyableResidence(player);
    boolean isFlying = plugin.getActiveFly().contains(uuid);

    if (shouldFly && !isFlying) {
      enableFlight(plugin, player);
    } else if (!shouldFly && isFlying) {
      disableFlight(plugin, player);
    }
  }

  static boolean canFlyInResidence(Player player, ClaimedResidence residence) {
    if (residence == null) {
      return false;
    }
    if (player.hasPermission("resfly.all")) {
      return true;
    }
    return player.hasPermission("resfly.own") && isOwner(residence, player);
  }

  static boolean isOwner(ClaimedResidence residence, Player player) {
    if (residence == null) {
      return false;
    }
    String owner = residence.getOwner();
    if (owner == null) {
      return false;
    }
    return owner.equalsIgnoreCase(player.getName())
        || owner.equalsIgnoreCase(player.getUniqueId().toString());
  }

  static boolean isInsideFlyableResidence(Player player) {
    Location loc = player.getLocation();
    ClaimedResidence residence = Residence.getInstance().getResidenceManager().getByLoc(loc);
    return canFlyInResidence(player, residence);
  }

  static void enableFlight(ResFlyPlugin plugin, Player player) {
    UUID uuid = player.getUniqueId();
    if (plugin.getActiveFly().contains(uuid)) {
      return;
    }
    plugin.getActiveFly().add(uuid);
    player.setAllowFlight(true);
    player.setFlying(true);
  }

  static void disableFlight(ResFlyPlugin plugin, Player player) {
    UUID uuid = player.getUniqueId();
    plugin.getActiveFly().remove(uuid);

    boolean wasFlying = player.isFlying();
    player.setFlying(false);
    // only remove allowFlight if player isnt in gm c or gm sp
    if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
      player.setAllowFlight(false);
    }

    if (wasFlying) {
      // 1time fall dmg immunity
      plugin.getFallImmune().add(uuid);
      Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getFallImmune().remove(uuid), 20L * 10);
    }
  }
}

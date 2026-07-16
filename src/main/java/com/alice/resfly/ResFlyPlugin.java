package com.alice.resfly;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ResFlyPlugin extends JavaPlugin {

  // players that have /resfly toggled on
  private final Set<UUID> toggledOn = new HashSet<>();

  // players currently flying using resfly
  private final Set<UUID> activeFly = new HashSet<>();

  // fall damage cancel
  private final Set<UUID> fallImmune = new HashSet<>();

  @Override
  public void onEnable() {
    if (getServer().getPluginManager().getPlugin("Residence") == null) {
      getLogger().severe("Residence plugin nenalezen (ResFly dep)");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    getServer().getPluginManager().registerEvents(new ResFlyListener(this), this);
    getCommand("resfly").setExecutor(new ResFlyCommand(this));

    getLogger().info("ResFly zapnuto.");
  }

  @Override
  public void onDisable() {
    // clean up flight
    for (UUID uuid : new HashSet<>(activeFly)) {
      var player = getServer().getPlayer(uuid);
      if (player != null) {
        player.setAllowFlight(false);
        player.setFlying(false);
      }
    }
    toggledOn.clear();
    activeFly.clear();
    fallImmune.clear();
  }

  public Set<UUID> getToggledOn() {
    return toggledOn;
  }

  public Set<UUID> getActiveFly() {
    return activeFly;
  }

  public Set<UUID> getFallImmune() {
    return fallImmune;
  }
}

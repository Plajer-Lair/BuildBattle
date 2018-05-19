/*
 *  Village Defense 3 - Protect villagers from hordes of zombies
 * Copyright (C) 2018  Plajer's Lair - maintained by Plajer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.plajer.buildbattle3.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import pl.plajer.buildbattle3.Main;
import pl.plajer.buildbattle3.arena.Arena;
import pl.plajer.buildbattle3.arena.ArenaRegistry;
import pl.plajer.buildbattle3.handlers.ConfigurationManager;
import pl.plajer.buildbattle3.handlers.PermissionManager;
import pl.plajer.buildbattle3.plots.Plot;
import pl.plajer.buildbattle3.utils.SetupInventory;
import pl.plajer.buildbattle3.utils.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tom on 15/06/2015.
 */
public class SetupInventoryEvents implements Listener {

    private Main plugin;

    public SetupInventoryEvents(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if(event.getWhoClicked().getType() != EntityType.PLAYER)
            return;
        Player player = (Player) event.getWhoClicked();
        if(!player.hasPermission(PermissionManager.getEditGames()))
            return;
        if(!event.getInventory().getName().contains("Arena:"))
            return;
        if(event.getInventory().getHolder() != null)
            return;
        if(event.getCurrentItem() == null)
            return;
        if(!event.getCurrentItem().hasItemMeta())
            return;
        if(!event.getCurrentItem().getItemMeta().hasDisplayName())
            return;

        String name = event.getCurrentItem().getItemMeta().getDisplayName();
        name = ChatColor.stripColor(name);

        Arena arena = ArenaRegistry.getArena(event.getInventory().getName().replace("Arena: ", ""));
        if(arena == null) return;
        if(event.getCurrentItem().getType() == Material.NAME_TAG && event.getCursor().getType() == Material.NAME_TAG) {
            event.setCancelled(true);
            if(!event.getCursor().hasItemMeta()) {
                player.sendMessage("§cThis item doesn't has a name!");
                return;
            }
            if(!event.getCursor().getItemMeta().hasDisplayName()) {
                player.sendMessage("§cThis item doesn't has a name!");
                return;
            }

            player.performCommand("bb " + arena.getID() + " set MAPNAME " + event.getCursor().getItemMeta().getDisplayName());
            event.getCurrentItem().getItemMeta().setDisplayName("§6Set a mapname (currently: " + event.getCursor().getItemMeta().getDisplayName());
            return;
        }
        ClickType clickType = event.getClick();
        if(name.contains("ending location")) {
            event.setCancelled(true);
            player.closeInventory();
            player.performCommand("bb " + arena.getID() + " set ENDLOC");
            return;
        }
        if(name.contains("starting location")) {
            event.setCancelled(true);
            player.closeInventory();
            player.performCommand("bb " + arena.getID() + " set STARTLOC");
            return;
        }
        if(name.contains("lobby location")) {
            event.setCancelled(true);
            player.closeInventory();
            player.performCommand("bb " + arena.getID() + " set LOBBYLOC");
            return;
        }
        if(name.contains("maximum players")) {
            event.setCancelled(true);
            if(clickType.isRightClick()) {
                event.getCurrentItem().setAmount(event.getCurrentItem().getAmount() + 1);
                player.performCommand("bb " + arena.getID() + " set MAXPLAYERS " + event.getCurrentItem().getAmount());
            }
            if(clickType.isLeftClick()) {
                event.getCurrentItem().setAmount(event.getCurrentItem().getAmount() - 1);
                player.performCommand("bb " + arena.getID() + " set MAXPLAYERS " + event.getCurrentItem().getAmount());
            }
            player.closeInventory();
            player.openInventory(new SetupInventory(arena).getInventory());
        }

        if(name.contains("minimum players")) {
            event.setCancelled(true);
            if(clickType.isRightClick()) {
                event.getCurrentItem().setAmount(event.getCurrentItem().getAmount() + 1);
                player.performCommand("bb " + arena.getID() + " set MINPLAYERS " + event.getCurrentItem().getAmount());
            }
            if(clickType.isLeftClick()) {
                event.getCurrentItem().setAmount(event.getCurrentItem().getAmount() - 1);
                player.performCommand("bb " + arena.getID() + " set MINPLAYERS " + event.getCurrentItem().getAmount());
            }
            player.closeInventory();
            player.openInventory(new SetupInventory(arena).getInventory());
        }
        if(name.contains("Add game sign")) {
            event.setCancelled(true);
            plugin.getMainCommand().getAdminCommands().addSign(player, arena.getID());
            return;
        }
        if(event.getCurrentItem().getType() != Material.NAME_TAG) {
            event.setCancelled(true);
        }
        if(name.contains("Add game plot")) {
            player.performCommand("bb addplot " + arena.getID());
        }
        if(name.contains("Register arena")) {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
            if(arena.isReady()) {
                event.getWhoClicked().sendMessage("§aThis arena was already validated and is ready to use!");
                return;
            }
            String[] locations = new String[]{"lobbylocation", "Endlocation"};
            for(String s : locations) {
                if(!ConfigurationManager.getConfig("arenas").isSet("instances." + arena.getID() + "." + s) || ConfigurationManager.getConfig("arenas").getString("instances." + arena.getID() + "." + s).equals(Util.locationToString(Bukkit.getWorlds().get(0).getSpawnLocation()))) {
                    event.getWhoClicked().sendMessage("§cArena validation failed! Please configure following spawn properly: " + s + " (cannot be world spawn location)");
                    return;
                }
            }
            if(ConfigurationManager.getConfig("arenas").getConfigurationSection("instances." + arena.getID() + ".plots") == null) {
                event.getWhoClicked().sendMessage("§cArena validation failed! Please configure plots properly");
                return;
            } else {
                for(String plotName : ConfigurationManager.getConfig("arenas").getConfigurationSection(arena.getID() + "plots").getKeys(false)) {
                    if(ConfigurationManager.getConfig("arenas").isSet(arena.getID() + "plots." + plotName + ".maxpoint") && ConfigurationManager.getConfig("arenas").isSet(arena.getID() + "plots." + plotName + ".minpoint")) {
                        Plot buildPlot = new Plot();
                        buildPlot.setMaxPoint(Util.getLocation(false, ConfigurationManager.getConfig("arenas").getString(arena.getID() + "plots." + plotName + ".maxpoint")));
                        buildPlot.setMinPoint(Util.getLocation(false, ConfigurationManager.getConfig("arenas").getString(arena.getID() + "plots." + plotName + ".minpoint")));
                        buildPlot.reset();
                        arena.getPlotManager().addBuildPlot(buildPlot);
                    } else {
                        event.getWhoClicked().sendMessage("§cArena validation failed! Plots are not configured properly! (missing selection values)");
                        return;
                    }
                }
            }
            event.getWhoClicked().sendMessage("§aValidation succeeded! Registering new arena instance: " + arena.getID());
            FileConfiguration config = ConfigurationManager.getConfig("arenas");
            config.set("instances." + arena.getID() + ".isdone", true);
            ConfigurationManager.saveConfig(config, "arenas");
            List<Sign> signsToUpdate = new ArrayList<>();
            ArenaRegistry.unregisterArena(arena);
            if(plugin.getSignManager().getLoadedSigns().containsValue(arena)) {
                for(Sign s : plugin.getSignManager().getLoadedSigns().keySet()) {
                    if(plugin.getSignManager().getLoadedSigns().get(s).equals(arena)) {
                        signsToUpdate.add(s);
                    }
                }
            }
            arena = new Arena(arena.getID());
            arena.setReady(true);
            arena.setMinimumPlayers(ConfigurationManager.getConfig("arenas").getInt("instances." + arena.getID() + ".minimumplayers"));
            arena.setMaximumPlayers(ConfigurationManager.getConfig("arenas").getInt("instances." + arena.getID() + ".maximumplayers"));
            arena.setMapName(ConfigurationManager.getConfig("arenas").getString("instances." + arena.getID() + ".mapname"));
            arena.setLobbyLocation(Util.getLocation(false, ConfigurationManager.getConfig("arenas").getString("instances." + arena.getID() + ".lobbylocation")));
            arena.setEndLocation(Util.getLocation(false, ConfigurationManager.getConfig("arenas").getString("instances." + arena.getID() + ".Endlocation")));

            for(String plotName : config.getConfigurationSection(arena.getID() + "plots").getKeys(false)) {
                Plot buildPlot = new Plot();
                buildPlot.setMaxPoint(Util.getLocation(false, config.getString(arena.getID() + "plots." + plotName + ".maxpoint")));
                buildPlot.setMinPoint(Util.getLocation(false, config.getString(arena.getID() + "plots." + plotName + ".minpoint")));
                buildPlot.reset();
                arena.getPlotManager().addBuildPlot(buildPlot);
            }
            ArenaRegistry.registerArena(arena);
            arena.start();
            for(Sign s : signsToUpdate) {
                plugin.getSignManager().getLoadedSigns().put(s, arena);
            }
        }
    }

}

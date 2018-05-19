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

package pl.plajer.buildbattle3.commands;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import pl.plajer.buildbattle3.ConfigPreferences;
import pl.plajer.buildbattle3.Main;
import pl.plajer.buildbattle3.arena.Arena;
import pl.plajer.buildbattle3.arena.ArenaRegistry;
import pl.plajer.buildbattle3.arena.ArenaState;
import pl.plajer.buildbattle3.handlers.ChatManager;
import pl.plajer.buildbattle3.handlers.ConfigurationManager;
import pl.plajer.buildbattle3.utils.Util;

import java.util.List;

/**
 * @author Plajer
 * <p>
 * Created at 03.05.2018
 */
public class AdminCommands extends MainCommand {

    private Main plugin;

    public AdminCommands(Main plugin) {
        this.plugin = plugin;
    }

    public void addPlot(Player player, String arena) {
        if(ArenaRegistry.getArena(arena) == null) {
            player.sendMessage(ChatManager.PREFIX + ChatManager.colorMessage("Commands.No-Arena-Like-That"));
            return;
        }
        Selection selection = plugin.getWorldEditPlugin().getSelection(player);
        if(selection instanceof CuboidSelection) {
            FileConfiguration config = ConfigurationManager.getConfig("arenas");
            if(config.contains("instances." + arena + ".plots")) {
                Util.saveLocation("instances." + arena + ".plots." + (config.getConfigurationSection("instances." + arena + ".plots").getKeys(false).size() + 1) + ".minpoint", selection.getMinimumPoint());
                Util.saveLocation("instances." + arena + ".plots." + (config.getConfigurationSection("instances." + arena + ".plots").getKeys(false).size()) + ".maxpoint", selection.getMaximumPoint());
            } else {
                Util.saveLocation("instances." + arena + ".plots.0.minpoint", selection.getMinimumPoint());
                Util.saveLocation("instances." + arena + ".plots.0.maxpoint", selection.getMaximumPoint());
            }
            ConfigurationManager.saveConfig(config, "arenas");
            player.sendMessage("§aPlot added to instance§c " + arena);
        } else {
            player.sendMessage("§cYou don't have the right selection!");
        }
    }

    public void forceStart(Player player) {
        Arena arena = ArenaRegistry.getArena(player);
        if(arena == null) return;
        if(arena.getGameState() == ArenaState.WAITING_FOR_PLAYERS || arena.getGameState() == ArenaState.STARTING) {
            arena.setGameState(ArenaState.STARTING);
            arena.setTimer(0);
            for(Player p : arena.getPlayers()) {
                p.sendMessage(ChatManager.PREFIX + ChatManager.colorMessage("In-Game.Messages.Admin-Messages.Set-Starting-In-To-0"));
            }
        }
    }

    public void reloadPlugin(Player player) {
        ConfigPreferences.loadOptions();
        ConfigPreferences.loadOptions();
        ConfigPreferences.loadThemes();
        ConfigPreferences.loadBlackList();
        ConfigPreferences.loadWinCommands();
        ConfigPreferences.loadSecondPlaceCommands();
        ConfigPreferences.loadThirdPlaceCommands();
        ConfigPreferences.loadEndGameCommands();
        ConfigPreferences.loadWhitelistedCommands();
        plugin.loadArenas();
        player.sendMessage("§aPlugin reloaded!");
    }

    public void addSign(Player player, String arenaName) {
        Arena arena = ArenaRegistry.getArena(arenaName);
        if(arena == null) {
            player.sendMessage(ChatManager.PREFIX + ChatManager.colorMessage("Commands.No-Arena-Like-That"));
        } else {
            Location loc = player.getTargetBlock(null, 10).getLocation();
            if(loc.getBlock().getState() instanceof Sign) {
                FileConfiguration config = ConfigurationManager.getConfig("arenas");
                List<String> signs = config.getStringList("instances." + arena.getID() + ".signs");
                signs.add(loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch());
                config.set("instances." + arena.getID() + ".signs", signs);
                ConfigurationManager.saveConfig(config, "arenas");
                plugin.getSignManager().getLoadedSigns().put((Sign) loc.getBlock().getState(), arena);
                player.sendMessage(ChatManager.PREFIX + ChatManager.colorMessage("Signs.Sign-Created"));
            } else {
                player.sendMessage("§cYou have to look at a sign to perform this command!");
            }

        }
    }

}

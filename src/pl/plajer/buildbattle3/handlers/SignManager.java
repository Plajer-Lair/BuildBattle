/*
 * BuildBattle 3 - Ultimate building competition minigame
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

package pl.plajer.buildbattle3.handlers;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import pl.plajer.buildbattle3.Main;
import pl.plajer.buildbattle3.arena.Arena;
import pl.plajer.buildbattle3.arena.ArenaManager;
import pl.plajer.buildbattle3.arena.ArenaRegistry;
import pl.plajer.buildbattle3.arena.ArenaState;
import pl.plajer.buildbattle3.handlers.language.LanguageManager;
import pl.plajer.buildbattle3.utils.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Plajer
 * <p>
 * Created at 04.05.2018
 */
public class SignManager implements Listener {

    private Main plugin;
    private Map<Sign, Arena> loadedSigns = new HashMap<>();
    private Map<ArenaState, String> gameStateToString = new HashMap<>();
    private List<String> signLines;

    public SignManager(Main plugin) {
        this.plugin = plugin;
        gameStateToString.put(ArenaState.WAITING_FOR_PLAYERS, ChatManager.colorMessage("Signs.Game-States.Inactive"));
        gameStateToString.put(ArenaState.STARTING, ChatManager.colorMessage("Signs.Game-States.Starting"));
        gameStateToString.put(ArenaState.IN_GAME, ChatManager.colorMessage("Signs.Game-States.In-Game"));
        gameStateToString.put(ArenaState.ENDING, ChatManager.colorMessage("Signs.Game-States.Ending"));
        gameStateToString.put(ArenaState.RESTARTING, ChatManager.colorMessage("Signs.Game-States.Restarting"));
        signLines = LanguageManager.getLocaleFile().getStringList("Signs.Lines");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadSigns();
        updateSignScheduler();
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        if(!e.getPlayer().hasPermission("buildbattle.admin.sign.create")) return;
        if(!e.getLine(0).equalsIgnoreCase("[buildbattle]")) return;
        if(e.getLine(1).isEmpty()) {
            e.getPlayer().sendMessage(ChatManager.PLUGIN_PREFIX + ChatManager.colorMessage("Signs.Please-Type-Arena-Name"));
            return;
        }
        for(Arena arena : ArenaRegistry.getArenas()) {
            if(arena.getID().equalsIgnoreCase(e.getLine(1))) {
                for(int i = 0; i < signLines.size(); i++) {
                    e.setLine(i, formatSign(signLines.get(i), arena));
                }
                loadedSigns.put((Sign) e.getBlock().getState(), arena);
                e.getPlayer().sendMessage(ChatManager.PLUGIN_PREFIX + ChatManager.colorMessage("Signs.Sign-Created"));
                String location = e.getBlock().getWorld().getName() + "," + e.getBlock().getX() + "," + e.getBlock().getY() + "," + e.getBlock().getZ() + ",0.0,0.0";
                List<String> locs = ConfigurationManager.getConfig("arenas").getStringList("instances." + arena.getID() + ".signs");
                locs.add(location);
                FileConfiguration config = ConfigurationManager.getConfig("arenas");
                config.set("instances." + arena.getID() + ".signs", locs);
                ConfigurationManager.saveConfig(config, "arenas");
                return;
            }
        }
        e.getPlayer().sendMessage(ChatManager.PLUGIN_PREFIX + ChatManager.colorMessage("Signs.Arena-Doesnt-Exists"));
    }

    private String formatSign(String msg, Arena a) {
        String formatted = msg;
        formatted = StringUtils.replace(formatted, "%mapname%", a.getMapName());
        if(a.getPlayers().size() >= a.getMaximumPlayers()) {
            formatted = StringUtils.replace(formatted, "%state%", ChatManager.colorMessage("Signs.Game-States.Full-Game"));
        } else {
            formatted = StringUtils.replace(formatted, "%state%", gameStateToString.get(a.getArenaState()));
        }
        formatted = StringUtils.replace(formatted, "%playersize%", String.valueOf(a.getPlayers().size()));
        formatted = StringUtils.replace(formatted, "%maxplayers%", String.valueOf(a.getMaximumPlayers()));
        formatted = ChatManager.colorRawMessage(formatted);
        return formatted;
    }

    @EventHandler
    public void onSignDestroy(BlockBreakEvent e) {
        if(!e.getPlayer().hasPermission("buildbattle.admin.sign.break")) return;
        if(loadedSigns.get(e.getBlock().getState()) == null) return;
        loadedSigns.remove(e.getBlock().getState());
        String location = e.getBlock().getWorld().getName() + "," + e.getBlock().getX() + ".0," + e.getBlock().getY() + ".0," + e.getBlock().getZ() + ".0," + "0.0,0.0";
        for(String arena : ConfigurationManager.getConfig("arenas").getConfigurationSection("instances").getKeys(false)) {
            for(String sign : ConfigurationManager.getConfig("arenas").getStringList("instances." + arena + ".signs")) {
                if(sign.equals(location)) {
                    List<String> signs = ConfigurationManager.getConfig("arenas").getStringList("instances." + arena + ".signs");
                    signs.remove(location);
                    FileConfiguration config = ConfigurationManager.getConfig("arenas");
                    config.set(arena + ".signs", signs);
                    ConfigurationManager.saveConfig(config, "arenas");
                    e.getPlayer().sendMessage(ChatManager.PLUGIN_PREFIX + ChatManager.colorMessage("Signs.Sign-Removed"));
                    return;
                }
            }
        }
        e.getPlayer().sendMessage(ChatManager.PLUGIN_PREFIX + ChatColor.RED + "Couldn't remove sign from configuration! Please do this manually!");
    }

    @EventHandler
    public void onJoinAttempt(PlayerInteractEvent e) {
        if(e.getHand() == EquipmentSlot.OFF_HAND) return;
        if(e.getAction() == Action.RIGHT_CLICK_BLOCK &&
                e.getClickedBlock().getState() instanceof Sign && loadedSigns.containsKey(e.getClickedBlock().getState())) {
            ArenaManager.joinAttempt(e.getPlayer(), loadedSigns.get(e.getClickedBlock().getState()));
        }
    }

    public void loadSigns() {
        loadedSigns.clear();
        for(String path : ConfigurationManager.getConfig("arenas").getConfigurationSection("instances").getKeys(false)) {
            for(String sign : ConfigurationManager.getConfig("arenas").getStringList("instances." + path + ".signs")) {
                Location loc = Util.getLocation(false, sign);
                if(loc.getBlock().getState() instanceof Sign) {
                    loadedSigns.put((Sign) loc.getBlock().getState(), ArenaRegistry.getArena(path));
                } else {
                    Main.debug("Block at loc " + loc + " for arena " + path + " not a sign", System.currentTimeMillis());
                }
            }
        }
    }

    private void updateSignScheduler() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for(Sign s : loadedSigns.keySet()) {
                for(int i = 0; i < signLines.size(); i++) {
                    s.setLine(i, formatSign(signLines.get(i), loadedSigns.get(s)));
                    if(plugin.getConfig().getBoolean("Signs-Block-States.Enabled")) {
                        if(s.getType() == Material.SIGN_POST || s.getType() == Material.WALL_SIGN) {
                            Block behind = s.getBlock().getRelative(((org.bukkit.material.Sign) s.getData()).getAttachedFace());
                            if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("glass")) {
                                behind.setType(Material.STAINED_GLASS);
                            }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("clay")) {
                                behind.setType(Material.STAINED_CLAY);
                            }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("wool")) {
                                behind.setType(Material.WOOL);
                            }
                            switch(loadedSigns.get(s).getArenaState()) {
                                case WAITING_FOR_PLAYERS:
                                    if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("glass")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Waiting-For-Players.Glass")));
                                    }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("clay")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Waiting-For-Players.Clay")));
                                    }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("wool")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Waiting-For-Players.Wool")));
                                    }
                                    break;
                                case STARTING:
                                    if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("glass")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Starting.Glass")));
                                    }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("clay")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Starting.Clay")));
                                    }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("wool")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Starting.Wool")));
                                    }
                                    break;
                                case IN_GAME:
                                    if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("glass")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.In-Game.Glass")));
                                    }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("clay")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.In-Game.Clay")));
                                    }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("wool")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.In-Game.Wool")));
                                    }
                                    break;
                                case ENDING:
                                    if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("glass")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Ending.Glass")));
                                    }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("clay")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Ending.Clay")));
                                    }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("wool")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Ending.Wool")));
                                    }
                                    break;
                                case RESTARTING:
                                    if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("glass")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Restarting.Glass")));
                                    }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("clay")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Restarting.Clay")));
                                    }else if(plugin.getConfig().getString("Signs-Block-States.Block-Type").equalsIgnoreCase("wool")) {
                                        behind.setData((byte) plugin.getConfig().getInt("Signs-Block-States.Colors.Restarting.Wool")));
                                    }
                                    break;
                            }
                        }
                    }
                }
                s.update();
            }
        }, 10, 10);
    }

    public Map<Sign, Arena> getLoadedSigns() {
        return loadedSigns;
    }
}

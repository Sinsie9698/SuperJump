/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.game.signs;

import com.spleefleague.core.io.typeconverters.LocationConverter;
import com.spleefleague.entitybuilder.DBEntity;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBLoadable;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.AbstractBattle;
import com.spleefleague.superjump.game.Arena;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

/**
 *
 * @author Jonas
 */
public class GameSign extends DBEntity implements DBLoadable {

    private Location location;
    private Arena arena;

    @DBLoad(fieldName = "location", typeConverter = LocationConverter.class)
    private void setSign(Location location) {
        this.location = location;
    }

    @DBLoad(fieldName = "arena")
    private void setArena(String name) {
        arena = Arena.byName(name);
        if (signs.get(arena) == null) {
            signs.put(arena, new HashSet<>());
        }
        signs.get(arena).add(this);
    }

    public Sign getSign() {
        BlockState bs = location.getBlock().getState();
        if (bs instanceof Sign) {
            return (Sign) bs;
        } else {
            return null;
        }
    }

    public Location getLocation() {
        return location;
    }

    public Arena getArena() {
        return arena;
    }

    public AbstractBattle getBattle() {
        return SuperJump.getInstance().getBattleManager().getBattle(arena);
    }

    public void updateStatus() {
        Sign sign = getSign();
        if (sign != null) {
            sign.setLine(0, "[" + (arena.isPaused() ? ChatColor.DARK_RED + "Closed" : (arena.isOccupied() ? ChatColor.YELLOW + "Occupied" : ChatColor.GREEN + "Free")) + ChatColor.RESET + "]");
            sign.setLine(1, getArena().getName());
            if (!arena.isPaused()) {
                sign.setLine(2, ChatColor.ITALIC + "Click here to");
                sign.setLine(3, ChatColor.ITALIC + "enter the queue");
            }
            sign.update();
        }
    }

    private static Map<Arena, HashSet<GameSign>> signs;

    public static void initialize() {
        signs = new HashMap<>();
        for (Document document : SuperJump.getInstance().getPluginDB().getCollection("GameSigns").find()) {
            EntityBuilder.load(document, GameSign.class).updateStatus();
        }
    }

    public static HashSet<GameSign> getGameSigns(Arena arena) {
        HashSet<GameSign> set = signs.get(arena);
        return set != null ? set : new HashSet<>();
    }

    public static void updateGameSigns(Arena arena) {
        for (GameSign gs : getGameSigns(arena)) {
            gs.updateStatus();
        }
    }

    public static void updateGameSigns() {
        for (Arena arena : signs.keySet()) {
            updateGameSigns(arena);
        }
    }

    public static HashSet<GameSign> getAll() {
        HashSet<GameSign> all = new HashSet<>();
        for (HashSet<GameSign> gs : signs.values()) {
            all.addAll(gs);
        }
        return all;
    }
}

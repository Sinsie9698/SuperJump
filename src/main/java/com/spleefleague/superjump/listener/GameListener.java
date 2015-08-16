/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.listener;


import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.utils.PlayerUtil;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.Arena;
import com.spleefleague.superjump.game.Battle;
import com.spleefleague.superjump.player.SJPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 *
 * @author Jonas
 */
public class GameListener implements Listener{
    
    private static Listener instance;
    
    public static void init() {
        if(instance == null) {
            instance = new GameListener();
            Bukkit.getPluginManager().registerEvents(instance, SuperJump.getInstance());
        }
    }
    
    private GameListener() {
        
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(event.getPlayer());
        if(sjp.isFrozen()) {
            Location from = event.getFrom();
            Location to = event.getTo();
            from.setY(to.getY());
            from.setYaw(to.getYaw());
            from.setPitch(to.getPitch());
            event.setTo(from);
        }
        else if (!sjp.isIngame()) {
            if(!SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer()).getRank().hasPermission(Rank.MODERATOR)) {
                for(Arena arena : Arena.getAll()) {
                    if(arena.isTpBackSpectators() && arena.getBorder().isInArea(sjp.getPlayer().getLocation())) {
                        Location loc = arena.getSpectatorSpawn();
                        if(loc == null) {
                            loc = SpleefLeague.getInstance().getSpawnLocation();
                        }
                        sjp.getPlayer().teleport(loc);
                        break;
                    }
                }
            }
        }
        else {
            Battle battle = SuperJump.getInstance().getBattleManager().getBattle(sjp);
            Arena arena = battle.getArena();
            if(!arena.getBorder().isInArea(sjp.getPlayer().getLocation())) {
                battle.onArenaLeave(sjp);
            }
            else if(arena.isLiquidLose() && (PlayerUtil.isInLava(event.getPlayer()) || PlayerUtil.isInWater(event.getPlayer()))) {
                battle.onArenaLeave(sjp);
            }
            else if(battle.getGoal(sjp).isInArea(sjp.getPlayer().getLocation())) {
                battle.end(sjp);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(event.getPlayer());
        if(sjp.isIngame()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(event.getPlayer());
        if(sjp.isIngame()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(event.getPlayer());
        if(sjp.isIngame()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onDamage(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }
}
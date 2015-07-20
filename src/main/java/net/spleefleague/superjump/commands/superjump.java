/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.commands;

import net.spleefleague.core.command.BasicCommand;
import net.spleefleague.core.io.EntityBuilder;
import net.spleefleague.core.player.SLPlayer;
import net.spleefleague.core.plugin.CorePlugin;
import net.spleefleague.core.plugin.GamePlugin;
import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.game.Arena;
import net.spleefleague.superjump.game.BattleManager;
import net.spleefleague.superjump.game.signs.GameSign;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

/**
 *
 * @author Jonas
 */
public class superjump extends BasicCommand {

    public superjump(CorePlugin plugin, String name, String usage) {
        super(SuperJump.getInstance(), name, usage);
    }

    @Override
    protected void run(Player p, SLPlayer slp, Command cmd, String[] args) {
        if (SuperJump.getInstance().queuesOpen()) {
            SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(p);
            BattleManager bm = SuperJump.getInstance().getBattleManager();
            if (!GamePlugin.isIngameGlobal(p)) {
                if (args.length == 0) {
                    GamePlugin.dequeueGlobal(p);
                    bm.queue(sjp);
                    success(p, "You have been added to the queue.");
                }
                else if (args.length == 1) {
                    Arena arena = Arena.byName(args[0]);
                    if (arena != null) {
                        if (!arena.isPaused()) {
                            if (sjp.getVisitedArenas().contains(arena)) {
                                bm.queue(sjp, arena);
                                success(p, "You have been added to the queue for: " + ChatColor.GREEN + arena.getName());
                            }
                            else {
                                error(p, "You have not visited this arena yet!");
                            }
                        }
                        else {
                            error(p, "This arena is currently paused.");
                        }
                    }
                    else {
                        error(p, "This arena does not exist.");
                    }
                }
                else if (args.length == 2) {
                    Arena arena = Arena.byName(args[1]);
                    if (arena != null) {
                        if (args[0].equalsIgnoreCase("pause")) {
                            arena.setPaused(true);
                            success(p, "You have paused the arena " + arena.getName());
                        }
                        else if (args[0].equalsIgnoreCase("unpause")) {
                            arena.setPaused(false);
                            success(p, "You have unpaused the arena " + arena.getName());
                        }
                        GameSign.updateGameSigns(arena);
                        EntityBuilder.save(arena, SuperJump.getInstance().getPluginDB().getCollection("Arenas"));
                    }
                    else {
                        error(p, "This arena does not exist.");
                    }
                }
                else {
                    sendUsage(p);
                }
            }
            else {
                error(p, "You are currently ingame!");
            }
        }
        else {
            error(p, "All queues are currently paused!");
        }
    }
}

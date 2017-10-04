/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.commands;

import static com.spleefleague.annotations.CommandSource.COMMAND_BLOCK;
import static com.spleefleague.annotations.CommandSource.CONSOLE;
import static com.spleefleague.annotations.CommandSource.PLAYER;
import com.spleefleague.annotations.Endpoint;
import com.spleefleague.annotations.LiteralArg;
import com.spleefleague.annotations.PlayerArg;
import com.spleefleague.annotations.StringArg;
import com.spleefleague.commands.command.BasicCommand;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.events.BattleStartEvent.StartReason;
import com.spleefleague.core.player.PlayerManager;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.plugin.CorePlugin;
import com.spleefleague.core.plugin.GamePlugin;
import com.spleefleague.core.queue.Challenge;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.Arena;
import com.spleefleague.superjump.player.SJPlayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 *
 * @author Jonas
 */
public class superjump extends BasicCommand {

    public superjump(CorePlugin plugin, String name, String usage) {
        super(SuperJump.getInstance(), new superjumpDispatcher(), name, usage);
    }

    private boolean checkQueuesClosed(Player p) {
        if (!SuperJump.getInstance().queuesOpen()) {
            error(p, "All queues are currently paused!");
            return true;
        }
        return false;
    }

    private boolean checkIngame(Player p) {
        if (GamePlugin.isIngameGlobal(p)) {
            error(p, "You are currently ingame!");
            return true;
        }
        return false;
    }

    @Endpoint(target = {PLAYER})
    public void queueGlobally(Player sender) {
        if (checkQueuesClosed(sender)) {
            return;
        }
        if (checkIngame(sender)) {
            return;
        }
        GamePlugin.dequeueGlobal(sender);
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(sender);
        SuperJump.getInstance().getBattleManager().queue(sjp);
        success(sender, "You have been added to the queue");
    }

    @Endpoint(target = {PLAYER})
    public void queueArena(Player p, @StringArg String arenaName) {
        if (checkQueuesClosed(p)) {
            return;
        }
        if (checkIngame(p)) {
            return;
        }
        Arena arena = Arena.byName(arenaName);
        if (arena == null) {
            error(p, "This arena does not exist.");
            return;
        }
        if (arena.isPaused()) {
            error(p, "This arena is currently paused.");
            return;
        }
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(p);
        if (!arena.isAvailable(sjp)) {
            error(p, "You have not visited this arena yet!");
            return;
        }
        SuperJump.getInstance().getBattleManager().queue(sjp, arena);
        success(p, "You have been added to the queue for: " + ChatColor.GREEN + arena.getName());
    }

    @Endpoint(target = {PLAYER, CONSOLE, COMMAND_BLOCK})
    public void forcestart(SLPlayer sender, @LiteralArg(value = "match") String l, @StringArg String arenaName, @PlayerArg Player[] players) {
        if (checkIngame(sender)) {
            return;
        }
        if (!sender.getRank().hasPermission(Rank.MODERATOR) && sender.getRank() != Rank.ORGANIZER) {
            sendUsage(sender);
            return;
        }
        Arena arena = Arena.byName(arenaName);
        if (arena == null) {
            error(sender, "This arena does not exist.");
            return;
        }
        if (arena.isOccupied()) {
            error(sender, "This arena is currently occupied.");
            return;
        }
        if (arena.getRequiredPlayers() > players.length || arena.getSize() < players.length) {
            if (arena.getRequiredPlayers() == arena.getSize()) {
                error(sender, "This arena requires " + arena.getSize() + " players.");
            } else {
                error(sender, "This arena requires between " + arena.getRequiredPlayers() + " and " + arena.getSize() + " players");
            }
            return;
        }
        PlayerManager<SJPlayer> pm = SuperJump.getInstance().getPlayerManager();
        List<SJPlayer> sjplayers = Arrays.stream(players)
                .map(pm::get)
                .collect(Collectors.toList());
        arena.startBattle(sjplayers, StartReason.FORCE);
        success(sender, "You started a battle on the arena " + arena.getName());
    }

    @Endpoint(target = {PLAYER})
    public void challenge(SLPlayer sender, @LiteralArg(value = "challenge", aliases = {"c"}) String l, @StringArg String arenaName, @PlayerArg Player[] players) {
        handleChallenge(sender, false, arenaName, players);
    }

    @Endpoint(target = {PLAYER})
    public void multichallenge(SLPlayer sender, @LiteralArg(value = "multichallenge", aliases = {"mc"}) String l, @StringArg String arenaName, @PlayerArg Player[] players) {
        handleChallenge(sender, false, arenaName, players);
    }

    public void handleChallenge(SLPlayer sender, boolean isMulti, String arenaName, Player[] players) {
        if (checkQueuesClosed(sender)) {
            return;
        }
        if (checkIngame(sender)) {
            return;
        }
        Arena arena = Arena.byName(arenaName);
        if (arena == null) {
            error(sender, "This arena does not exist.");
            return;
        }
        if (arena.isPaused()) {
            error(sender, "This arena is currently paused.");
            return;
        }
        if (arena.isOccupied()) {
            error(sender, "This arena is currently occupied.");
            return;
        }
        if (arena.getRequiredPlayers() - 1 > players.length || arena.getSize() - 1 < players.length) {
            if (arena.getRequiredPlayers() == arena.getSize()) {
                error(sender, "This arena requires " + arena.getSize() + " players.");
            } else {
                error(sender, "This arena requires between " + arena.getRequiredPlayers() + " and " + arena.getSize() + " players");
            }
            return;
        }
        PlayerManager<SJPlayer> pm = SuperJump.getInstance().getPlayerManager();
        SJPlayer sendersjp = pm.get(sender);
        if (sender.getState() == PlayerState.INGAME) {
            error(sender, "You are currently ingame.");
            return;
        }
        if (!arena.isAvailable(sendersjp)) {
            error(sender, "You have not discovered this arena yet.");
        }
        List<SLPlayer> challenged = new ArrayList<>();
        for (Player player : players) {
            SJPlayer sjp = pm.get(player);
            SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(player);
            if (sjp == sendersjp) {
                error(sender, "You cannot challenge yourself.");
                return;
            }
            if (challenged.contains(slp)) {
                error(sender, "You cannot challenge " + sjp.getName() + " more than once.");
                return;
            }
            if (slp.getState() == PlayerState.INGAME) {
                error(sender, player.getName() + " is currently ingame.");
                return;
            }
            if (!arena.isAvailable(sjp)) {
                error(sender, player.getName() + " has not discovered this arena yet.");
            }
            challenged.add(slp);
        }
        Challenge challenge = new Challenge(sender, challenged.toArray(new SLPlayer[0])) {
            @Override
            public void start(SLPlayer[] accepted) {
                List<SJPlayer> players = new ArrayList<>();
                for (SLPlayer slpt : accepted) {
                    players.add(SuperJump.getInstance().getPlayerManager().get(slpt));
                }
                if (isMulti) {
                    arena.startMultiBattle(players, StartReason.CHALLENGE);
                } else {
                    arena.startBattle(players, StartReason.CHALLENGE);
                }
            }
        };
        challenged.forEach((slpt) -> {
            slpt.addChallenge(challenge);
        });
        challenge.sendMessages(SuperJump.getInstance().getChatPrefix(), arena.getName(), Arrays.asList(players));
        success(sender, "The players have been challenged.");
    }

    @Endpoint(target = {PLAYER, CONSOLE})
    public void pause(SLPlayer sender, @LiteralArg(value = "pause") String l, @StringArg String arenaName) {
        handlePause(sender, true, arenaName);
    }

    @Endpoint(target = {PLAYER, CONSOLE})
    public void unpause(SLPlayer sender, @LiteralArg(value = "unpause") String l, @StringArg String arenaName) {
        handlePause(sender, false, arenaName);
    }

    private void handlePause(SLPlayer sender, boolean pauseValue, @StringArg String arenaName) {
        if (!sender.getRank().hasPermission(Rank.MODERATOR) && sender.getRank() != Rank.ORGANIZER) {
            sendUsage(sender);
            return;
        }
        Arena arena = Arena.byName(arenaName);
        if (arena == null) {
            error(sender, "This arena does not exist.");
            return;
        }
        arena.setPaused(pauseValue);
        if (pauseValue) {
            success(sender, "You have paused the arena " + arena.getName());
        } else {
            success(sender, "You have unpaused the arena " + arena.getName());
        }
    }
}

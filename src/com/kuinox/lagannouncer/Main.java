package com.kuinox.lagannouncer;

import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main extends JavaPlugin {

    Timer timer;

    @Override
    public void onEnable() {
        getLogger().info("Enabled");
        timer = new Timer(getServer());
        getServer().getScheduler().scheduleSyncRepeatingTask(this, timer, 0L, 1L);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled");
    }

}

class PlayerAlerter implements Listener {
    private static DecimalFormat df2 = new DecimalFormat("#.##");
    private Server server;
    private Timer timer;
    PlayerAlerter(Server server, Timer timer) {
        this.server = server;
        this.timer = timer;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if(timer.isServerLagging) {
            server.broadcastMessage("Hello "+e.getPlayer().getDisplayName()+"! The server is currently overloaded ! Tickrate: "+df2.format(timer.tickrate)+" tps.");
        }
    }
}


class Timer implements Runnable {

    private static DecimalFormat df2 = new DecimalFormat("#.##");
    @NotNull
    private long previous;
    @NotNull
    private int[] smallHistory;
    @NotNull
    private short smallHistoryIndex;
    @NotNull
    private Server server;
    @NotNull
    boolean isServerLagging;
    @NotNull
    double tickrate;
    Timer(Server server ) {
        previous = System.nanoTime();
        smallHistory = new int[1000];
        smallHistoryIndex = 0;
        this.server = server;
        isServerLagging = false;
        tickrate = 0;
    }


    @Override
    public void run() {
        final long startTime = System.nanoTime();
        final int elapsed = (int) TimeUnit.NANOSECONDS.toMillis(startTime-previous);
        previous = startTime;
        smallHistory[smallHistoryIndex++] = elapsed;
        if(smallHistoryIndex == 1000) {
            smallHistoryIndex = 0;
            checkServerOverloaded();
        }

        if(elapsed>200) {
            computeTickrate();
            server.broadcastMessage("Server lag spike: tick took "+df2.format(elapsed)+"ms instead of 20ms. (Current average tickrate: "+df2.format(tickrate)+"tps");
        }
    }

    private void computeTickrate() {
        OptionalDouble temp = Arrays.stream(smallHistory).filter(value -> value!=0).average();
        if(!temp.isPresent()) {
            server.getLogger().warning("Kuinox don't know how to do java and the lag reporting errored... Exiting safely, but ask me to fix this...");
        }
        double average = temp.getAsDouble();
        tickrate = 1000/average;
    }

    private void checkServerOverloaded(){
        computeTickrate();
        server.broadcastMessage(tickrate+"tps"+" "+(tickrate<15 && !isServerLagging));
        if(tickrate<15 && !isServerLagging) {
            isServerLagging = true;
            server.broadcastMessage("The server is overloaded ! Pay attention! Current tickrate is "+df2.format(tickrate)+"tps instead of 20 tps");
        }

        if(isServerLagging && tickrate>18) {
            isServerLagging = false;
            server.broadcastMessage("The server is not overloaded anymore ! Enjoy! Current tickrate is "+df2.format(tickrate)+"tps (target: 20 tps)");
        }
    }
}

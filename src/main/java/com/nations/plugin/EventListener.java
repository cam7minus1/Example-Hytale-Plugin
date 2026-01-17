package com.nations.plugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.nations.plugin.NationsPlugin;

public class EventListener {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void register(NationsPlugin plugin) {
        // Hook into block breaking events
        plugin.getEventRegistry().register(BreakBlockEvent.class, event -> {
            LOGGER.atInfo().log("Block broken at: " + event.getTargetBlock().toString());

             // Example: To cancel the event (if supported by the specific event)
             event.setCancelled(true);
        });

        // Hook into player connection events
        plugin.getEventRegistry().register(PlayerConnectEvent.class, event -> {
            // Use getPlayerRef() to get the player's reference and username
            LOGGER.atInfo().log("Player connected: " + event.getPlayerRef().getUsername());
        });
    }
}
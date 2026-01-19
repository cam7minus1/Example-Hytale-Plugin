package com.nations.plugin;

import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.math.vector.Vector3i;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BlockBreakHandler {
    private final DatabaseInterface db;
    private final Connection conn;
    private final Map<String, Long> lastActionTime = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BlockBreakHandler(DatabaseInterface db) {
        this.db = db;
        try {
            this.conn = db.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get DB connection", e);
        }
        scheduler.scheduleAtFixedRate(this::cleanupMap, 5, 5, TimeUnit.MINUTES);
    }

    private void cleanupMap() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = lastActionTime.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > 30000) { // 30 seconds
                System.out.println("Removing old destroy debounces");
                iterator.remove();
            }
        }
    }

    public void destroyBlock(DamageBlockEvent event) {
        Vector3i pos = event.getTargetBlock();
        String world = "world";
        String key = world + "_" + pos.x + "_" + pos.y + "_" + pos.z;

        long now = System.currentTimeMillis();

        try (PreparedStatement select = conn.prepareStatement(
                "SELECT health FROM reinforced_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            select.setString(1, world);
            select.setInt(2, pos.x);
            select.setInt(3, pos.y);
            select.setInt(4, pos.z);
            ResultSet rs = select.executeQuery();

            int health = 0;
            boolean exists = rs.next();
            if (exists) {
                health = rs.getInt("health");
            }

            if (!exists) {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO reinforced_blocks (world, x, y, z, health) VALUES (?, ?, ?, ?, 5)")) {
                    insert.setString(1, world);
                    insert.setInt(2, pos.x);
                    insert.setInt(3, pos.y);
                    insert.setInt(4, pos.z);
                    insert.executeUpdate();
                }
                health = 5;
            }

            if (health == 0) {
                return; // allow break, skip debounce
            }

            Long last = lastActionTime.get(key);
            if (last != null && now - last < 10000) {
                event.setCancelled(true);
                return;
            }
            lastActionTime.put(key, now);

            int newHealth = health - 1;
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE reinforced_blocks SET health = ? WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                update.setInt(1, newHealth);
                update.setString(2, world);
                update.setInt(3, pos.x);
                update.setInt(4, pos.y);
                update.setInt(5, pos.z);
                update.executeUpdate();
            }

            if (newHealth > 0) {
                event.setCancelled(true);
            }
        } catch (SQLException e) {
            System.err.println("DB error: " + e.getMessage());
            event.setCancelled(true);
        }
    }
}
package com.nations.plugin;

import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.math.vector.Vector3i; // correct import for Vector3i
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BlockBreakHandler {
    private final DatabaseInterface db;
    private final Connection conn;

    public BlockBreakHandler(DatabaseInterface db) {
        this.db = db;
        try {
            this.conn = db.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get DB connection", e);
        }
    }

    public void destroyBlock(BreakBlockEvent event) {
        Vector3i pos = event.getTargetBlock();

        // Replace "world" with actual world name retrieval
        // Example: event.getWorld() might exist, or use a fixed name for single-world servers
        String world = "world"; // ← TODO: Replace with real world name, e.g. event.getWorld().getName()

        try (PreparedStatement select = conn.prepareStatement(
                "SELECT health FROM reinforced_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {

            select.setString(1, world);
            select.setInt(2, pos.x);   // direct field access
            select.setInt(3, pos.y);
            select.setInt(4, pos.z);

            ResultSet rs = select.executeQuery();

            int health = 0;
            boolean exists = rs.next();
            if (exists) {
                health = rs.getInt("health");
            }

            if (!exists) {
                // Initialize with default health (5)
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

            if (health > 1) {
                // Decrement health and cancel break
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE reinforced_blocks SET health = health - 1 WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                    update.setString(1, world);
                    update.setInt(2, pos.x);
                    update.setInt(3, pos.y);
                    update.setInt(4, pos.z);
                    update.executeUpdate();
                }
                event.setCancelled(true);
                // Optional: Send message to player
                // event.getPlayerRef().ifPresent(p -> p.sendMessage("Reinforced! Health now: " + (health - 1)));
            } else if (health == 1) {
                // Last hit: remove reinforcement, allow break
                try (PreparedStatement delete = conn.prepareStatement(
                        "DELETE FROM reinforced_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                    delete.setString(1, world);
                    delete.setInt(2, pos.x);
                    delete.setInt(3, pos.y);
                    delete.setInt(4, pos.z);
                    delete.executeUpdate();
                }
                // No cancel → block breaks normally
            } else {
                // Not reinforced (health <= 0 or missing) → allow break
            }

        } catch (SQLException e) {
            // Log error, optionally cancel to prevent exploits on DB failure
            System.err.println("DB error during block break handling: " + e.getMessage());
            event.setCancelled(true); // Safety fallback
            throw new RuntimeException("Block break DB error", e);
        }
    }
}
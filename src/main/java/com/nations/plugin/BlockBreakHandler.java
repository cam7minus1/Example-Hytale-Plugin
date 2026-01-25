package com.nations.plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

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
    private InventoryInterface inventoryInterface;

    // NEW FIELDS
    private Map<String, Integer> reinforceValues = new HashMap<>();
    private int secondsPerHp = 10;

    public BlockBreakHandler(DatabaseInterface db) {

        this.db = db;

        try {
            this.conn = db.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get DB connection", e);
        }

        inventoryInterface = new InventoryInterface();
        scheduler.scheduleAtFixedRate(this::cleanupMap, 5, 5, TimeUnit.MINUTES);

        // Load YAML config files
        loadReinforceConfig();
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

    @SuppressWarnings("unchecked")
    private void loadReinforceConfig() {
        File folder = new File("mods/HynationsReinforce");

        if (!folder.exists()) {
            System.err.println("[BlockBreakHandler] Reinforce folder missing: " + folder.getAbsolutePath());
            return;
        }

        // -----------------------------
        // Load properties.yml
        // -----------------------------
        File propertiesFile = new File(folder, "properties.yml");

        if (propertiesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(fis);

                if (data != null && data.containsKey("secondsPerHp")) {
                    Object val = data.get("secondsPerHp");
                    if (val instanceof Number) {
                        secondsPerHp = ((Number) val).intValue();
                    }
                }

                System.out.println("[BlockBreakHandler] Loaded secondsPerHp = " + secondsPerHp);

            } catch (Exception e) {
                System.err.println("[BlockBreakHandler] Failed to load properties.yml");
                e.printStackTrace();
            }
        }

        // -----------------------------
        // Load reinforceBlocks.yml
        // -----------------------------
        File reinforceFile = new File(folder, "reinforceBlocks.yml");

        if (reinforceFile.exists()) {
            try (FileInputStream fis = new FileInputStream(reinforceFile)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(fis);

                if (data != null) {
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        if (entry.getValue() instanceof Number) {
                            reinforceValues.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                        }
                    }
                }

                System.out.println("[BlockBreakHandler] Loaded reinforce blocks: " + reinforceValues);

            } catch (Exception e) {
                System.err.println("[BlockBreakHandler] Failed to load reinforceBlocks.yml");
                e.printStackTrace();
            }
        }
    }

    public void destroyBlock(DamageBlockEvent event, Player player) {
        Vector3i pos = event.getTargetBlock();
        String world = "world";
        String key = world + "_" + pos.x + "_" + pos.y + "_" + pos.z;

        long now = System.currentTimeMillis();

        ItemStack item = player.getInventory().getItemInHand();
        String itemName = (item == null || item.getItem() == null)
                ? "EMPTY_HAND"
                : item.getItemId();

        Integer reinforceAmount = reinforceValues.get(itemName);

        int health = 0;
        boolean exists = false;

        // ---------------------------------------------------------
        // LOAD HEALTH FROM DB
        // ---------------------------------------------------------
        try (PreparedStatement select = conn.prepareStatement(
                "SELECT health FROM reinforced_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {

            select.setString(1, world);
            select.setInt(2, pos.x);
            select.setInt(3, pos.y);
            select.setInt(4, pos.z);

            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                exists = true;
                health = rs.getInt("health");
            }

        } catch (SQLException e) {
            System.err.println("[Reinforce] DB error loading health: " + e.getMessage());
            event.setCancelled(true);
            return;
        }

        // ---------------------------------------------------------
        // 1. STICK CHECK
        // ---------------------------------------------------------
        if (itemName.equals("Ingredient_Stick")) {
            System.out.println("[Reinforce] Stick check at " + key + " health=" + health);
            player.sendMessage(Message.raw("Block health: " + health));
            event.setCancelled(true);
            return;
        }

        // ---------------------------------------------------------
        // 2. REINFORCE ITEM
        // ---------------------------------------------------------
        if (reinforceAmount != null) {

            if (!exists) {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO reinforced_blocks (world, x, y, z, health) VALUES (?, ?, ?, ?, ?)")) {

                    insert.setString(1, world);
                    insert.setInt(2, pos.x);
                    insert.setInt(3, pos.y);
                    insert.setInt(4, pos.z);
                    insert.setInt(5, reinforceAmount);
                    insert.executeUpdate();

                    health = reinforceAmount;

                    System.out.println("[Reinforce] New reinforced block at " + key +
                            " health=" + health + " (added " + reinforceAmount + ")");

                } catch (SQLException e) {
                    System.err.println("[Reinforce] DB error inserting reinforce: " + e.getMessage());
                    event.setCancelled(true);
                    return;
                }

            } else {
                int newHealth = health + reinforceAmount;

                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE reinforced_blocks SET health = ? WHERE world = ? AND x = ? AND y = ? AND z = ?")) {

                    update.setInt(1, newHealth);
                    update.setString(2, world);
                    update.setInt(3, pos.x);
                    update.setInt(4, pos.y);
                    update.setInt(5, pos.z);
                    update.executeUpdate();

                    System.out.println("[Reinforce] Reinforced block at " + key +
                            " oldHP=" + health + " newHP=" + newHealth +
                            " (added " + reinforceAmount + ")");

                    health = newHealth;

                } catch (SQLException e) {
                    System.err.println("[Reinforce] DB error updating reinforce: " + e.getMessage());
                    event.setCancelled(true);
                    return;
                }
            }

            boolean success = inventoryInterface.removePlayerItem(player, itemName, reinforceAmount);

            if (!success){
                System.out.println("User did not have enough resources needed "+itemName+" "+reinforceAmount);
                return;
            }

            player.sendMessage(Message.raw("Reinforced block. New health: " + health));
            event.setCancelled(true);
            return;
        }

        // ---------------------------------------------------------
        // 3. NORMAL HIT
        // ---------------------------------------------------------

        if (!exists) {
            System.out.println("[Reinforce] Normal hit on unreinforced block at " + key + " → allow break");
            return;
        }

        Long last = lastActionTime.get(key);

        // First touch → start timer, NO DAMAGE
        if (last == null) {
            lastActionTime.put(key, now);
            System.out.println("[Reinforce] First touch at " + key + " → start 10s timer");
            event.setCancelled(true);
            return;
        }

        // Not enough time passed → cancel
        if (now - last < 10000) {
            System.out.println("[Reinforce] Debounce hit at " + key + " → cancel");
            event.setCancelled(true);
            return;
        }

        // Enough time passed → apply damage
        lastActionTime.put(key, now);

        int newHealth = health - 1;

        // Update DB
        try (PreparedStatement update = conn.prepareStatement(
                "UPDATE reinforced_blocks SET health = ? WHERE world = ? AND x = ? AND y = ? AND z = ?")) {

            update.setInt(1, newHealth);
            update.setString(2, world);
            update.setInt(3, pos.x);
            update.setInt(4, pos.y);
            update.setInt(5, pos.z);
            update.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[Reinforce] DB error decrementing health: " + e.getMessage());
            event.setCancelled(true);
            return;
        }

        System.out.println("[Reinforce] Damage block at " + key +
                " oldHP=" + health + " newHP=" + newHealth);

        player.sendMessage(Message.raw("Block health " + newHealth + " HP"));

        // ---------------------------------------------------------
        // HP <= 0 → ALLOW BREAK
        // ---------------------------------------------------------
        if (newHealth <= 0) {

            try (PreparedStatement delete = conn.prepareStatement(
                    "DELETE FROM reinforced_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {

                delete.setString(1, world);
                delete.setInt(2, pos.x);
                delete.setInt(3, pos.y);
                delete.setInt(4, pos.z);
                delete.executeUpdate();

                System.out.println("[Reinforce] Block at " + key + " reached 0 HP → DB entry removed");

            } catch (SQLException e) {
                System.err.println("[Reinforce] DB error deleting block: " + e.getMessage());
            }

            lastActionTime.remove(key);
            System.out.println("[Reinforce] Debounce cleared for " + key);

            System.out.println("[Reinforce] Block HP <= 0 at " + key + " → allow break");
            return;
        }

        // ---------------------------------------------------------
        // HP > 0 → CANCEL BREAK
        // ---------------------------------------------------------
        System.out.println("[Reinforce] Block still reinforced at " + key + " → cancel break");
        event.setCancelled(true);
    }
}
package com.nations.plugin;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

public class InventoryInterface {

    InventoryInterface(){}

    void givePlayerItem(Player player, String itemName, int amount){
        // 1. Get the player's inventory
        Inventory inventory = player.getInventory();

        // 2. Pick a container (main storage, hotbar, etc.)
        ItemContainer storage = inventory.getStorage(); // main inventory

        // 3. Create an item stack
        ItemStack stack = new ItemStack(itemName, amount); // use the correct item id for your setup

        // 4. Add it to the container
        storage.addItemStack(stack);
    }

    boolean removePlayerItem(Player player, String itemName, int amount) {
        Inventory inv = player.getInventory();

        ItemContainer hotbar = inv.getHotbar();
        ItemContainer storage = inv.getStorage();

        ItemContainer[] containers = new ItemContainer[] { hotbar, storage };

        int needed = amount;

        // First pass: count total available
        int totalFound = 0;

        for (ItemContainer c : containers) {
            if (c == null) continue;

            int cap = c.getCapacity();
            for (short slot = 0; slot < cap; slot++) {
                ItemStack s = c.getItemStack(slot);
                if (s == null) continue;

                if (s.getItemId().equals(itemName)) {
                    totalFound += s.getQuantity();
                    if (totalFound >= needed) break;
                }
            }

            if (totalFound >= needed) break;
        }

        if (totalFound < needed) {
            return false; // not enough items
        }

        // Second pass: remove exactly N items
        int remaining = needed;

        for (ItemContainer c : containers) {
            if (c == null) continue;

            int cap = c.getCapacity();
            for (short slot = 0; slot < cap; slot++) {
                if (remaining <= 0) break;

                ItemStack s = c.getItemStack(slot);
                if (s == null) continue;

                if (!s.getItemId().equals(itemName)) continue;

                int qty = s.getQuantity();

                // Remove the whole stack
                c.removeItemStackFromSlot(slot);

                if (qty > remaining) {
                    // Create leftover stack
                    int leftover = qty - remaining;
                    ItemStack newStack = s.withQuantity(leftover);

                    // Add leftover back to the SAME container
                    c.addItemStack(newStack);
                }

                remaining -= qty;
            }

            if (remaining <= 0) break;
        }

        return true;
    }

}

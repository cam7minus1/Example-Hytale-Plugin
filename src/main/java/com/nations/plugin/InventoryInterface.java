package com.nations.plugin;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

public class InventoryInterface {

    InventoryInterface(){

    }

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
        Inventory inventory = player.getInventory();

        ItemContainer[] containers = new ItemContainer[] {
                inventory.getHotbar(),
                inventory.getStorage(),
                inventory.getArmor(),
                inventory.getUtility(),
                inventory.getBackpack()
        };

        // -------------------------
        // FIRST PASS: Count total
        // -------------------------
        int total = 0;

        for (ItemContainer container : containers) {
            short cap = container.getCapacity();

            for (short slot = 0; slot < cap; slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (stack == null) continue;
                if (!stack.getItemId().equals(itemName)) continue;

                total += stack.getQuantity();
                if (total >= amount) break;
            }

            if (total >= amount) break;
        }

        // Not enough → fail without touching inventory
        if (total < amount) {
            return false;
        }

        // -------------------------
        // SECOND PASS: Remove stacks
        // -------------------------
        int remaining = amount;

        for (ItemContainer container : containers) {
            short cap = container.getCapacity();

            for (short slot = 0; slot < cap; slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (stack == null) continue;
                if (!stack.getItemId().equals(itemName)) continue;

                int qty = stack.getQuantity();

                if (qty <= remaining) {
                    // Remove entire stack
                    container.removeItemStack(stack);
                    remaining -= qty;

                    if (remaining == 0) {
                        return true;
                    }
                } else {
                    // We would need to remove only part of this stack,
                    // but the API does NOT allow modifying quantity.
                    // Therefore: atomic rule → fail.
                    return false;
                }
            }
        }

        return true; // logically unreachable after first-pass guarantee
    }

}

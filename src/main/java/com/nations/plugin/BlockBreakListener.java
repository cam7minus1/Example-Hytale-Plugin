package com.nations.plugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.query.Query;

public class BlockBreakListener extends EntityEventSystem<EntityStore, DamageBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public NationsPlugin plugin;

    public BlockBreakListener(NationsPlugin plugin) {
        super(DamageBlockEvent.class);
        this.plugin = plugin;
    }

    @Override
    public void handle(int index,
                          ArchetypeChunk<EntityStore> archetypeChunk,
                          Store<EntityStore> store,
                          CommandBuffer<EntityStore> commandBuffer,
                          DamageBlockEvent event) {
        LOGGER.atInfo().log("Block broken at: " + event.getTargetBlock().toString());


        Ref<EntityStore> entityStoreRef = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(entityStoreRef, Player.getComponentType());

        plugin.blockBreakhandler.destroyBlock(event, player);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
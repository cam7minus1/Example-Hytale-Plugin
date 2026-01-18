package com.nations.plugin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;

public class BlockBreakListener extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public BlockBreakListener() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index,
                          ArchetypeChunk<EntityStore> archetypeChunk,
                          Store<EntityStore> store,
                          CommandBuffer<EntityStore> commandBuffer,
                          BreakBlockEvent event) {
        LOGGER.atInfo().log("Block broken at: " + event.getTargetBlock().toString());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
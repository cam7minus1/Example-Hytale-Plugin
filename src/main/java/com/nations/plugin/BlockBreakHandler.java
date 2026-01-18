package com.nations.plugin;

import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;

public class BlockBreakHandler {

    DatabaseInterface db;
    BlockBreakHandler(DatabaseInterface db){
        this.db = db;
    }

    public void destroyBlock(BreakBlockEvent event){

    }

}

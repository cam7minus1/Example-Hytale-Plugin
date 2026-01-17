package com.nations.plugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class NationsPlugin extends JavaPlugin {

    public NationsPlugin(@Nonnull JavaPluginInit init){
        super(init);
    }

    @Override
    protected void setup(){
        super.setup();;
        this.getCommandRegistry().registerCommand(new NationCommand("hello", "An example command", false));
    }
}

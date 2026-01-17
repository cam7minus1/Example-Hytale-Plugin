package com.nations.plugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.Scanner;

public class NationsPlugin extends JavaPlugin {

    private DatabaseInterface db;

    public NationsPlugin(@Nonnull JavaPluginInit init){

        super(init);

        String host = "localhost";
        String user = "root";
        String dataBase = "nations";
        String password = "password";
        int port = 3306;

        this.db = new DatabaseInterface(host, port, dataBase, user, password);

    }

    @Override
    protected void setup(){
        super.setup();;
        this.getCommandRegistry().registerCommand(new NationCommand("hello", "An example command", false));
    }
}

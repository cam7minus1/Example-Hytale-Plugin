package com.nations.plugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NationsPlugin extends JavaPlugin {

    public DatabaseInterface db;
    public BlockBreakHandler blockBreakhandler;

    public NationsPlugin(@Nonnull JavaPluginInit init) {
        super(init);

        String host = "localhost";
        String user = "root";
        String dataBase = "nations";
        String password = "password";
        int port = 3306;

        this.db = new DatabaseInterface(host, port, dataBase, user, password);
        blockBreakhandler = new BlockBreakHandler(this.db);
    }

    @Override
    protected void setup() {
        super.setup();
        this.getCommandRegistry().registerCommand(
                new NationCommand("hello", "An example command", false)
        );
    }

    @Override
    protected void start() {
        super.start();

        // Auto-create reinforce config folder + YAML files
        createReinforceConfig();

        getEntityStoreRegistry().registerSystem(
                new BlockBreakListener(this)
        );
    }

    private void createReinforceConfig() {
        // Base folder: server working directory
        File baseFolder = new File("."); // current working directory

        // Our custom folder
        File reinforceFolder = new File(baseFolder, "HynationsReinforce");

        if (!reinforceFolder.exists()) {
            boolean created = reinforceFolder.mkdirs();
            if (!created) {
                System.err.println("[NationsPlugin] Failed to create HynationsReinforce folder at: "
                        + reinforceFolder.getAbsolutePath());
                return;
            }
        }

        // -----------------------------
        // properties.yml
        // -----------------------------
        File propertiesFile = new File(reinforceFolder, "properties.yml");

        if (!propertiesFile.exists()) {
            try (FileWriter writer = new FileWriter(propertiesFile)) {
                writer.write("secondsPerHp: 10\n");
            } catch (IOException e) {
                System.err.println("[NationsPlugin] Failed to write properties.yml");
                e.printStackTrace();
            }
        }

        // -----------------------------
        // reinforceBlocks.yml
        // -----------------------------
        File blocksFile = new File(reinforceFolder, "reinforceBlocks.yml");

        if (!blocksFile.exists()) {
            try (FileWriter writer = new FileWriter(blocksFile)) {
                writer.write("Rock_Stone_Cobble: 1\n");
            } catch (IOException e) {
                System.err.println("[NationsPlugin] Failed to write reinforceBlocks.yml");
                e.printStackTrace();
            }
        }
    }
}
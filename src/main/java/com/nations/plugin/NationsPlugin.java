package com.nations.plugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class NationsPlugin extends JavaPlugin {

    public DatabaseInterface db;
    public BlockBreakHandler blockBreakhandler;

    private File reinforceFolder;
    private File propertiesFile;
    private File blocksFile;

    public NationsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        // Create config folder + default YAMLs if missing
        createReinforceConfig();

        // Load DB settings from properties.yml
        Map<String, Object> props = loadProperties();

        String host     = (String) props.getOrDefault("dbHost", "localhost");
        int    port     = (int) props.getOrDefault("dbPort", 3306);
        String user     = (String) props.getOrDefault("dbUser", "root");
        String password = (String) props.getOrDefault("dbPassword", "password");
        String database = (String) props.getOrDefault("dbName", "nations");

        // Create DB connection using loaded values
        this.db = new DatabaseInterface(host, port, database, user, password);

        // Handlers
        blockBreakhandler = new BlockBreakHandler(this.db);

        // Commands
        this.getCommandRegistry().registerCommand(
                new NationCommand("hello", "An example command", false)
        );
    }

    @Override
    protected void start() {
        super.start();

        // Register block break listener
        getEntityStoreRegistry().registerSystem(
                new BlockBreakListener(this)
        );
    }

    // -------------------------------------------------------------------------
    // CONFIG CREATION
    // -------------------------------------------------------------------------
    private void createReinforceConfig() {
        File baseFolder = new File("."); // server working directory
        reinforceFolder = new File(baseFolder, "mods/HynationsReinforce");

        if (!reinforceFolder.exists()) {
            boolean created = reinforceFolder.mkdirs();
            if (!created) {
                System.err.println("[NationsPlugin] Failed to create HynationsReinforce folder at: "
                        + reinforceFolder.getAbsolutePath());
                return;
            }
        }

        // properties.yml
        propertiesFile = new File(reinforceFolder, "properties.yml");
        if (!propertiesFile.exists()) {
            try (FileWriter writer = new FileWriter(propertiesFile)) {
                writer.write("# Database configuration\n");
                writer.write("dbHost: localhost\n");
                writer.write("dbPort: 3306\n");
                writer.write("dbUser: root\n");
                writer.write("dbPassword: password\n");
                writer.write("dbName: nations\n\n");

                writer.write("# Reinforce settings\n");
                writer.write("hitsPerHp: 20\n");

            } catch (IOException e) {
                System.err.println("[NationsPlugin] Failed to write properties.yml");
                e.printStackTrace();
            }
        }

        // reinforceBlocks.yml
        blocksFile = new File(reinforceFolder, "reinforceBlocks.yml");
        if (!blocksFile.exists()) {
            try (FileWriter writer = new FileWriter(blocksFile)) {
                writer.write("# Reinforce values per block type\n");
                writer.write("Rock_Stone_Cobble: 1\n");
            } catch (IOException e) {
                System.err.println("[NationsPlugin] Failed to write reinforceBlocks.yml");
                e.printStackTrace();
            }
        }
    }

    // -------------------------------------------------------------------------
    // LOAD PROPERTIES
    // -------------------------------------------------------------------------
    private Map<String, Object> loadProperties() {
        try {
            Yaml yaml = new Yaml();
            return yaml.load(java.nio.file.Files.newInputStream(propertiesFile.toPath()));
        } catch (Exception e) {
            System.err.println("[NationsPlugin] Failed to load properties.yml, using defaults.");
            return Map.of(
                    "dbHost", "localhost",
                    "dbPort", 3306,
                    "dbUser", "root",
                    "dbPassword", "password",
                    "dbName", "nations",
                    "hitsPerHp", 10
            );
        }
    }
}
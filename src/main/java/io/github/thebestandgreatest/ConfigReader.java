package io.github.thebestandgreatest;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Helper class to generate and read the config file, implements a very basic toml parser to only rely on the java
 * standard library, and the logger providied by impulse
 *
 * @author Thebestandgreatest
 * @version 0.1.0
 */
public class ConfigReader {
    private final Logger logger;
    private final File configFile;

    /**
     * Creates a new config reader and attemps to create a default config file if one doesn't exist
     *
     * @param logger          The logger to log to
     * @param serverDirectory The directory of the server to read the config from
     */
    public ConfigReader(Logger logger, String serverDirectory) {
        this.logger = logger;
        Path dir = Paths.get(serverDirectory);
        this.configFile = dir.resolve("config.toml").toFile();

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            logger.error("Failed to create config directory {}: {}", dir, e.getMessage());
        }

        if (!configFile.exists()) {
            logger.info("Config file not found, creating default");
            createDefaultConfig();
        } else {
            logger.info("Found config file at {}", configFile.getAbsolutePath());
        }
    }

    /**
     * Creates a default config file and saves it to the plugin directory provided by Velocity
     */
    private void createDefaultConfig() {
        List<String> lines = Arrays.asList(
                "#name of the server that players initially join, a limbo or hub server of some kind",
                "entry_server = \"limbo\"", "#name of the server that will take too long to startup",
                "redirect_server = \"lobby\"");

        try {
            FileWriter writer = new FileWriter(configFile);
            for (String line : lines) {
                writer.write(line);
                writer.write('\n');
            }
            logger.info("Created default config file at {}", configFile.getAbsolutePath());
            writer.close();
        } catch (IOException e) {
            logger.error("Failed to create default config file {}", e.getMessage());
        }
    }

    /**
     * Implements a basic toml parser to read the config file, really won't work with anything but the most absolute
     * basic of toml files, but that is all the config needs to be
     *
     * @return A map of config keys and values
     */
    public Map<ConfigKeys, String> readConfig() {
        Map<ConfigKeys, String> config = new HashMap<>();

        try {
            // basic toml parser
            Scanner scanner = new Scanner(configFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                // ignore comments
                if (line.startsWith("#")) {
                    continue;
                }

                // split line by = and remove whitespace
                ConfigKeys key = ConfigKeys.valueOf(line.split("=")[0].trim().toUpperCase());
                // remove quotes from value
                String value = line.split("=")[1].trim().replaceAll("\"", "");
                config.put(key, value);
            }
        } catch (FileNotFoundException e) {
            logger.error("Failed to read config file {}", e.getMessage());
        }
        return config;
    }

    /**
     * Small helper enum to keep track of valid confid keys, just consists of the entry server (first server in the try
     * block), and a redirect server (server that takes a long time to startup)
     */
    public enum ConfigKeys {
        ENTRY_SERVER("entry_server"),
        REDIRECT_SERVER("redirect_server");

        public final String value;

        ConfigKeys(String value) {
            this.value = value;
        }
    }
}

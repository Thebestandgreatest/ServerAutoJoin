package io.github.thebestandgreatest;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.thebestandgreatest.utils.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * <p>A Plugin to get around the default minecraft connection timeout. The default timeout for vanilla minecraft is 30
 * * seconds, on low-end hardware or heavily modded servers startup can take significantly longer</p>
 *
 * <p>
 * This plugin gets around this by relying on velocity to initially send the player to a high-availability server,
 * either an always on lobby or a limbo server. It listens to all the requests, and if the player is attempting to join
 * the limbo server, it will queue a redirect, allowing for longer than the 30-second timeout
 * </p>
 *
 * @author Thebestandgreatest
 * @version 1.0.0
 */
@Plugin(id = "serverautojoin", name = "Server Auto Join", version = "1.0.0", description = "Auto join a server on a network to get around minecraft timeout restrictions", authors = "Thebestandgreatest")
public class ServerAutoJoin {
    private final ProxyServer server;
    private final Logger logger;

    private final String ENTRY_SERVER_NAME;
    private final String REDIRECT_SERVER_NAME;

    private RegisteredServer ENTRY_SERVER;
    private RegisteredServer REDIRECT_SERVER;

    /**
     * Constructor for the plugin that takes in standard parameters supplied by velocity
     *
     * @param server        Main proxy instance
     * @param logger        Logger instance
     * @param dataDirectory Plugin-specific directory
     */
    @Inject
    public ServerAutoJoin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        ConfigManager configReader = new ConfigManager(logger, dataDirectory.toString());
        Map<ConfigManager.ConfigKeys, String> config = configReader.readConfig();
        ENTRY_SERVER_NAME = config.get(ConfigManager.ConfigKeys.ENTRY_SERVER);
        REDIRECT_SERVER_NAME = config.get(ConfigManager.ConfigKeys.REDIRECT_SERVER);
    }

    /**
     * Makes sure the config is filled out, and that the options are things that are registered in velocity. Prints any
     * errors to the console
     */
    private void validateConfig() {
        if (ENTRY_SERVER_NAME == null || REDIRECT_SERVER_NAME == null) {
            logger.error("Entry server or redirect server is empty!");
            return;
        }
        if (server.getServer(ENTRY_SERVER_NAME).isEmpty()) {
            logger.error("Entry server {} does not exist!", ENTRY_SERVER_NAME);
        }
        if (server.getServer(REDIRECT_SERVER_NAME).isEmpty()) {
            logger.error("Redirect server {} does not exist!", REDIRECT_SERVER_NAME);
        }
    }

    /**
     * Fired when the plugin is initialized, and all the apis are safe to use, just validates the config and sets
     * instance variables
     *
     * @param event Provided by velocity
     */
    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        validateConfig();

        ENTRY_SERVER = server.getServer(ENTRY_SERVER_NAME).isPresent() ? server.getServer(ENTRY_SERVER_NAME).get() : null;
        REDIRECT_SERVER = server.getServer(REDIRECT_SERVER_NAME).isPresent() ? server.getServer(REDIRECT_SERVER_NAME).get() : null;
    }

    /**
     * Fired after a player has fully joined a server, determines if the player needs to be redirected, and queues a
     * connection request if the player needs to be
     *
     * @param event Server connection event provided by velocity
     */
    @Subscribe
    public void postPlayerJoinServer(ServerPostConnectEvent event) {
        Player player = event.getPlayer();

        if (player.getCurrentServer().isEmpty()) return;

        if (ENTRY_SERVER.equals(player.getCurrentServer().get().getServer())) {
            // communicate status with player
            TextComponent welcomeMessage = Component.text("Hello ")
                    .append(Component.text(player.getUsername() + "!", NamedTextColor.GREEN))
                    .append(Component.newline())
                    .append(Component.text("Sending you to "))
                    .append(Component.text(REDIRECT_SERVER_NAME, NamedTextColor.BLUE))
                    .append(Component.text("..."));
            player.sendMessage(welcomeMessage);

            // build and send connection request
            ConnectionRequestBuilder requestBuilder = player.createConnectionRequest(REDIRECT_SERVER);
            CompletableFuture<ConnectionRequestBuilder.Result> request = requestBuilder.connect();

            // monitor connection request status
            request.thenAccept(result -> {
//                    hideProgressBar(player);
                if (result.getStatus() != ConnectionRequestBuilder.Status.SUCCESS) {
                    player.sendMessage(Component.text("Unable to connect ", NamedTextColor.RED).
                            append(Component.text(result.getStatus().toString())));
                } else {
                    player.sendMessage(Component.text("Successfully Connected!"));
                }
            });
        }
    }
}

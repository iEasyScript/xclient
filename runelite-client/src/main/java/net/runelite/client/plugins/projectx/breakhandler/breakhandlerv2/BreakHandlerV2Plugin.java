package net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

/**
 * Break Handler V2 Plugin
 * Enhanced break handler with profile-based auto-login and intelligent world selection
 *
 * Features:
 * - Automatic login using profile data (username, password, member status)
 * - Intelligent world selection based on multiple modes:
 *   * Current/Preferred world
 *   * Random accessible world
 *   * Regional selection
 *   * Best population balance
 *   * Best ping performance
 * - Configurable break timing (min/max playtime and break duration)
 * - Optional in-game breaks (pause scripts without logout)
 * - Safety checks (waits for combat/interaction to end)
 * - Discord webhook notifications
 * - Profile-aware member/F2P world selection
 * - Retry mechanism with configurable attempts and delays
 * - In-game overlay showing break status and timers
 *
 * @version 2.0.1
 */
@PluginDescriptor(
    name = PluginDescriptor.Default + "BreakHandler V2",
    description = "Advanced break handler with profile-based login and world selection",
    tags = {"break", "projectx", "breakhandler", "login", "world", "profile", "v2"},
    enabledByDefault = false
)
@Slf4j
public class BreakHandlerV2Plugin extends Plugin {

    @Inject
    private BreakHandlerV2Config config;

    @Inject
    private BreakHandlerV2Script script;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private BreakHandlerV2Overlay overlay;

    @Provides
    BreakHandlerV2Config provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BreakHandlerV2Config.class);
    }

    @Override
    protected void startUp() throws AWTException {
        log.info("[BreakHandlerV2] Plugin starting up");
        System.out.println("[DEBUG] BreakHandlerV2Plugin.startUp() with script instance hash: " + System.identityHashCode(script));

        // Add in-game overlay
        if (overlayManager != null && overlay != null) {
            overlayManager.add(overlay);
            log.info("[BreakHandlerV2] In-game overlay added");
        }

        // Start the script
        if (script != null) {
            script.run(config);
            log.info("[BreakHandlerV2] Script started");
        }

        log.info("[BreakHandlerV2] Plugin started successfully (v{})", BreakHandlerV2Script.version);
    }

    @Override
    protected void shutDown() {
        log.info("[BreakHandlerV2] Plugin shutting down");

        // Shutdown script
        if (script != null) {
            script.shutdown();
            log.info("[BreakHandlerV2] Script stopped");
        }

        // Remove in-game overlay
        if (overlayManager != null && overlay != null) {
            overlayManager.remove(overlay);
            log.info("[BreakHandlerV2] In-game overlay removed");
        }

        log.info("[BreakHandlerV2] Plugin shut down successfully");
    }

    /**
     * Handle game state changes
     * Can be used to detect unexpected logouts or other state changes
     */
    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        // Future implementation: detect unexpected logouts, bans, etc.
        log.debug("[BreakHandlerV2] Game state changed: {}", event.getGameState());
    }
}

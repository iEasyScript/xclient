package net.runelite.client.plugins.projectx.externalplugins;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ExternalPluginsChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * "What's new": when a script you have installed moves to a new version, its
 * changelog appears in the game chat, so the fix you reported is visibly the fix
 * you got. The first sighting of a script is remembered quietly; only changes after
 * that are announced. Messages wait for login if the client is at the login screen.
 */
@Slf4j
@Singleton
public class ProjectXWhatsNew
{
    private static final String GROUP = "projectxwhatsnew";
    private static final int MAX_CHANGELOG = 300;

    private final Client client;
    private final EventBus eventBus;
    private final ConfigManager configManager;
    private final PluginManager pluginManager;
    private final ProjectXPluginManager projectxPluginManager;
    private final ChatMessageManager chatMessageManager;

    private final List<String> pending = new ArrayList<>();

    @Inject
    ProjectXWhatsNew(Client client, EventBus eventBus, ConfigManager configManager, PluginManager pluginManager,
                     ProjectXPluginManager projectxPluginManager, ChatMessageManager chatMessageManager)
    {
        this.client = client;
        this.eventBus = eventBus;
        this.configManager = configManager;
        this.pluginManager = pluginManager;
        this.projectxPluginManager = projectxPluginManager;
        this.chatMessageManager = chatMessageManager;
    }

    public void start()
    {
        eventBus.register(this);
    }

    public void stop()
    {
        eventBus.unregister(this);
    }

    @Subscribe
    public void onExternalPluginsChanged(ExternalPluginsChanged event)
    {
        check();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            flush();
        }
    }

    private void check()
    {
        for (Plugin plugin : pluginManager.getPlugins())
        {
            String internalName = plugin.getClass().getSimpleName();
            ProjectXPluginManifest manifest = projectxPluginManager.getManifestMap().get(internalName);
            PluginDescriptor d = plugin.getClass().getAnnotation(PluginDescriptor.class);
            if (manifest == null || d == null || d.version() == null || d.version().isEmpty())
            {
                continue;
            }
            String installed = d.version();
            String seen = configManager.getConfiguration(GROUP, internalName);
            if (installed.equals(seen))
            {
                continue;
            }
            configManager.setConfiguration(GROUP, internalName, installed);
            if (seen == null)
            {
                continue; // first time we have seen it: nothing has changed for this player yet
            }
            String name = d.name().replaceAll("<[^>]*>", "").trim();
            String changelog = installed.equals(manifest.getVersion()) ? manifest.getChangelog() : null;
            if (changelog != null && changelog.length() > MAX_CHANGELOG)
            {
                changelog = changelog.substring(0, MAX_CHANGELOG - 3).trim() + "...";
            }
            synchronized (pending)
            {
                pending.add(new ChatMessageBuilder()
                    .append(ChatColorType.HIGHLIGHT)
                    .append("[Project X] " + name + " updated to " + installed + ". ")
                    .append(ChatColorType.NORMAL)
                    .append(changelog == null || changelog.isBlank() ? "" : changelog)
                    .build());
            }
        }
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            flush();
        }
    }

    private void flush()
    {
        List<String> messages;
        synchronized (pending)
        {
            if (pending.isEmpty())
            {
                return;
            }
            messages = new ArrayList<>(pending);
            pending.clear();
        }
        for (String message : messages)
        {
            chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(message)
                .build());
        }
    }
}

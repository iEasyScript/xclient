package net.runelite.client.plugins.projectx.alerts;

import net.runelite.client.plugins.projectx.scriptqueue.ScriptQueueFinished;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2Plugin;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2State;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXSessionTracker;
import net.runelite.client.util.Text;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * Personal alerts: tells the player, on their own Discord webhook and/or as a
 * desktop notification, when something happens while they are away -- a script
 * stopped (and why), a death, a level-up, a valuable drop, the script queue
 * finishing, or an unplanned logout.
 *
 * <p>Alerts are queued and sent from a background task, at most one per type per
 * {@link #RATE_WINDOW_MS}; whatever piles up meanwhile goes out as one message.
 */
@PluginDescriptor(
    name = PluginDescriptor.Default + "Alerts",
    description = "Discord and desktop alerts: script stopped, death, level-up, valuable drop, queue finished, logged out",
    tags = {"projectx", "alerts", "discord", "webhook", "notification", "notify"},
    enabledByDefault = false
)
@Slf4j
public class AlertsPlugin extends Plugin
{
    static final long RATE_WINDOW_MS = 30_000;
    private static final long DRAIN_PERIOD_MS = 1_000;
    /** A logout this soon after a Logout click (the player's or a script's) was meant. */
    private static final long LOGOUT_CLICK_WINDOW_MS = 15_000;
    /** Stats the game sends this soon after login are the starting point, not level-ups. */
    private static final int LOGIN_BASELINE_TICKS = 5;
    private static final long TEST_COOLDOWN_MS = 5_000;
    private static final int REASON_MAX = 900;

    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private AlertsConfig config;
    @Inject
    private ConfigManager configManager;
    @Inject
    private OkHttpClient okHttpClient;
    @Inject
    private Notifier notifier;
    @Inject
    private ItemManager itemManager;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private ScheduledExecutorService executor;

    private final AlertRateLimiter limiter = new AlertRateLimiter(RATE_WINDOW_MS);
    private ScheduledFuture<?> drainTask;

    // Client thread only.
    private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
    private int ticksSinceLogin;
    private boolean wasLoggedIn;
    private boolean connectionLost;

    private volatile String playerName;
    private volatile long lastLogoutClickAt;
    private volatile long lastTestAt;

    @Provides
    AlertsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AlertsConfig.class);
    }

    @Override
    protected void startUp()
    {
        limiter.clear();
        drainTask = executor.scheduleWithFixedDelay(this::drain, DRAIN_PERIOD_MS, DRAIN_PERIOD_MS, TimeUnit.MILLISECONDS);
        clientThread.invokeLater(() ->
        {
            levels.clear();
            wasLoggedIn = client.getGameState() == GameState.LOGGED_IN;
            connectionLost = false;
            // Switched on mid-session: today's levels are the starting point.
            ticksSinceLogin = wasLoggedIn ? LOGIN_BASELINE_TICKS : 0;
            if (wasLoggedIn && client.getOverallExperience() > 0)
            {
                for (Skill skill : Skill.values())
                {
                    levels.put(skill, client.getRealSkillLevel(skill));
                }
            }
        });
    }

    @Override
    protected void shutDown()
    {
        if (drainTask != null)
        {
            drainTask.cancel(false);
            drainTask = null;
        }
        limiter.clear();
    }

    // ---- events

    @Subscribe
    public void onRunEnded(ProjectXSessionTracker.RunEnded event)
    {
        if (!config.scriptStopped())
        {
            return;
        }
        String how;
        switch (event.getEnd())
        {
            case SELF_STOP:
                how = event.getReason() == null || event.getReason().isEmpty()
                    ? "It stopped by itself without giving a reason."
                    : cleanReason(event.getReason());
                break;
            case ACCESS_ENDED:
                how = "Your access to this script ended.";
                break;
            case MANUAL:
                if (!config.manualStop())
                {
                    return;
                }
                how = "You stopped it.";
                break;
            default:
                // QUEUE: the queue moving on is normal. CLIENT_CLOSED: the player closed the client.
                return;
        }
        List<Alert.Field> fields = new ArrayList<>();
        fields.add(new Alert.Field("Script", event.getScriptName(), true));
        fields.add(new Alert.Field("Runtime", formatDuration(event.getEndedAt() - event.getStartedAt()), true));
        if (event.isDied())
        {
            fields.add(new Alert.Field("Died during the run", "Yes", true));
        }
        offer(AlertType.SCRIPT_STOPPED, "Script stopped: " + event.getScriptName(), how, fields);
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        Player me = client.getLocalPlayer();
        if (me == null || event.getActor() != me || !config.death())
        {
            return;
        }
        List<String> scripts = ProjectXSessionTracker.activeRunNames();
        if (config.onlyWhileScriptRuns() && scripts.isEmpty())
        {
            return;
        }
        offer(AlertType.DEATH, "You died", "Your character died.", scriptsField(scripts));
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        Skill skill = event.getSkill();
        int level = event.getLevel();
        Integer before = levels.put(skill, level);
        // The first value after login (or after switching the plugin on) is where we start from.
        if (before == null || ticksSinceLogin < LOGIN_BASELINE_TICKS || level <= before || !config.levelUp())
        {
            return;
        }
        List<String> scripts = ProjectXSessionTracker.activeRunNames();
        if (config.onlyWhileScriptRuns() && scripts.isEmpty())
        {
            return;
        }
        String title = skill.getName() + " level " + level + (level - before > 1 ? " (from " + before + ")" : "");
        offer(AlertType.LEVEL_UP, title, "", scriptsField(scripts));
    }

    @Subscribe
    public void onServerNpcLoot(ServerNpcLoot event)
    {
        String name = event.getComposition() == null ? null : Text.removeTags(event.getComposition().getName());
        checkLoot(name == null || name.isEmpty() ? "an NPC" : name, event.getItems());
    }

    @Subscribe
    public void onPlayerLootReceived(PlayerLootReceived event)
    {
        // Not the other player's name: alerts carry no one else's.
        checkLoot("a player", event.getItems());
    }

    /**
     * Chests, clue caskets, minigame rewards: the Loot Tracker's events. NPC and
     * player kills come from the two above (which need no plugin), and pickpocket
     * loot also arrives as server NPC loot, so those types are skipped here.
     */
    @Subscribe
    public void onLootReceived(LootReceived event)
    {
        LootRecordType type = event.getType();
        if (type == LootRecordType.NPC || type == LootRecordType.PLAYER || type == LootRecordType.PICKPOCKET)
        {
            return;
        }
        checkLoot(event.getName() == null || event.getName().isEmpty() ? "loot" : event.getName(), event.getItems());
    }

    @Subscribe
    public void onScriptQueueFinished(ScriptQueueFinished event)
    {
        if (!config.queueFinished())
        {
            return;
        }
        // The player stopping the queue themselves is not news to them.
        if (event.getOutcome() == ScriptQueueFinished.Outcome.STOPPED)
        {
            return;
        }
        StringBuilder description = new StringBuilder();
        if (event.getSummary() != null && !event.getSummary().trim().isEmpty())
        {
            description.append(event.getSummary().trim());
        }
        if (event.getReason() != null && !event.getReason().trim().isEmpty())
        {
            description.append(description.length() > 0 ? "\n" : "").append(event.getReason().trim());
        }
        String title = event.getOutcome() == ScriptQueueFinished.Outcome.COMPLETED
            ? "Script queue finished" : "Script queue gave up";
        if (event.getPlan() != null && !event.getPlan().isBlank())
        {
            title += ": " + event.getPlan();
        }
        offer(AlertType.QUEUE_FINISHED, title, description.toString(), null);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (ticksSinceLogin < LOGIN_BASELINE_TICKS)
        {
            ticksSinceLogin++;
        }
        Player me = client.getLocalPlayer();
        String name = me == null ? null : me.getName();
        if (name != null && !name.isEmpty())
        {
            playerName = name.replace(' ', ' ');
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        String option = event.getMenuOption();
        if (option != null && "logout".equalsIgnoreCase(Text.removeTags(option).replace(" ", "")))
        {
            lastLogoutClickAt = System.currentTimeMillis();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        switch (event.getGameState())
        {
            case LOGGED_IN:
                wasLoggedIn = true;
                connectionLost = false;
                break;
            case CONNECTION_LOST:
                connectionLost = true;
                break;
            case LOGIN_SCREEN:
                if (wasLoggedIn)
                {
                    loggedOut(connectionLost);
                }
                wasLoggedIn = false;
                connectionLost = false;
                levels.clear();
                ticksSinceLogin = 0;
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!AlertsConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }
        if (AlertsConfig.KEY_TEST.equals(event.getKey()))
        {
            sendTest();
        }
        else if (AlertsConfig.KEY_WEBHOOK.equals(event.getKey()))
        {
            String value = event.getNewValue();
            if (value != null && !value.trim().isEmpty() && !DiscordWebhook.isValid(value))
            {
                // Never the value itself: it is a credential.
                log.info("The alerts webhook URL is not a Discord webhook URL; Discord alerts stay off until it is");
            }
        }
    }

    // ---- building alerts

    private void loggedOut(boolean lost)
    {
        if (!config.loggedOut())
        {
            return;
        }
        List<String> scripts = ProjectXSessionTracker.activeRunNames();
        if (scripts.isEmpty())
        {
            return;
        }
        if (System.currentTimeMillis() - lastLogoutClickAt < LOGOUT_CLICK_WINDOW_MS || onBreak())
        {
            return;
        }
        String running = String.join(", ", scripts);
        if (lost)
        {
            offer(AlertType.LOGGED_OUT, "Disconnected",
                "Lost the connection to the game while " + running + " was running.", scriptsField(scripts));
        }
        else
        {
            offer(AlertType.LOGGED_OUT, "Logged out",
                "Logged out while " + running + " was running, and not for a planned break.", scriptsField(scripts));
        }
    }

    /** A BreakHandler (either version) that is switched on and in the middle of a break. */
    private boolean onBreak()
    {
        for (Plugin plugin : pluginManager.getPlugins())
        {
            if (plugin instanceof BreakHandlerPlugin && pluginManager.isPluginActive(plugin)
                && BreakHandlerScript.isBreakActive())
            {
                return true;
            }
            if (plugin instanceof BreakHandlerV2Plugin && pluginManager.isPluginActive(plugin)
                && BreakHandlerV2State.isBreakActive())
            {
                return true;
            }
        }
        return false;
    }

    /** Called on the client thread: prices need item definitions. */
    private void checkLoot(String source, Collection<ItemStack> items)
    {
        if (!config.valuableDrop() || items == null || items.isEmpty())
        {
            return;
        }
        List<String> scripts = ProjectXSessionTracker.activeRunNames();
        if (config.onlyWhileScriptRuns() && scripts.isEmpty())
        {
            return;
        }
        List<LootValue.Stack> stacks = new ArrayList<>();
        for (ItemStack item : items)
        {
            long price = itemManager.getItemPrice(item.getId());
            if (price <= 0 || item.getQuantity() <= 0)
            {
                continue;
            }
            String name = itemManager.getItemComposition(item.getId()).getName();
            stacks.add(new LootValue.Stack(name, item.getQuantity(), price));
        }
        long total = LootValue.total(stacks);
        if (!LootValue.isValuable(total, config.dropThreshold()))
        {
            return;
        }
        List<Alert.Field> fields = new ArrayList<>();
        fields.add(new Alert.Field("From", source, true));
        fields.addAll(scriptsField(scripts));
        offer(AlertType.VALUABLE_DROP, "Valuable drop: " + LootValue.formatGp(total) + " gp",
            LootValue.describe(stacks), fields);
    }

    private static List<Alert.Field> scriptsField(List<String> scripts)
    {
        List<Alert.Field> fields = new ArrayList<>();
        if (!scripts.isEmpty())
        {
            fields.add(new Alert.Field("Script", String.join(", ", scripts), true));
        }
        return fields;
    }

    private Alert build(AlertType type, String title, String description, List<Alert.Field> fields)
    {
        String player = config.includePlayerName() ? playerName : null;
        return new Alert(type, title, description, fields, player, System.currentTimeMillis());
    }

    private void offer(AlertType type, String title, String description, List<Alert.Field> fields)
    {
        if (!config.discordEnabled() && !config.desktopEnabled())
        {
            return;
        }
        limiter.offer(build(type, title, description, fields));
    }

    // ---- sending

    private void drain()
    {
        try
        {
            for (Alert alert : limiter.drain(System.currentTimeMillis()))
            {
                deliver(alert);
            }
        }
        catch (RuntimeException e)
        {
            log.warn("Alerts: could not send", e);
        }
    }

    private void deliver(Alert alert)
    {
        if (config.discordEnabled())
        {
            HttpUrl url = webhookUrl();
            if (url != null)
            {
                DiscordWebhook.send(okHttpClient, url, DiscordWebhook.buildPayload(alert), null);
            }
        }
        if (config.desktopEnabled())
        {
            String text = alert.toNotificationText();
            clientThread.invokeLater(() -> notifier.notify(text));
        }
    }

    /** This plugin's webhook, or the one saved on the account profile; null if neither is a Discord webhook. */
    private HttpUrl webhookUrl()
    {
        String raw = config.discordWebhookUrl();
        if (raw == null || raw.trim().isEmpty())
        {
            ConfigProfile profile = configManager.getProfile();
            raw = profile == null ? null : profile.getDiscordWebhookUrl();
        }
        return DiscordWebhook.parse(raw);
    }

    private void sendTest()
    {
        long now = System.currentTimeMillis();
        if (now - lastTestAt < TEST_COOLDOWN_MS)
        {
            return;
        }
        lastTestAt = now;

        boolean discord = config.discordEnabled();
        boolean desktop = config.desktopEnabled();
        if (!discord && !desktop)
        {
            showResult("Both channels are switched off: turn on Discord webhook or Desktop notification first.");
            return;
        }
        Alert alert = build(AlertType.TEST, "Project X test alert", "If you can read this, alerts reach you here.", null);
        String desktopLine = desktop
            ? "\n\nDesktop notification sent. RuneLite shows them only while the client is not focused,"
                + " unless its notification settings say otherwise."
            : "";
        if (desktop)
        {
            String text = alert.toNotificationText();
            clientThread.invokeLater(() -> notifier.notify(text));
        }
        if (!discord)
        {
            showResult(desktopLine.trim());
            return;
        }
        HttpUrl url = webhookUrl();
        if (url == null)
        {
            showResult("No Discord webhook to send to. Paste a https://discord.com/api/webhooks/... URL into"
                + " Webhook URL (or save one on your account profile)." + desktopLine);
            return;
        }
        DiscordWebhook.send(okHttpClient, url, DiscordWebhook.buildPayload(alert), code ->
        {
            String line;
            if (code >= 200 && code < 300)
            {
                line = "Discord accepted the test alert: check your channel.";
            }
            else if (code == 401 || code == 403 || code == 404)
            {
                line = "Discord refused the test alert (HTTP " + code + "). The webhook may have been deleted; make a new one.";
            }
            else if (code < 0)
            {
                line = "Could not reach Discord. Check your internet connection.";
            }
            else
            {
                line = "Discord refused the test alert (HTTP " + code + "). Try again in a minute.";
            }
            showResult(line + desktopLine);
        });
    }

    /** Not ProjectX.showMessage: that would be taken as the reason a running script stops next. */
    private static void showResult(String message)
    {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(null, message, "Project X alerts", JOptionPane.INFORMATION_MESSAGE));
    }

    static String cleanReason(String reason)
    {
        String text = reason.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<[^>]*>", "")
            .replaceAll("[ \\t\\x0B\\f]+", " ").replaceAll(" *\\n *", "\n").trim();
        return text.length() > REASON_MAX ? text.substring(0, REASON_MAX) + "…" : text;
    }

    static String formatDuration(long ms)
    {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0)
        {
            return String.format("%dh %02dm", h, m);
        }
        if (m > 0)
        {
            return String.format("%dm %02ds", m, sec);
        }
        return sec + "s";
    }
}

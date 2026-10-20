package net.runelite.client.plugins.projectx.remote;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerConfig;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerState;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2Plugin;
import net.runelite.client.plugins.projectx.externalplugins.PluginJarClassLoader;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXEntitlements;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManager;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManifest;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXSessionTracker;
import net.runelite.client.plugins.projectx.scriptqueue.ScriptQueuePlugin;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.requirements.RequirementCheck;
import net.runelite.client.plugins.projectx.util.requirements.RequirementCheckConfig;
import net.runelite.client.plugins.projectx.util.requirements.RequirementOutcome;
import net.runelite.client.plugins.projectx.util.requirements.RequirementReport;
import net.runelite.client.plugins.projectx.util.requirements.RequirementSeverity;
import net.runelite.client.plugins.projectx.util.requirements.RequirementStatus;
import net.runelite.client.util.Text;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * The website's commands, carried out in this client by the same routes the
 * player's own clicks take: the plugin manager (behind the entitlement gate) for
 * scripts, the Script Queue's own start and stop, BreakHandler's Break Now and
 * End Break Now settings, and the usual logout.
 *
 * <p>Only store scripts can be started or stopped, only ones this account may
 * run, and only after the pre-start requirements check the queue uses. Runs on
 * the remote thread; plugin changes are made on the Swing thread, as the plugin
 * list makes them.
 */
@Slf4j
class LiveRemoteActions implements RemoteActions
{
    private static final long SWING_TIMEOUT_MS = 10_000;
    private static final long LOGOUT_WAIT_MS = 10_000;

    private final PluginManager pluginManager;
    private final ProjectXPluginManager projectxPluginManager;
    private final ProjectXEntitlements entitlements;
    private final ConfigManager configManager;
    private final BooleanSupplier queueRunning;

    LiveRemoteActions(PluginManager pluginManager, ProjectXPluginManager projectxPluginManager,
                      ProjectXEntitlements entitlements, ConfigManager configManager, BooleanSupplier queueRunning)
    {
        this.pluginManager = pluginManager;
        this.projectxPluginManager = projectxPluginManager;
        this.entitlements = entitlements;
        this.configManager = configManager;
        this.queueRunning = queueRunning;
    }

    // ---- scripts

    @Override
    public String startScript(String internalName) throws Refused
    {
        Plugin plugin = storePlugin(internalName);
        if (plugin == null)
        {
            throw new Refused(projectxPluginManager.getManifestMap().containsKey(internalName)
                ? "it isn't installed in this client yet; restart the client to download it"
                : "this client doesn't have that script");
        }
        String name = RequirementCheck.scriptName(plugin);
        if (pluginManager.isPluginActive(plugin))
        {
            return name + " is already running";
        }

        // The start gate would refuse it anyway; asking first gives the website a real reason.
        if (entitlements.isPaid(internalName) && !entitlements.isEntitled(internalName))
        {
            entitlements.refresh();
            if (!entitlements.isEntitled(internalName))
            {
                throw new Refused("you don't have access to " + name + " right now");
            }
        }

        ProjectXPluginManifest manifest = projectxPluginManager.getManifestMap().get(internalName);
        if (manifest != null && manifest.isBroken())
        {
            throw new Refused(name + " is marked as not working right now; start it in the client if you want to anyway");
        }

        String missing = missingRequirements(plugin);
        if (missing != null)
        {
            throw new Refused("missing requirements: " + missing);
        }

        Boolean started = onSwing(() ->
        {
            pluginManager.setPluginEnabled(plugin, true);
            return pluginManager.startPlugin(plugin);
        });
        if (started == null || !pluginManager.isPluginActive(plugin))
        {
            throw new Refused(name + " didn't start");
        }
        return "started " + name;
    }

    @Override
    public String stopScript(String internalName) throws Refused
    {
        Plugin plugin = storePlugin(internalName);
        if (plugin == null || !pluginManager.isPluginActive(plugin))
        {
            return "it wasn't running";
        }
        stop(plugin);
        return "stopped " + RequirementCheck.scriptName(plugin);
    }

    @Override
    public String stopAll() throws Refused
    {
        boolean queue = queueRunning.getAsBoolean();
        if (queue)
        {
            ScriptQueuePlugin.stopQueue();
        }
        int stopped = 0;
        for (Plugin plugin : runningStoreScripts())
        {
            stop(plugin);
            stopped++;
        }
        if (stopped == 0 && !queue)
        {
            return "nothing was running";
        }
        return "stopped " + stopped + (stopped == 1 ? " script" : " scripts") + (queue ? " and the queue" : "");
    }

    /** The store script by its class name, or by a name it had before the store renamed it. */
    private Plugin storePlugin(String internalName)
    {
        for (Plugin plugin : pluginManager.getPlugins())
        {
            if (!isStoreScript(plugin))
            {
                continue;
            }
            String simple = plugin.getClass().getSimpleName();
            if (simple.equals(internalName))
            {
                return plugin;
            }
            ProjectXPluginManifest manifest = projectxPluginManager.getManifestMap().get(internalName);
            if (manifest != null && manifest.getPreviousInternalNames() != null && manifest.getPreviousInternalNames().contains(simple))
            {
                return plugin;
            }
        }
        return null;
    }

    private List<Plugin> runningStoreScripts()
    {
        List<Plugin> out = new ArrayList<>();
        for (Plugin plugin : pluginManager.getPlugins())
        {
            if (isStoreScript(plugin) && pluginManager.isPluginActive(plugin))
            {
                out.add(plugin);
            }
        }
        return out;
    }

    /** Came out of a downloaded store jar: never one of the client's own plugins. */
    private static boolean isStoreScript(Plugin plugin)
    {
        return plugin != null && plugin.getClass().getClassLoader() instanceof PluginJarClassLoader;
    }

    private void stop(Plugin plugin) throws Refused
    {
        // Stopped on purpose, as from the plugin list: never a "stopped by itself" alert.
        ProjectXSessionTracker.markStop(plugin, ProjectXSessionTracker.End.MANUAL);
        onSwing(() ->
        {
            pluginManager.setPluginEnabled(plugin, false);
            return pluginManager.stopPlugin(plugin);
        });
    }

    /**
     * The required things the script is missing, as the queue's check sees them, or
     * null. Recommended and unknown ones never stop a start; nor does a check that is
     * switched off or skipped for this script, or being logged out (levels and items
     * read as nothing then, and the script logs in itself).
     */
    private String missingRequirements(Plugin plugin)
    {
        RequirementCheckConfig config = configManager.getConfig(RequirementCheckConfig.class);
        if (config == null || !config.enabled() || !ProjectX.isLoggedIn())
        {
            return null;
        }
        String skipped = config.skippedScripts();
        String simple = plugin.getClass().getSimpleName();
        if (skipped != null && Text.fromCSV(skipped).stream().anyMatch(s -> s.equalsIgnoreCase(simple)))
        {
            return null;
        }
        RequirementReport report = RequirementCheck.evaluate(plugin);
        if (!report.hasUnmetRequired())
        {
            return null;
        }
        String which = report.getOutcomes().stream()
            .filter(o -> o.getSeverity() == RequirementSeverity.REQUIRED && o.getStatus() == RequirementStatus.UNMET)
            .map(RequirementOutcome::getDescription)
            .limit(5)
            .collect(Collectors.joining(", "));
        return which.isEmpty() ? report.summary() : which;
    }

    // ---- queue

    @Override
    public String startQueue(String plan) throws Refused
    {
        String why = ScriptQueuePlugin.startPlan(plan);
        if (why != null)
        {
            throw new Refused(why);
        }
        return plan == null ? "queue started" : "queue started with " + plan;
    }

    @Override
    public String stopQueue() throws Refused
    {
        String why = ScriptQueuePlugin.stopQueue();
        if (why != null)
        {
            throw new Refused(why);
        }
        return "queue stopping";
    }

    // ---- breaks

    /**
     * BreakHandler's own Break Now switch: it picks the request up within a second
     * and takes the break its settings describe. BreakHandler V2 has no such switch.
     */
    @Override
    public String startBreak() throws Refused
    {
        requireBreakHandlerV1();
        if (BreakHandlerScript.isBreakActive())
        {
            throw new Refused("already on a break");
        }
        if (BreakHandlerScript.isLockState())
        {
            throw new Refused("a script is holding breaks off right now");
        }
        configManager.setConfiguration(BreakHandlerConfig.configGroup, "breakNow", true);
        return "break requested";
    }

    @Override
    public String endBreak() throws Refused
    {
        requireBreakHandlerV1();
        BreakHandlerState state = BreakHandlerScript.getCurrentState();
        if (state != BreakHandlerState.LOGGED_OUT && state != BreakHandlerState.INGAME_BREAK_ACTIVE)
        {
            throw new Refused(BreakHandlerScript.isBreakActive() ? "the break is already starting or ending" : "not on a break");
        }
        configManager.setConfiguration(BreakHandlerConfig.configGroup, "breakEndNow", true);
        return "ending the break";
    }

    private void requireBreakHandlerV1() throws Refused
    {
        if (isActive(BreakHandlerPlugin.class))
        {
            return;
        }
        if (isActive(BreakHandlerV2Plugin.class))
        {
            throw new Refused("BreakHandler V2 can't be told to break from the website; use BreakHandler instead");
        }
        throw new Refused("no break handler is on in this client");
    }

    private boolean isActive(Class<? extends Plugin> type)
    {
        for (Plugin plugin : pluginManager.getPlugins())
        {
            if (plugin.getClass() == type)
            {
                return pluginManager.isPluginActive(plugin);
            }
        }
        return false;
    }

    // ---- account

    /**
     * Stops everything first, so no script or queue step logs straight back in,
     * then logs out the usual way. The client stays open.
     */
    @Override
    public String logout() throws Refused
    {
        if (!ProjectX.isLoggedIn())
        {
            return "already logged out";
        }
        String stopped = stopAll();
        Rs2Player.logout();
        long until = System.currentTimeMillis() + LOGOUT_WAIT_MS;
        while (ProjectX.isLoggedIn() && System.currentTimeMillis() < until)
        {
            try
            {
                Thread.sleep(250);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (ProjectX.isLoggedIn())
        {
            throw new Refused("the game didn't log out; " + stopped);
        }
        return "logged out; " + stopped;
    }

    // ---- threads

    /** Runs on the Swing thread, as plugin changes from the plugin list do, and waits for it. */
    private static <T> T onSwing(Callable<T> work) throws Refused
    {
        FutureTask<T> task = new FutureTask<>(work);
        SwingUtilities.invokeLater(task);
        try
        {
            return task.get(SWING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new Refused("interrupted");
        }
        catch (ExecutionException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof PluginInstantiationException)
            {
                log.warn("Website command: plugin change failed", cause);
                throw new Refused("the client couldn't do that: " + cause.getMessage());
            }
            throw new RuntimeException(cause);
        }
        catch (TimeoutException e)
        {
            throw new Refused("the client is busy; try again");
        }
    }
}

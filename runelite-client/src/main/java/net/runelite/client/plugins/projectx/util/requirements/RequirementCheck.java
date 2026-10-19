package net.runelite.client.plugins.projectx.util.requirements;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.projectx.ProjectX;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The pre-start requirements check, without any UI.
 *
 * <p>For a script queue or anything else that starts scripts:
 * <pre>{@code
 * RequirementReport report = RequirementCheck.evaluate(plugin);   // not on the EDT
 * if (report.hasUnmetRequired())
 * {
 *     log.info("Skipping {}: {}", report.getScriptName(), report);
 * }
 * }</pre>
 *
 * <p>What is checked: the plugin's own {@link ScriptRequirements#requirements()} if it
 * implements {@link ScriptRequirements}, plus (unless it opts out) the inventory setups chosen
 * in its settings, as recommended. A declared requirement replaces an inferred one with the same
 * {@link Requirement#getKey() key}.
 */
@Slf4j
public final class RequirementCheck
{
    private RequirementCheck()
    {
    }

    /** What the plugin needs, declared and inferred. Any thread; reads no game state. */
    public static List<Requirement> collect(Plugin plugin)
    {
        if (plugin == null)
        {
            return Collections.emptyList();
        }
        List<Requirement> declared = new ArrayList<>();
        boolean infer = true;
        if (plugin instanceof ScriptRequirements)
        {
            ScriptRequirements sr = (ScriptRequirements) plugin;
            try
            {
                declared.addAll(Requirements.nonNull(sr.requirements()));
                infer = sr.inferInventorySetups();
            }
            catch (RuntimeException e)
            {
                log.warn("{}: requirements() failed: {}", plugin.getClass().getSimpleName(), e.toString());
            }
        }
        List<Requirement> inferred = Collections.emptyList();
        if (infer)
        {
            try
            {
                inferred = InventorySetupInference.fromConfig(plugin, ProjectX.getPluginManager(), ProjectX.getConfigManager());
            }
            catch (RuntimeException e)
            {
                log.debug("{}: setup inference failed: {}", plugin.getClass().getSimpleName(), e.toString());
            }
        }
        return merge(declared, inferred);
    }

    /** Declared first; an inferred requirement whose key was declared is dropped. */
    static List<Requirement> merge(List<Requirement> declared, List<Requirement> inferred)
    {
        List<Requirement> out = new ArrayList<>(declared);
        Set<String> keys = new HashSet<>();
        for (Requirement r : declared)
        {
            if (r.getKey() != null)
            {
                keys.add(r.getKey());
            }
        }
        for (Requirement r : inferred)
        {
            if (r.getKey() == null || keys.add(r.getKey()))
            {
                out.add(r);
            }
        }
        return out;
    }

    /**
     * Checks everything the plugin needs and returns the results, worst first. Reads the game
     * on the client thread: from another thread this waits for it (up to 10 seconds), so do
     * not call it on the Swing thread. Never throws.
     */
    public static RequirementReport evaluate(Plugin plugin)
    {
        String name = scriptName(plugin);
        List<Requirement> requirements = collect(plugin);
        if (requirements.isEmpty())
        {
            return RequirementReport.empty(name);
        }
        if (SwingUtilities.isEventDispatchThread())
        {
            log.warn("RequirementCheck.evaluate({}) called on the Swing thread; it may freeze the UI", name);
        }
        Client client = ProjectX.getClient();
        if (client == null || ProjectX.getClientThread() == null)
        {
            return RequirementReport.allUnknown(name, requirements, "The game isn't loaded yet");
        }
        if (client.isClientThread())
        {
            return evaluateOnClientThread(name, requirements);
        }
        return ProjectX.getClientThread()
            .runOnClientThreadOptional(() -> evaluateOnClientThread(name, requirements))
            .orElseGet(() -> RequirementReport.allUnknown(name, requirements, "Couldn't read the game in time"));
    }

    /** Checks {@code requirements} against the live game. Must be called on the client thread. */
    public static RequirementReport evaluateOnClientThread(String scriptName, List<Requirement> requirements)
    {
        Client client = ProjectX.getClient();
        if (client == null || !client.isClientThread())
        {
            throw new IllegalStateException("evaluateOnClientThread must run on the client thread");
        }
        return RequirementReport.evaluate(scriptName, requirements, new LiveRequirementContext(client));
    }

    /** The name the player sees for the plugin, without any HTML. */
    public static String scriptName(Plugin plugin)
    {
        if (plugin == null)
        {
            return "";
        }
        PluginDescriptor d = plugin.getClass().getAnnotation(PluginDescriptor.class);
        String raw = d != null && !d.name().isBlank() ? d.name() : plugin.getClass().getSimpleName();
        return raw.replaceAll("<[^>]*>", "").trim();
    }
}

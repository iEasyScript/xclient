package net.runelite.client.plugins.projectx.util.requirements;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.scriptqueue.ScriptQueuePlugin;
import net.runelite.client.plugins.projectx.scriptqueue.ScriptQueuePreStartCheck;
import net.runelite.client.util.Text;

/**
 * Holds the settings of the pre-start requirements check, so they have a place in the plugin
 * list. The check itself runs from the plugin list when a script is started; see
 * {@link RequirementCheck}.
 *
 * <p>It also gives the Script Queue the same check before each step. Nobody is there to
 * answer a dialog during a queue, so only a missing <em>required</em> item stops a step (which
 * then follows the step's "if it fails" setting); recommended and unknown ones never do.
 */
@PluginDescriptor(
    name = "Pre-start checks",
    description = "Before a script starts, shows a checklist of what it needs and what's missing",
    tags = {"projectx", "requirements", "checklist", "start"},
    enabledByDefault = true,
    alwaysOn = true
)
public class RequirementCheckPlugin extends Plugin
{
    @Inject
    private RequirementCheckConfig config;

    @Provides
    RequirementCheckConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(RequirementCheckConfig.class);
    }

    @Override
    protected void startUp()
    {
        ScriptQueuePlugin.setPreStartCheck(queueCheck);
    }

    @Override
    protected void shutDown()
    {
        ScriptQueuePlugin.setPreStartCheck(null);
    }

    private final ScriptQueuePreStartCheck queueCheck = step ->
    {
        if (!config.enabled() || step.getPluginClass() == null)
        {
            return null;
        }
        Plugin plugin = ProjectX.getPlugin(step.getPluginClass());
        if (plugin == null || isSkipped(plugin))
        {
            // A script that isn't installed is the queue's own failure to report.
            return null;
        }
        RequirementReport report = RequirementCheck.evaluate(plugin);
        return report.hasUnmetRequired() ? "missing requirements (" + report.summary() + ")" : null;
    };

    private boolean isSkipped(Plugin plugin)
    {
        String csv = config.skippedScripts();
        if (csv == null || csv.isBlank())
        {
            return false;
        }
        String simple = plugin.getClass().getSimpleName();
        return Text.fromCSV(csv).stream().anyMatch(s -> s.equalsIgnoreCase(simple));
    }
}

package net.runelite.client.plugins.projectx.example;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.projectx.util.requirements.ItemSource;
import net.runelite.client.plugins.projectx.util.requirements.Requirement;
import net.runelite.client.plugins.projectx.util.requirements.Requirements;
import net.runelite.client.plugins.projectx.util.requirements.ScriptRequirements;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Example Plugin",
        description = "Performance test for GameObject composition retrieval",
        tags = {"performance", "projectx", "test", "gameobject"},
        enabledByDefault = false
)
@Slf4j
public class ExamplePlugin extends Plugin implements ScriptRequirements {
    @Inject
    ExampleScript exampleScript;
    @Inject
    ExampleScriptOverlay exampleScriptOverlay;
    @Inject
    OverlayManager overlayManager;

    /**
     * An example of the pre-start checklist: shown when this plugin is started from the
     * plugin list and something here is missing.
     */
    @Override
    public List<Requirement> requirements() {
        return List.of(
                Requirements.skill(Skill.ATTACK, 1),
                Requirements.questCompleted(Quest.COOKS_ASSISTANT).recommended(),
                Requirements.item("Coins", 1000, ItemSource.ANYWHERE, ItemID.COINS).recommended(),
                Requirements.membersWorld().recommended());
    }

    @Override
    protected void startUp() throws AWTException {
        overlayManager.add(exampleScriptOverlay);
        exampleScript.run();
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(exampleScriptOverlay);
        exampleScript.shutdown();
    }

    // on settings change
    @Subscribe
    public void onConfigChanged(final ConfigChanged event) {
    }
}

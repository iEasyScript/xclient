package net.runelite.client.plugins.projectx.util.requirements;

import java.util.List;

/**
 * Implemented by a script's plugin to say what the script needs. Before the player starts it
 * from the plugin list, the client checks these and shows a checklist when something is
 * missing, with Start anyway and Cancel.
 *
 * <pre>{@code
 * public class MyAlcherPlugin extends Plugin implements ScriptRequirements
 * {
 *     @Inject
 *     private MyAlcherConfig config;
 *
 *     @Override
 *     public List<Requirement> requirements()
 *     {
 *         return List.of(
 *             Requirements.skill(Skill.MAGIC, 55),
 *             Requirements.runesFor(Rs2Spells.HIGH_LEVEL_ALCHEMY, 100, true),
 *             Requirements.itemNamed(config.itemName(), 100),
 *             Requirements.membersWorld().recommended());
 *     }
 * }
 * }</pre>
 *
 * <p>{@code requirements()} may be called on any thread and must only build the list; the
 * checks themselves run later on the client thread. A plugin that does not implement this
 * still has the inventory setups chosen in its settings checked; see
 * {@link #inferInventorySetups()}.
 */
public interface ScriptRequirements
{
    List<Requirement> requirements();

    /**
     * Whether the inventory setups chosen in the plugin's settings are also checked
     * (as recommended). Return false when the script decides itself which setups matter and
     * declares them in {@link #requirements()}.
     */
    default boolean inferInventorySetups()
    {
        return true;
    }
}

package net.runelite.client.plugins.projectx.bot;

import net.runelite.api.NPC;
import net.runelite.client.plugins.projectx.api.npc.models.Rs2NpcModel;

/**
 * An NPC in the scene, as returned by {@link Npcs#query()}.
 *
 * <pre>{@code
 * Npc cow = Npcs.query().nameEquals("Cow").nearestReachable();
 * if (cow != null) {
 *     cow.interact("Attack");
 * }
 * }</pre>
 *
 * <p>Every method of the older {@link Rs2NpcModel} works here, and an {@code Npc} can be passed
 * anywhere an {@code Rs2NpcModel} is expected.
 */
@SuppressWarnings("deprecation")
public class Npc extends Rs2NpcModel
{
    public Npc(final NPC npc)
    {
        super(npc);
    }

    /**
     * The same NPC as an {@code Npc}. Returns {@code model} itself when it already is one.
     *
     * @return the NPC, or null when {@code model} is null
     */
    public static Npc of(Rs2NpcModel model)
    {
        if (model == null)
        {
            return null;
        }
        if (model instanceof Npc)
        {
            return (Npc) model;
        }
        return new Npc(model.getNpc());
    }
}

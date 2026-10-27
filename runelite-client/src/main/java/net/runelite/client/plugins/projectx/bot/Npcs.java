package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.npc.Rs2Npc;

import java.util.stream.Stream;

/**
 * NPCs in the scene: monsters, bankers, shopkeepers.
 *
 * <pre>{@code
 * Npc cow = Npcs.query().nameEquals("Cow").nearestReachable();
 * if (cow != null) {
 *     cow.interact("Attack");
 * }
 * // or in one go: Npcs.query().nameEquals("Cow").interact("Attack");
 * }</pre>
 *
 * <p>{@link #query()} is the way to find NPCs. The older static helpers of {@link Rs2Npc}
 * ({@code Npcs.attack(...)}, {@code Npcs.getNpc(...)}, ...) are reachable here too so existing code
 * can move over by renaming; note those return the older
 * {@code util.npc.Rs2NpcModel}, not {@link Npc}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Npcs extends Rs2Npc
{
    private Npcs()
    {
    }

    /**
     * Starts a search over the NPCs in the scene, for example
     * {@code Npcs.query().nameEquals("Banker").nearest()}.
     */
    public static NpcQuery query()
    {
        return new NpcQuery();
    }

    /**
     * Starts a search over the NPCs named exactly {@code name} (ignoring case). Same as
     * {@code query().nameEquals(name)}, for example {@code Npcs.query("Cow").nearest()}.
     */
    public static NpcQuery query(String name)
    {
        return query().nameEquals(name);
    }

    /** Every NPC in the scene this tick. */
    public static Stream<Npc> stream()
    {
        return ProjectX.getRs2NpcCache().getStream().map(Npc::of);
    }
}

/**
 * The scripting API: short, prefix-free names for everything a script does.
 *
 * <pre>{@code
 * import net.runelite.client.plugins.projectx.bot.*;
 *
 * SceneObject rock = Objects.query().nameEquals("Copper rocks").nearestReachable();
 * if (rock != null) {
 *     rock.interact("Mine");
 * }
 * Npcs.query().nameEquals("Cow").interact("Attack");
 * if (Inventory.isFull()) {
 *     Bank.depositAll();
 * }
 * }</pre>
 *
 * <ul>
 *   <li><b>Helpers</b> ({@link Inventory}, {@link Bank}, {@link Player}, {@link Walker}, ...) are
 *       subclasses of the older {@code Rs2*} helpers, so every static method of the old class is
 *       available under the new name.</li>
 *   <li><b>Searches</b> start from {@link Objects#query()}, {@link Npcs#query()},
 *       {@link GroundItems#query()} and {@link Players#query()} and return {@link SceneObject},
 *       {@link Npc}, {@link FloorItem} and {@link PlayerModel}.</li>
 *   <li><b>Constants</b> of the older enums are in holders such as {@link Prayers},
 *       {@link Spells} and {@link Foods}.</li>
 * </ul>
 *
 * <p>The older {@code Rs2*} names keep working (they are marked deprecated, never removed), so
 * scripts that were already compiled against them run unchanged. Guide:
 * {@code docs/SCRIPTING.md} in the client repository.
 */
package net.runelite.client.plugins.projectx.bot;

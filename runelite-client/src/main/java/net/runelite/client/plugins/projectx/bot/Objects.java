package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject;

import java.util.Comparator;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Objects in the scene: rocks, trees, doors, bank booths, ladders, altars.
 *
 * <pre>{@code
 * SceneObject rock = Objects.query().nameEquals("Copper rocks").nearestReachable();
 * if (rock != null) {
 *     rock.interact("Mine");
 * }
 * }</pre>
 *
 * <p>{@link #query()} is the way to find objects. The older static helpers of
 * {@link Rs2GameObject} ({@code Objects.interact(...)}, {@code Objects.getGameObject(...)}, ...)
 * are reachable here too so existing code can move over by renaming.
 *
 * <p><b>{@code java.util.Objects}.</b> This class shares its simple name with
 * {@link java.util.Objects}, so a file cannot import both by name. To spare you the clash it also
 * offers the same static methods ({@code equals}, {@code hash}, {@code requireNonNull},
 * {@code isNull}, {@code nonNull}, ...), which simply call {@code java.util.Objects}. Drop
 * {@code import java.util.Objects;} and the existing calls keep working.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Objects extends Rs2GameObject
{
    private Objects()
    {
    }

    /**
     * Starts a search over the objects in the scene, for example
     * {@code Objects.query().nameEquals("Oak tree").nearestReachable()}.
     */
    public static ObjectQuery query()
    {
        return new ObjectQuery();
    }

    /**
     * Starts a search over the objects named exactly {@code name} (ignoring case). Same as
     * {@code query().nameEquals(name)}, for example {@code Objects.query("Copper rocks").nearest()}.
     */
    public static ObjectQuery query(String name)
    {
        return query().nameEquals(name);
    }

    /** Every object in the scene this tick. */
    public static Stream<SceneObject> stream()
    {
        return ProjectX.getRs2TileObjectCache().getStream().map(SceneObject::of);
    }

    // ---- java.util.Objects, so files that used it keep compiling -------------------------------

    /** Same as {@link java.util.Objects#equals(Object, Object)}. */
    public static boolean equals(Object a, Object b)
    {
        return java.util.Objects.equals(a, b);
    }

    /** Same as {@link java.util.Objects#deepEquals(Object, Object)}. */
    public static boolean deepEquals(Object a, Object b)
    {
        return java.util.Objects.deepEquals(a, b);
    }

    /** Same as {@link java.util.Objects#hashCode(Object)}. */
    public static int hashCode(Object o)
    {
        return java.util.Objects.hashCode(o);
    }

    /** Same as {@link java.util.Objects#hash(Object...)}. */
    public static int hash(Object... values)
    {
        return java.util.Objects.hash(values);
    }

    /** Same as {@link java.util.Objects#toString(Object)}. */
    public static String toString(Object o)
    {
        return java.util.Objects.toString(o);
    }

    /** Same as {@link java.util.Objects#toString(Object, String)}. */
    public static String toString(Object o, String nullDefault)
    {
        return java.util.Objects.toString(o, nullDefault);
    }

    /** Same as {@link java.util.Objects#compare(Object, Object, Comparator)}. */
    public static <T> int compare(T a, T b, Comparator<? super T> c)
    {
        return java.util.Objects.compare(a, b, c);
    }

    /** Same as {@link java.util.Objects#requireNonNull(Object)}. */
    public static <T> T requireNonNull(T obj)
    {
        return java.util.Objects.requireNonNull(obj);
    }

    /** Same as {@link java.util.Objects#requireNonNull(Object, String)}. */
    public static <T> T requireNonNull(T obj, String message)
    {
        return java.util.Objects.requireNonNull(obj, message);
    }

    /** Same as {@link java.util.Objects#requireNonNull(Object, Supplier)}. */
    public static <T> T requireNonNull(T obj, Supplier<String> messageSupplier)
    {
        return java.util.Objects.requireNonNull(obj, messageSupplier);
    }

    /** Same as {@link java.util.Objects#requireNonNullElse(Object, Object)}. */
    public static <T> T requireNonNullElse(T obj, T defaultObj)
    {
        return java.util.Objects.requireNonNullElse(obj, defaultObj);
    }

    /** Same as {@link java.util.Objects#requireNonNullElseGet(Object, Supplier)}. */
    public static <T> T requireNonNullElseGet(T obj, Supplier<? extends T> supplier)
    {
        return java.util.Objects.requireNonNullElseGet(obj, supplier);
    }

    /** Same as {@link java.util.Objects#isNull(Object)}. */
    public static boolean isNull(Object obj)
    {
        return obj == null;
    }

    /** Same as {@link java.util.Objects#nonNull(Object)}. */
    public static boolean nonNull(Object obj)
    {
        return obj != null;
    }

    /** Same as {@link java.util.Objects#checkIndex(int, int)}. */
    public static int checkIndex(int index, int length)
    {
        return java.util.Objects.checkIndex(index, length);
    }
}

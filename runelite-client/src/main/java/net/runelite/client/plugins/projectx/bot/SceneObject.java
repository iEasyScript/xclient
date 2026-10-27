package net.runelite.client.plugins.projectx.bot;

import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.client.plugins.projectx.api.tileobject.models.Rs2TileObjectModel;

/**
 * An object in the scene (a rock, tree, door, bank booth, ladder...), as returned by
 * {@link Objects#query()}.
 *
 * <pre>{@code
 * SceneObject rock = Objects.query().nameEquals("Copper rocks").nearestReachable();
 * if (rock != null) {
 *     rock.interact("Mine");
 * }
 * }</pre>
 *
 * <p>Every method of the older {@link Rs2TileObjectModel} works here, and a {@code SceneObject}
 * can be passed anywhere an {@code Rs2TileObjectModel} is expected. Named {@code SceneObject}
 * so it does not clash with RuneLite's {@link GameObject} and {@link TileObject}.
 */
@SuppressWarnings("deprecation")
public class SceneObject extends Rs2TileObjectModel
{
    public SceneObject(GameObject gameObject)
    {
        super(gameObject);
    }

    public SceneObject(DecorativeObject tileObject)
    {
        super(tileObject);
    }

    public SceneObject(WallObject tileObject)
    {
        super(tileObject);
    }

    public SceneObject(GroundObject tileObject)
    {
        super(tileObject);
    }

    public SceneObject(TileObject tileObject)
    {
        super(tileObject);
    }

    /**
     * The same object as a {@code SceneObject}. Returns {@code model} itself when it already is one.
     *
     * @return the object, or null when {@code model} is null
     */
    public static SceneObject of(Rs2TileObjectModel model)
    {
        if (model == null)
        {
            return null;
        }
        if (model instanceof SceneObject)
        {
            return (SceneObject) model;
        }
        return new SceneObject(model.getTileObject());
    }
}

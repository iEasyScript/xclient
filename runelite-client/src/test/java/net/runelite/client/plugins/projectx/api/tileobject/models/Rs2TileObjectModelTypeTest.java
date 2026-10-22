package net.runelite.client.plugins.projectx.api.tileobject.models;

import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class Rs2TileObjectModelTypeTest {

    @Test
    public void wrappingAPlainTileObjectKeepsItsRealType() {
        TileObject boat = mock(GameObject.class);
        TileObject door = mock(WallObject.class);
        TileObject decoration = mock(DecorativeObject.class);
        TileObject ground = mock(GroundObject.class);

        assertEquals(TileObjectType.GAME, new Rs2TileObjectModel(boat).getTileObjectType());
        assertEquals(TileObjectType.WALL, new Rs2TileObjectModel(door).getTileObjectType());
        assertEquals(TileObjectType.DECORATIVE, new Rs2TileObjectModel(decoration).getTileObjectType());
        assertEquals(TileObjectType.GROUND, new Rs2TileObjectModel(ground).getTileObjectType());
        assertEquals(TileObjectType.GENERIC, new Rs2TileObjectModel(mock(TileObject.class)).getTileObjectType());
    }
}

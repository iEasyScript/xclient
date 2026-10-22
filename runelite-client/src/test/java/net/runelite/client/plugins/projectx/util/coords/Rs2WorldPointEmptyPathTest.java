package net.runelite.client.plugins.projectx.util.coords;

import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Rs2WorldPointEmptyPathTest
{
    private static final WorldPoint SOURCE = new WorldPoint(3200, 3200, 0);
    private static final WorldPoint TARGET = new WorldPoint(3205, 3200, 0);

    @Test
    public void distanceToPathIsUnreachableWhenPathIsEmpty()
    {
        Rs2WorldPoint point = new Rs2WorldPoint(SOURCE)
        {
            @Override
            public List<WorldPoint> pathTo(WorldPoint other)
            {
                return Collections.emptyList();
            }
        };

        assertEquals(Integer.MAX_VALUE, point.distanceToPath(TARGET));
    }

    @Test
    public void distanceToPathSumsCheckpointsWhenPathReachesTarget()
    {
        Rs2WorldPoint point = new Rs2WorldPoint(SOURCE)
        {
            @Override
            public List<WorldPoint> pathTo(WorldPoint other)
            {
                return List.of(new WorldPoint(3203, 3200, 0), TARGET);
            }
        };

        assertEquals(5, point.distanceToPath(TARGET));
    }
}

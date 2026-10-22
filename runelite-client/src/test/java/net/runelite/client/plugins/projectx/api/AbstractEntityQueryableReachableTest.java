package net.runelite.client.plugins.projectx.api;

import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Selection behind {@code interact(...)} / {@link AbstractEntityQueryable#nearestReachable(int)}:
 * nearest reachable first, and a fallback (not a silent refusal) when reachability cannot be
 * worked out.
 */
public class AbstractEntityQueryableReachableTest {

    // No client in these tests. Other tests in the suite leave ProjectX's static client and client
    // thread set; with them, anchor mapping would wait on a client thread that never runs.
    private Object previousClient;
    private Object previousClientThread;

    @Before
    public void noClient() throws Exception {
        previousClient = swapStatic("client", null);
        previousClientThread = swapStatic("clientThread", null);
    }

    @After
    public void restoreClient() throws Exception {
        swapStatic("client", previousClient);
        swapStatic("clientThread", previousClientThread);
    }

    private static Object swapStatic(String name, Object value) throws Exception {
        java.lang.reflect.Field field = net.runelite.client.plugins.projectx.ProjectX.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private static final WorldPoint PLAYER = new WorldPoint(3200, 3200, 0);

    @After
    public void clearInterrupt() {
        Thread.interrupted();
    }

    private static List<EntityDistance<Fake>> byDistance(int maxDistance, Fake... entities) {
        return AbstractEntityQueryable.byDistance(Stream.of(entities), PLAYER, maxDistance);
    }

    @Test
    public void picksTheNearestReachableNotTheNearest() {
        Fake nearUnreachable = new Fake(1, 3201, 3200, false);
        Fake farReachable = new Fake(2, 3205, 3200, true);
        Fake fartherReachable = new Fake(3, 3210, 3200, true);

        assertSame(farReachable, AbstractEntityQueryable.pickNearestReachable(
                byDistance(Integer.MAX_VALUE, fartherReachable, nearUnreachable, farReachable), true));
    }

    @Test
    public void stopsAskingAfterTheFirstReachable() {
        Fake near = new Fake(1, 3201, 3200, true);
        Fake far = new Fake(2, 3205, 3200, true);
        AbstractEntityQueryable.pickNearestReachable(byDistance(Integer.MAX_VALUE, far, near), true);
        assertEquals(1, near.reachableCalls);
        assertEquals(0, far.reachableCalls);
    }

    @Test
    public void equallyNearEntitiesKeepStreamOrder() {
        Fake first = new Fake(1, 3202, 3200, true);
        Fake second = new Fake(2, 3200, 3202, true);
        assertSame(first, AbstractEntityQueryable.pickNearestReachable(byDistance(Integer.MAX_VALUE, first, second), true));
    }

    @Test
    public void maxDistanceStillApplies() {
        Fake far = new Fake(1, 3220, 3200, true);
        assertNull(AbstractEntityQueryable.pickNearestReachable(byDistance(10, far), true));
    }

    @Test
    public void noneReachableIsNull() {
        Fake a = new Fake(1, 3201, 3200, false);
        assertNull(AbstractEntityQueryable.pickNearestReachable(byDistance(Integer.MAX_VALUE, a), true));
    }

    @Test
    public void noPlayerWorldViewFallsBackToTheNearestCloseEntity() {
        Fake near = new Fake(1, 3203, 3200, false);
        assertSame(near, AbstractEntityQueryable.pickNearestReachable(byDistance(Integer.MAX_VALUE, near), false));
        assertEquals("reachability is not asked when it cannot be judged", 0, near.reachableCalls);
    }

    @Test
    public void fallbackOnlyReachesASmallDistance() {
        Fake far = new Fake(1, 3200 + AbstractEntityQueryable.REACHABILITY_FALLBACK_DISTANCE + 1, 3200, true);
        assertNull(AbstractEntityQueryable.pickNearestReachable(byDistance(Integer.MAX_VALUE, far), false));
    }

    @Test
    public void aThrowingReachabilityCheckFallsBackInsteadOfRefusing() {
        Fake broken = new Fake(1, 3201, 3200, false) {
            @Override
            public boolean isReachable() {
                throw new IllegalStateException("collision maps not loaded");
            }
        };
        Fake other = new Fake(2, 3204, 3200, false);
        assertSame(broken, AbstractEntityQueryable.pickNearestReachable(byDistance(Integer.MAX_VALUE, other, broken), true));
    }

    @Test
    public void anInterruptIsNotTreatedAsUnknownReachability() {
        Fake shuttingDown = new Fake(1, 3201, 3200, false) {
            @Override
            public boolean isReachable() {
                throw new RuntimeException("Interrupted waiting for client thread", new InterruptedException());
            }
        };
        try {
            AbstractEntityQueryable.pickNearestReachable(byDistance(Integer.MAX_VALUE, shuttingDown), true);
            fail("an interrupt must reach the caller, which turns it into null");
        } catch (RuntimeException expected) {
            assertEquals(InterruptedException.class, expected.getCause().getClass());
        }
    }

    @Test
    public void nearestByTemplateAnchorWithoutAClientUsesTheAnchorAsIs() {
        // No client (unit test): the anchor cannot be mapped and must be used unchanged.
        Fake near = new Fake(1, 3201, 3200, true);
        Fake far = new Fake(2, 3300, 3200, true);
        TestQueryable q = new TestQueryable(Stream.of(far, near));
        assertSame(near, q.nearest(PLAYER, 5));
    }

    // ---- fixtures ------------------------------------------------------------

    private static class TestQueryable extends AbstractEntityQueryable<TestQueryable, Fake> {
        TestQueryable(Stream<Fake> source) {
            super();
            this.source = source;
        }

        @Override
        protected Stream<Fake> initialSource() {
            return Stream.empty();
        }
    }

    private static class Fake implements IEntity {
        private final int id;
        private final WorldPoint loc;
        private final boolean reachable;
        int reachableCalls;

        Fake(int id, int x, int y, boolean reachable) {
            this.id = id;
            this.loc = new WorldPoint(x, y, 0);
            this.reachable = reachable;
        }

        @Override public int getId() { return id; }
        @Override public String getName() { return "fake"; }
        @Override public WorldPoint getWorldLocation() { return loc; }
        @Override public LocalPoint getLocalLocation() { return null; }
        @Override public WorldView getWorldView() { return null; }
        @Override public boolean click() { return false; }
        @Override public boolean click(String action) { return false; }

        @Override
        public boolean isReachable() {
            reachableCalls++;
            return reachable;
        }
    }
}

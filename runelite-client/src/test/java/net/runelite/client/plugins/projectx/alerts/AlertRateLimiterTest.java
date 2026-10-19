package net.runelite.client.plugins.projectx.alerts;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AlertRateLimiterTest
{
    private static final long WINDOW = 30_000;

    private static Alert alert(AlertType type, String title)
    {
        return new Alert(type, title, "", null, "p", 0L);
    }

    @Test
    public void firstAlertGoesStraightOut()
    {
        AlertRateLimiter limiter = new AlertRateLimiter(WINDOW);
        Alert a = alert(AlertType.DEATH, "You died");
        limiter.offer(a);
        List<Alert> out = limiter.drain(1_000);
        assertEquals(1, out.size());
        assertSame("a lone alert is sent as it is", a, out.get(0));
        assertTrue(limiter.drain(1_001).isEmpty());
    }

    @Test
    public void atMostOnePerTypePerWindow()
    {
        AlertRateLimiter limiter = new AlertRateLimiter(WINDOW);
        limiter.offer(alert(AlertType.LEVEL_UP, "Mining level 50"));
        assertEquals(1, limiter.drain(0).size());

        limiter.offer(alert(AlertType.LEVEL_UP, "Mining level 51"));
        assertTrue("held inside the window", limiter.drain(10_000).isEmpty());
        assertTrue(limiter.drain(29_999).isEmpty());
        List<Alert> out = limiter.drain(30_000);
        assertEquals(1, out.size());
        assertEquals("Mining level 51", out.get(0).getTitle());
    }

    @Test
    public void typesAreLimitedSeparately()
    {
        AlertRateLimiter limiter = new AlertRateLimiter(WINDOW);
        limiter.offer(alert(AlertType.LEVEL_UP, "Mining level 50"));
        limiter.drain(0);
        limiter.offer(alert(AlertType.DEATH, "You died"));
        limiter.offer(alert(AlertType.LEVEL_UP, "Mining level 51"));
        List<Alert> out = limiter.drain(1_000);
        assertEquals(1, out.size());
        assertEquals(AlertType.DEATH, out.get(0).getType());
    }

    @Test
    public void burstsCollapseIntoOne()
    {
        AlertRateLimiter limiter = new AlertRateLimiter(WINDOW);
        limiter.offer(alert(AlertType.LEVEL_UP, "Attack level 40"));
        limiter.offer(alert(AlertType.LEVEL_UP, "Hitpoints level 41"));
        limiter.offer(alert(AlertType.LEVEL_UP, "Strength level 42"));
        List<Alert> out = limiter.drain(0);
        assertEquals(1, out.size());
        Alert collapsed = out.get(0);
        assertEquals(AlertType.LEVEL_UP, collapsed.getType());
        assertEquals("Level up (x3)", collapsed.getTitle());
        assertTrue(collapsed.getDescription().contains("Attack level 40"));
        assertTrue(collapsed.getDescription().contains("Hitpoints level 41"));
        assertTrue(collapsed.getDescription().contains("Strength level 42"));
        assertEquals("p", collapsed.getPlayer());
    }

    @Test
    public void bigBurstsAreCountedNotKept()
    {
        AlertRateLimiter limiter = new AlertRateLimiter(WINDOW);
        for (int i = 0; i < 50; i++)
        {
            limiter.offer(alert(AlertType.VALUABLE_DROP, "drop " + i));
        }
        List<Alert> out = limiter.drain(0);
        assertEquals(1, out.size());
        assertEquals("Valuable drop (x50)", out.get(0).getTitle());
        assertTrue(out.get(0).getDescription().endsWith("...and " + (50 - AlertRateLimiter.MAX_KEPT) + " more"));
    }

    @Test
    public void clearDropsWhatWasWaiting()
    {
        AlertRateLimiter limiter = new AlertRateLimiter(WINDOW);
        limiter.offer(alert(AlertType.DEATH, "You died"));
        limiter.clear();
        assertTrue(limiter.drain(0).isEmpty());
    }
}

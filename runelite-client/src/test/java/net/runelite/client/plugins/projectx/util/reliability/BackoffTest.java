package net.runelite.client.plugins.projectx.util.reliability;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BackoffTest
{
    @Test
    public void doublesFromTheBaseUpToTheCap()
    {
        long[] expected = {600, 1200, 2400, 4800, 4800, 4800};
        for (int attempt = 1; attempt <= expected.length; attempt++)
        {
            assertEquals("attempt " + attempt, expected[attempt - 1],
                Backoff.delayAfterAttempt(attempt, Backoff.DEFAULT_BASE_MS, Backoff.DEFAULT_MAX_MS));
        }
    }

    @Test
    public void noWaitForNonsenseInput()
    {
        assertEquals(0, Backoff.delayAfterAttempt(0, 600, 4800));
        assertEquals(0, Backoff.delayAfterAttempt(-3, 600, 4800));
        assertEquals(0, Backoff.delayAfterAttempt(2, 0, 4800));
        assertEquals(0, Backoff.delayAfterAttempt(2, 600, 0));
    }

    @Test
    public void neverOverflows()
    {
        assertEquals(10_000, Backoff.delayAfterAttempt(500, 600, 10_000));
        assertEquals(Long.MAX_VALUE, Backoff.delayAfterAttempt(40, Long.MAX_VALUE / 2, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE - 1, Backoff.delayAfterAttempt(1, Long.MAX_VALUE - 1, Long.MAX_VALUE));
    }
}

package net.runelite.client.plugins.projectx.util.reliability;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReliabilityLogTest
{
    @Test
    public void linesAreStructured()
    {
        String line = ReliabilityLog.line("Woodcutter", "Chop tree", "retry",
            "attempt", "2/4", "waitMs", 1200L, "skipped", null, "reason", "not confirmed within 3000ms");

        assertEquals("[Reliability] script=Woodcutter step=\"Chop tree\" event=retry attempt=2/4 waitMs=1200"
            + " reason=\"not confirmed within 3000ms\"", line);
    }

    @Test
    public void emailsAndTokensAreMasked()
    {
        String s = ReliabilityLog.sanitize("login player.one+alt@example.co.uk failed with token "
            + "eyJhbGciOiJIUzI1NiJ9abcdefghijklmnopqrstuvwxyz0123");

        assertFalse(s, s.contains("example.co.uk"));
        assertFalse(s, s.contains("eyJhbGciOiJIUzI1NiJ9"));
        assertTrue(s, s.contains("<email>"));
        assertTrue(s, s.contains("<redacted>"));
    }

    @Test
    public void ordinaryDetailIsKept()
    {
        String s = ReliabilityLog.sanitize("IllegalStateException: net.runelite.client.plugins.projectx.util.bank.Rs2Bank not open");
        assertEquals("IllegalStateException: net.runelite.client.plugins.projectx.util.bank.Rs2Bank not open", s);
    }

    @Test
    public void flattenedAndShortened()
    {
        StringBuilder longText = new StringBuilder("line one\r\nline \"two\"\t");
        for (int i = 0; i < 100; i++)
        {
            longText.append("word ");
        }
        String s = ReliabilityLog.sanitize(longText.toString());

        assertTrue(s.startsWith("line one line 'two' word"));
        assertFalse(s.contains("\n"));
        assertEquals(ReliabilityLog.MAX_VALUE_LENGTH, s.length());
        assertTrue(s.endsWith("..."));
        assertEquals("", ReliabilityLog.sanitize(null));
    }

    @Test
    public void describesExceptions()
    {
        assertEquals("IllegalStateException: bank shut", ReliabilityLog.describe(new IllegalStateException("bank shut")));
        assertEquals("NullPointerException", ReliabilityLog.describe(new NullPointerException()));
    }
}

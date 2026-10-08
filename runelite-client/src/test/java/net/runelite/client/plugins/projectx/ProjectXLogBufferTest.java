package net.runelite.client.plugins.projectx;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectXLogBufferTest
{
    private static final Logger SCRIPT = LoggerFactory.getLogger("net.runelite.client.plugins.projectx.sailing.SalvagingScript");
    private static final Logger OTHER = LoggerFactory.getLogger("net.runelite.client.plugins.projectx.zulrah.ZulrahScript");

    @BeforeClass
    public static void install()
    {
        ProjectXLogBuffer.install();
        ProjectXLogBuffer.install(); // twice is harmless
    }

    @Test
    public void keepsTheScriptsOwnLinesAndWarningsFromAnywhere()
    {
        SCRIPT.info("Cargo hold: opening for deposit");
        OTHER.info("Zulrah phase 3");
        OTHER.warn("Walker could not find a path");

        String recent = ProjectXLogBuffer.recent("net.runelite.client.plugins.projectx.sailing", 30, 100);

        assertTrue(recent, recent.contains("Cargo hold: opening for deposit"));
        assertTrue(recent, recent.contains("Walker could not find a path"));
        assertFalse(recent, recent.contains("Zulrah phase 3"));
    }

    @Test
    public void keepsWhatScriptsLogThroughTheSharedLoggerAndTheWalker()
    {
        // Mad Cow's dodge trace, like most scripts' status lines, goes through ProjectX.log.
        LoggerFactory.getLogger("net.runelite.client.plugins.projectx.ProjectX").info("[Mad Cow][Dodge] event-time immediate CHARGE");
        LoggerFactory.getLogger("net.runelite.client.plugins.projectx.util.walker.Rs2Walker").info("Walker clicked tile");
        LoggerFactory.getLogger("net.runelite.client.plugins.gpu.GpuPlugin").info("unrelated core plugin line");

        String recent = ProjectXLogBuffer.recent("net.runelite.client.plugins.projectx.kspmadcow", 30, 100);

        assertTrue(recent, recent.contains("[Mad Cow][Dodge] event-time immediate CHARGE"));
        assertTrue(recent, recent.contains("Walker clicked tile"));
        assertFalse(recent, recent.contains("unrelated core plugin line"));
    }

    @Test
    public void includesStackTracesAndKeepsOnlyTheLastLines()
    {
        for (int i = 0; i < 50; i++)
        {
            SCRIPT.info("line {}", i);
        }
        SCRIPT.error("boom", new IllegalStateException("hold went missing"));

        String recent = ProjectXLogBuffer.recent("net.runelite.client.plugins.projectx.sailing", 30, 5);

        assertTrue(recent, recent.contains("IllegalStateException: hold went missing"));
        assertTrue(recent, recent.contains("line 49"));
        assertFalse(recent, recent.contains("line 10\n"));
    }
}

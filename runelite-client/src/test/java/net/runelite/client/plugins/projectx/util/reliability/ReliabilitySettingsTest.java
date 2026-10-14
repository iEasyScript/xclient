package net.runelite.client.plugins.projectx.util.reliability;

import net.runelite.client.plugins.projectx.ProjectXConfig;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ReliabilitySettingsTest
{
    @After
    public void restore()
    {
        ReliabilitySettings.update(ReliabilitySettings.DEFAULTS);
    }

    @Test
    public void defaultsMatchTheSettingsPanel()
    {
        ReliabilitySettings d = ReliabilitySettings.DEFAULTS;
        assertTrue(d.isEnabled());
        assertEquals(3, d.getMaxRetries());
        assertEquals(5, d.getMaxConsecutiveFailures());
        assertEquals(120, d.getNoProgressTimeoutSeconds());
        assertEquals(120_000L, d.getNoProgressTimeoutMillis());
        assertFalse(d.isDetailedLogging());
    }

    @Test
    public void outOfRangeValuesAreClamped()
    {
        ReliabilitySettings low = ReliabilitySettings.of(true, -4, 0, 1, false);
        assertEquals(0, low.getMaxRetries());
        assertEquals(1, low.getMaxConsecutiveFailures());
        assertEquals(15, low.getNoProgressTimeoutSeconds());

        ReliabilitySettings high = ReliabilitySettings.of(true, 900, 900, 90_000, true);
        assertEquals(10, high.getMaxRetries());
        assertEquals(50, high.getMaxConsecutiveFailures());
        assertEquals(1800, high.getNoProgressTimeoutSeconds());
    }

    @Test
    public void readsAndClampsTheConfig()
    {
        ProjectXConfig config = new ProjectXConfig()
        {
            @Override
            public boolean reliabilityEnabled()
            {
                return false;
            }

            @Override
            public int reliabilityMaxRetries()
            {
                return 99;
            }

            @Override
            public int reliabilityMaxConsecutiveFailures()
            {
                return 7;
            }

            @Override
            public int reliabilityNoProgressTimeout()
            {
                return 5;
            }

            @Override
            public boolean reliabilityDetailedLogging()
            {
                return true;
            }
        };

        ReliabilitySettings s = ReliabilitySettings.fromConfig(config);
        assertFalse(s.isEnabled());
        assertEquals(10, s.getMaxRetries());
        assertEquals(7, s.getMaxConsecutiveFailures());
        assertEquals(15, s.getNoProgressTimeoutSeconds());
        assertTrue(s.isDetailedLogging());
    }

    @Test
    public void configDefaultsEqualTheServiceDefaults()
    {
        ReliabilitySettings s = ReliabilitySettings.fromConfig(new ProjectXConfig()
        {
        });
        assertEquals(ReliabilitySettings.DEFAULTS.toString(), s.toString());
        assertSame(ReliabilitySettings.DEFAULTS, ReliabilitySettings.fromConfig(null));
    }

    @Test
    public void updatePublishesNewSettings()
    {
        ReliabilitySettings custom = ReliabilitySettings.of(true, 1, 2, 30, true);
        ReliabilitySettings.update(custom);
        assertSame(custom, ReliabilitySettings.current());

        ReliabilitySettings.update(null);
        assertSame(ReliabilitySettings.DEFAULTS, ReliabilitySettings.current());
    }

    @Test
    public void resetKeysCoverEveryStoredSetting()
    {
        assertEquals(5, ProjectXConfig.RELIABILITY_KEYS.length);
    }
}

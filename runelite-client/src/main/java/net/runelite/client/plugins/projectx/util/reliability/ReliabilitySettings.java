package net.runelite.client.plugins.projectx.util.reliability;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectXConfig;

/**
 * The player's "Script reliability" settings, as one immutable snapshot.
 *
 * <p>The values come from {@link ProjectXConfig}; {@code ProjectXPlugin} publishes a new
 * snapshot through {@link #update} whenever one of them changes, so a running script picks
 * the change up on its next step without restarting. Every value is clamped to its range
 * when read, so a hand-edited or out-of-date profile can never make a script retry 900
 * times or stop after zero failures.
 */
public final class ReliabilitySettings
{
    public static final boolean DEFAULT_ENABLED = true;

    public static final int MIN_RETRIES = 0;
    public static final int MAX_RETRIES = 10;
    public static final int DEFAULT_RETRIES = 3;

    public static final int MIN_CONSECUTIVE_FAILURES = 1;
    public static final int MAX_CONSECUTIVE_FAILURES = 50;
    public static final int DEFAULT_CONSECUTIVE_FAILURES = 5;

    public static final int MIN_NO_PROGRESS_SECONDS = 15;
    public static final int MAX_NO_PROGRESS_SECONDS = 1800;
    public static final int DEFAULT_NO_PROGRESS_SECONDS = 120;

    public static final boolean DEFAULT_DETAILED_LOGGING = false;

    public static final ReliabilitySettings DEFAULTS = of(DEFAULT_ENABLED, DEFAULT_RETRIES,
        DEFAULT_CONSECUTIVE_FAILURES, DEFAULT_NO_PROGRESS_SECONDS, DEFAULT_DETAILED_LOGGING);

    private static volatile ReliabilitySettings current = DEFAULTS;

    private final boolean enabled;
    private final int maxRetries;
    private final int maxConsecutiveFailures;
    private final int noProgressTimeoutSeconds;
    private final boolean detailedLogging;

    private ReliabilitySettings(boolean enabled, int maxRetries, int maxConsecutiveFailures,
                                int noProgressTimeoutSeconds, boolean detailedLogging)
    {
        this.enabled = enabled;
        this.maxRetries = maxRetries;
        this.maxConsecutiveFailures = maxConsecutiveFailures;
        this.noProgressTimeoutSeconds = noProgressTimeoutSeconds;
        this.detailedLogging = detailedLogging;
    }

    /** A snapshot with every number clamped into its allowed range. */
    public static ReliabilitySettings of(boolean enabled, int maxRetries, int maxConsecutiveFailures,
                                         int noProgressTimeoutSeconds, boolean detailedLogging)
    {
        return new ReliabilitySettings(enabled,
            clamp(maxRetries, MIN_RETRIES, MAX_RETRIES),
            clamp(maxConsecutiveFailures, MIN_CONSECUTIVE_FAILURES, MAX_CONSECUTIVE_FAILURES),
            clamp(noProgressTimeoutSeconds, MIN_NO_PROGRESS_SECONDS, MAX_NO_PROGRESS_SECONDS),
            detailedLogging);
    }

    /** Reads (and clamps) the current values from the Project X config. */
    public static ReliabilitySettings fromConfig(ProjectXConfig config)
    {
        if (config == null)
        {
            return DEFAULTS;
        }
        return of(config.reliabilityEnabled(), config.reliabilityMaxRetries(),
            config.reliabilityMaxConsecutiveFailures(), config.reliabilityNoProgressTimeout(),
            config.reliabilityDetailedLogging());
    }

    /** The settings scripts use right now. */
    public static ReliabilitySettings current()
    {
        return current;
    }

    /** Publishes new settings; running scripts use them from their next step. */
    public static void update(ReliabilitySettings settings)
    {
        current = settings == null ? DEFAULTS : settings;
    }

    /**
     * Puts every "Script reliability" setting back to its default. Only these keys are
     * touched: the rest of the Project X settings, including the account token, are left alone.
     */
    public static void resetToDefaults(ConfigManager configManager)
    {
        for (String key : ProjectXConfig.RELIABILITY_KEYS)
        {
            configManager.unsetConfiguration(ProjectXConfig.configGroup, key);
        }
        update(DEFAULTS);
    }

    static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public int getMaxRetries()
    {
        return maxRetries;
    }

    public int getMaxConsecutiveFailures()
    {
        return maxConsecutiveFailures;
    }

    public int getNoProgressTimeoutSeconds()
    {
        return noProgressTimeoutSeconds;
    }

    public long getNoProgressTimeoutMillis()
    {
        return noProgressTimeoutSeconds * 1000L;
    }

    public boolean isDetailedLogging()
    {
        return detailedLogging;
    }

    @Override
    public String toString()
    {
        return "enabled=" + enabled + " maxRetries=" + maxRetries
            + " maxConsecutiveFailures=" + maxConsecutiveFailures
            + " noProgressTimeout=" + noProgressTimeoutSeconds + "s detailedLogging=" + detailedLogging;
    }
}

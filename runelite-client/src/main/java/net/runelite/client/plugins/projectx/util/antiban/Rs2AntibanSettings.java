package net.runelite.client.plugins.projectx.util.antiban;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.antiban.enums.ActivityIntensity;

/**
 * Provides configuration settings for the anti-ban system used by various plugins within the bot framework.
 *
 * <p>
 * The <code>Rs2AntibanSettings</code> class contains a collection of static fields that define behaviors
 * and settings related to anti-ban mechanisms. These settings control how the bot simulates human-like
 * behavior to avoid detection during automated tasks. Each setting adjusts a specific aspect of the
 * anti-ban system, including break patterns, mouse movements, play style variability, and other behaviors
 * designed to mimic natural human interaction with the game.
 * </p>
 *
 * <h3>Main Features:</h3>
 * <ul>
 *   <li><strong>Action Cooldowns:</strong> Controls the cooldown behavior of actions, including random intervals
 *   and non-linear patterns.</li>
 *   <li><strong>Micro Breaks:</strong> Defines settings for taking small breaks at random intervals to simulate human pauses.</li>
 *   <li><strong>Play Style Simulation:</strong> Includes variables to simulate different play styles, attention span,
 *   and behavioral variability to create a more realistic user profile.</li>
 *   <li><strong>Mouse Movements:</strong> Settings to control mouse behavior, such as moving off-screen or randomly,
 *   mimicking natural user actions.</li>
 *   <li><strong>Dynamic Behaviors:</strong> Provides options to dynamically adjust activity intensity and behavior
 *   based on context and time of day.</li>
 * </ul>
 *
 * <h3>Fields:</h3>
 * <ul>
 *   <li><code>actionCooldownActive</code>: Tracks whether action cooldowns are currently active.</li>
 *   <li><code>microBreakActive</code>: Indicates if a micro break is currently active.</li>
 *   <li><code>antibanEnabled</code>: Globally enables or disables the anti-ban system.</li>
 *   <li><code>usePlayStyle</code>: Determines whether play style simulation is active.</li>
 *   <li><code>randomIntervals</code>: Enables random intervals between actions to avoid detection.</li>
 *   <li><code>simulateFatigue</code>: Simulates user fatigue by introducing delays or slower actions.</li>
 *   <li><code>simulateAttentionSpan</code>: Simulates varying levels of user attention over time.</li>
 *   <li><code>behavioralVariability</code>: Adds variability to actions to simulate a human's inconsistency.</li>
 *   <li><code>nonLinearIntervals</code>: Activates non-linear time intervals between actions.</li>
 *   <li><code>profileSwitching</code>: Simulates user behavior switching profiles at intervals.</li>
 *   <li><code>timeOfDayAdjust</code>: Lengthens action cooldowns late at night and early in the morning (see {@link TimeOfDay}).</li>
 *   <li><code>simulateMistakes</code>: Simulates user mistakes, often controlled by natural mouse movements.</li>
 *   <li><code>naturalMouse</code>: Enables natural-looking mouse movements.</li>
 *   <li><code>moveMouseOffScreen</code>: Moves the mouse off-screen during breaks to simulate user behavior.</li>
 *   <li><code>moveMouseRandomly</code>: Moves the mouse randomly to simulate human inconsistency.</li>
 *   <li><code>contextualVariability</code>: No longer read by anything. Kept so scripts that set it still compile.</li>
 *   <li><code>dynamicIntensity</code>: Dynamically adjusts the intensity of user actions based on context.</li>
 *   <li><code>dynamicActivity</code>: Adjusts activities dynamically based on the user's behavior profile.</li>
 *   <li><code>devDebug</code>: Enables debug mode for developers to inspect the anti-ban system's state.</li>
 *   <li><code>takeMicroBreaks</code>: Controls whether the bot takes micro breaks at random intervals.</li>
 *   <li><code>playSchedule</code>: Not read by anything. The play schedule belongs to the Break Handler, which the panel edits directly.</li>
 *   <li><code>universalAntiban</code>: Applies the same anti-ban settings across all plugins.</li>
 *   <li><code>microBreakDurationLow</code>: Minimum duration for micro breaks, in minutes.</li>
 *   <li><code>microBreakDurationHigh</code>: Maximum duration for micro breaks, in minutes.</li>
 *   <li><code>actionCooldownChance</code>: Probability of triggering an action cooldown.</li>
 *   <li><code>microBreakChance</code>: Probability of taking a micro break.</li>
 *   <li><code>moveMouseRandomlyChance</code>: Probability of moving the mouse randomly.</li>
 *   <li><code>fatigueSlowdownPerHour</code>: How much slower, in percent, fatigue makes the bot per hour played.</li>
 *   <li><code>fatigueMaxSlowdown</code>: The most fatigue can slow the bot down, in percent.</li>
 *   <li><code>preferredIntensity</code>: The activity intensity picked by hand on the Mouse tab, or null.</li>
 * </ul>
 *
 * <h3>Your settings and a script's settings:</h3>
 * <p>
 * Scripts write these fields directly, so the live values are often a script's rather than the user's. The
 * user's own settings are kept separately, in memory and in the RuneLite profile, and only change through
 * {@link #userChange(Runnable)}. That separation is what lets the panel say who is in control, put the user's
 * settings back when a script stops, and enforce them over every script when
 * <code>overwriteScriptSettings</code> is on.
 * </p>
 *
 * <h3>Usage:</h3>
 * <p>
 * These settings are typically used by anti-ban mechanisms within various plugins to adjust their behavior
 * dynamically based on the user's preferences or to simulate human-like play styles. Developers can adjust
 * these fields based on the needs of their specific automation scripts.
 * </p>
 *
 * <h3>Example:</h3>
 * <pre>
 * // Enable fatigue simulation and random intervals
 * Rs2AntibanSettings.simulateFatigue = true;
 * Rs2AntibanSettings.randomIntervals = true;
 *
 * // Set the micro break chance to 20%
 * Rs2AntibanSettings.microBreakChance = 0.2;
 * </pre>
 */

@Slf4j
public class Rs2AntibanSettings {
    private static final String CONFIG_GROUP = "ProjectXAntiban";
    private static final String CONFIG_KEY = "settings";
    private static final Gson GSON = new Gson();

    public static final int FATIGUE_SLOWDOWN_PER_HOUR_DEFAULT = (int) Math.round(SessionFatigue.HOURLY_SLOWDOWN * 100);
    public static final int FATIGUE_MAX_SLOWDOWN_DEFAULT = (int) Math.round((SessionFatigue.MAX_MULTIPLIER - 1.0) * 100);

    /**
     * The user's own settings, as last chosen in the panel. Scripts write the public fields below, never this,
     * so it is the record of what the user actually asked for. Null until the plugin first loads the profile.
     */
    private static PersistentSettings userSettings;

    /** {@link #userSettings} serialised, kept so comparing it with the live values costs one toJson. */
    private static String userSettingsJson;

    /**
     * Set while a user change is being made. The change may itself call code that enforces the override --
     * a preset that sets an activity does -- and enforcing mid-change would put back the very values the user
     * is in the middle of replacing.
     */
    private static boolean applyingUserChange;

    private Rs2AntibanSettings() {
        throw new IllegalStateException("Utility class");
    }

    private static class PersistentSettings {
        private Boolean antibanEnabled;
        private Boolean usePlayStyle;
        private Boolean randomIntervals;
        private Boolean simulateFatigue;
        private Boolean simulateAttentionSpan;
        private Boolean behavioralVariability;
        private Boolean nonLinearIntervals;
        private Boolean profileSwitching;
        private Boolean timeOfDayAdjust;
        private Boolean simulateMistakes;
        private Boolean naturalMouse;
        private Boolean moveMouseOffScreen;
        private Boolean moveMouseRandomly;
        private Boolean contextualVariability;
        private Boolean dynamicIntensity;
        private Boolean dynamicActivity;
        private Boolean devDebug;
        private Boolean overwriteScriptSettings;
        private Boolean takeMicroBreaks;
        private Boolean playSchedule;
        private Boolean universalAntiban;
        private Integer microBreakDurationLow;
        private Integer microBreakDurationHigh;
        private Integer fatigueSlowdownPerHour;
        private Integer fatigueMaxSlowdown;
        private Double actionCooldownChance;
        private Double microBreakChance;
        private Double moveMouseRandomlyChance;
        private Double moveMouseOffScreenChance;
        private String preferredIntensity;
    }

    /**
     * Records the live values as the user's settings and saves them to the profile.
     *
     * <p>Only right when nothing but the user has touched the live values. The panel uses
     * {@link #userChange(Runnable)} instead, which cannot mistake a script's values for the user's.
     */
    public static synchronized void saveToProfile() {
        setUserSettings(snapshot());
        persist();
    }

    /**
     * Applies one change the user made in the panel.
     *
     * <p>The change is made to the user's own settings and saved, and also takes effect immediately. If a
     * script has its own values live at the time, those stay live with just this one change on top -- the
     * script's other values are never saved as the user's, which is what the panel used to do whenever a
     * control was touched while a script was running.
     *
     * @param edit assigns the changed field(s); runs once against the user's settings and, when a script is in
     *             control, once more against the live ones, so it must be safe to run twice
     */
    public static synchronized void userChange(Runnable edit) {
        PersistentSettings live = snapshot();
        boolean scriptInControl = userSettings != null && !GSON.toJson(live).equals(userSettingsJson);

        applyingUserChange = true;
        try {
            if (userSettings != null) {
                apply(userSettings);
            }
            edit.run();
            setUserSettings(snapshot());
            persist();

            if (scriptInControl && !isOverriding()) {
                apply(live);
                edit.run();
            }
        } finally {
            applyingUserChange = false;
        }
    }

    /**
     * Whether the live values differ from the user's own, which means a script has set its own.
     */
    public static synchronized boolean isScriptControlled() {
        return userSettingsJson != null && !GSON.toJson(snapshot()).equals(userSettingsJson);
    }

    /**
     * Whether the user has asked for their settings to win over every script's. Read from the user's own
     * settings rather than the live field, so a script cannot switch the override off by assigning it.
     */
    public static synchronized boolean isOverriding() {
        return userSettings != null && Boolean.TRUE.equals(userSettings.overwriteScriptSettings);
    }

    /**
     * Puts the user's settings back over whatever a script set. Does nothing until the profile has loaded.
     */
    public static synchronized void restoreUserSettings() {
        if (userSettings == null) {
            return;
        }
        apply(userSettings);
        applyPreferredIntensity();
    }

    /**
     * When the override is on, undoes anything a script has written since the last check.
     *
     * <p>Scripts assign these fields directly -- over sixty of them do, and only one ever checked the
     * override -- so the override cannot be a check inside the scripts. It is enforced here instead, every
     * game tick and wherever a setting is about to be used.
     */
    public static synchronized void enforceUserSettingsIfOverriding() {
        if (applyingUserChange || !isOverriding()) {
            return;
        }
        if (!GSON.toJson(snapshot()).equals(userSettingsJson)) {
            apply(userSettings);
        }
        applyPreferredIntensity();
    }

    /**
     * Loads the user's settings from the RuneLite profile and makes them live.
     *
     * <p>Values missing from the saved profile fall back to their defaults. With nothing saved at all the user's
     * settings are the defaults and the live values are left alone, so a script that calls this to layer the
     * user's choices over its own template keeps its template.
     */
    public static synchronized void loadFromProfile() {
        PersistentSettings saved = readProfile();

        PersistentSettings live = snapshot();
        apply(defaults());
        if (saved != null) {
            apply(saved);
        }
        setUserSettings(snapshot());

        if (saved == null) {
            apply(live);
        } else {
            applyPreferredIntensity();
        }
    }

    private static PersistentSettings readProfile() {
        ConfigManager configManager = ProjectX.getConfigManager();
        if (configManager == null) {
            log.debug("ConfigManager not available, skipping antiban settings load");
            return null;
        }

        String json = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            return GSON.fromJson(json, PersistentSettings.class);
        } catch (JsonSyntaxException ex) {
            log.warn("Unable to parse antiban settings from profile", ex);
            return null;
        }
    }

    private static void persist() {
        ConfigManager configManager = ProjectX.getConfigManager();
        if (configManager == null) {
            log.debug("ConfigManager not available, skipping antiban settings save");
            return;
        }

        try {
            configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY, userSettingsJson);
        } catch (Exception ex) {
            log.warn("Unable to save antiban settings to profile", ex);
        }
    }

    private static void setUserSettings(PersistentSettings settings) {
        userSettings = settings;
        userSettingsJson = GSON.toJson(settings);
    }

    /**
     * A hand-picked intensity only holds while dynamic intensity is off; with it on, the activity decides.
     */
    private static void applyPreferredIntensity() {
        if (!dynamicIntensity && preferredIntensity != null && Rs2Antiban.getActivityIntensity() != preferredIntensity) {
            Rs2Antiban.updateActivityIntensity(preferredIntensity);
        }
    }

    private static PersistentSettings snapshot() {
        PersistentSettings settings = new PersistentSettings();
        settings.antibanEnabled = antibanEnabled;
        settings.usePlayStyle = usePlayStyle;
        settings.randomIntervals = randomIntervals;
        settings.simulateFatigue = simulateFatigue;
        settings.simulateAttentionSpan = simulateAttentionSpan;
        settings.behavioralVariability = behavioralVariability;
        settings.nonLinearIntervals = nonLinearIntervals;
        settings.profileSwitching = profileSwitching;
        settings.timeOfDayAdjust = timeOfDayAdjust;
        settings.simulateMistakes = simulateMistakes;
        settings.naturalMouse = naturalMouse;
        settings.moveMouseOffScreen = moveMouseOffScreen;
        settings.moveMouseRandomly = moveMouseRandomly;
        settings.contextualVariability = contextualVariability;
        settings.dynamicIntensity = dynamicIntensity;
        settings.dynamicActivity = dynamicActivity;
        settings.devDebug = devDebug;
        settings.overwriteScriptSettings = overwriteScriptSettings;
        settings.takeMicroBreaks = takeMicroBreaks;
        settings.playSchedule = playSchedule;
        settings.universalAntiban = universalAntiban;
        settings.microBreakDurationLow = microBreakDurationLow;
        settings.microBreakDurationHigh = microBreakDurationHigh;
        settings.fatigueSlowdownPerHour = fatigueSlowdownPerHour;
        settings.fatigueMaxSlowdown = fatigueMaxSlowdown;
        settings.actionCooldownChance = actionCooldownChance;
        settings.microBreakChance = microBreakChance;
        settings.moveMouseRandomlyChance = moveMouseRandomlyChance;
        settings.moveMouseOffScreenChance = moveMouseOffScreenChance;
        settings.preferredIntensity = preferredIntensity == null ? null : preferredIntensity.name();
        return settings;
    }

    /** The one place the default values are written down; {@link #reset()} applies these. */
    private static PersistentSettings defaults() {
        PersistentSettings settings = new PersistentSettings();
        settings.antibanEnabled = true;
        settings.usePlayStyle = false;
        settings.randomIntervals = false;
        settings.simulateFatigue = false;
        settings.simulateAttentionSpan = false;
        settings.behavioralVariability = false;
        settings.nonLinearIntervals = false;
        settings.profileSwitching = false;
        settings.timeOfDayAdjust = false;
        settings.simulateMistakes = false;
        settings.naturalMouse = true;
        settings.moveMouseOffScreen = false;
        settings.moveMouseRandomly = false;
        settings.contextualVariability = false;
        settings.dynamicIntensity = false;
        settings.dynamicActivity = false;
        settings.devDebug = false;
        settings.overwriteScriptSettings = false;
        settings.takeMicroBreaks = false;
        settings.playSchedule = false;
        settings.universalAntiban = false;
        settings.microBreakDurationLow = AntibanPlugin.MICRO_BREAK_DURATION_LOW_DEFAULT;
        settings.microBreakDurationHigh = AntibanPlugin.MICRO_BREAK_DURATION_HIGH_DEFAULT;
        settings.fatigueSlowdownPerHour = FATIGUE_SLOWDOWN_PER_HOUR_DEFAULT;
        settings.fatigueMaxSlowdown = FATIGUE_MAX_SLOWDOWN_DEFAULT;
        settings.actionCooldownChance = 0.1;
        settings.microBreakChance = 0.1;
        settings.moveMouseRandomlyChance = 0.1;
        settings.moveMouseOffScreenChance = 0.1;
        settings.preferredIntensity = null;
        return settings;
    }

    private static void apply(PersistentSettings settings) {
        if (settings.antibanEnabled != null) {
            antibanEnabled = settings.antibanEnabled;
        }
        if (settings.usePlayStyle != null) {
            usePlayStyle = settings.usePlayStyle;
        }
        if (settings.randomIntervals != null) {
            randomIntervals = settings.randomIntervals;
        }
        if (settings.simulateFatigue != null) {
            simulateFatigue = settings.simulateFatigue;
        }
        if (settings.simulateAttentionSpan != null) {
            simulateAttentionSpan = settings.simulateAttentionSpan;
        }
        if (settings.behavioralVariability != null) {
            behavioralVariability = settings.behavioralVariability;
        }
        if (settings.nonLinearIntervals != null) {
            nonLinearIntervals = settings.nonLinearIntervals;
        }
        if (settings.profileSwitching != null) {
            profileSwitching = settings.profileSwitching;
        }
        if (settings.timeOfDayAdjust != null) {
            timeOfDayAdjust = settings.timeOfDayAdjust;
        }
        if (settings.simulateMistakes != null) {
            simulateMistakes = settings.simulateMistakes;
        }
        if (settings.naturalMouse != null) {
            naturalMouse = settings.naturalMouse;
        }
        if (settings.moveMouseOffScreen != null) {
            moveMouseOffScreen = settings.moveMouseOffScreen;
        }
        if (settings.moveMouseRandomly != null) {
            moveMouseRandomly = settings.moveMouseRandomly;
        }
        if (settings.contextualVariability != null) {
            contextualVariability = settings.contextualVariability;
        }
        if (settings.dynamicIntensity != null) {
            dynamicIntensity = settings.dynamicIntensity;
        }
        if (settings.dynamicActivity != null) {
            dynamicActivity = settings.dynamicActivity;
        }
        if (settings.devDebug != null) {
            devDebug = settings.devDebug;
        }
        if (settings.overwriteScriptSettings != null) {
            overwriteScriptSettings = settings.overwriteScriptSettings;
        }
        if (settings.takeMicroBreaks != null) {
            takeMicroBreaks = settings.takeMicroBreaks;
        }
        if (settings.playSchedule != null) {
            playSchedule = settings.playSchedule;
        }
        if (settings.universalAntiban != null) {
            universalAntiban = settings.universalAntiban;
        }
        if (settings.microBreakDurationLow != null) {
            microBreakDurationLow = settings.microBreakDurationLow;
        }
        if (settings.microBreakDurationHigh != null) {
            microBreakDurationHigh = settings.microBreakDurationHigh;
        }
        if (settings.fatigueSlowdownPerHour != null) {
            fatigueSlowdownPerHour = settings.fatigueSlowdownPerHour;
        }
        if (settings.fatigueMaxSlowdown != null) {
            fatigueMaxSlowdown = settings.fatigueMaxSlowdown;
        }
        if (settings.actionCooldownChance != null) {
            actionCooldownChance = settings.actionCooldownChance;
        }
        if (settings.microBreakChance != null) {
            microBreakChance = settings.microBreakChance;
        }
        if (settings.moveMouseRandomlyChance != null) {
            moveMouseRandomlyChance = settings.moveMouseRandomlyChance;
        }
        if (settings.moveMouseOffScreenChance != null) {
            moveMouseOffScreenChance = settings.moveMouseOffScreenChance;
        }
        // Null is a real value here -- "no hand-picked intensity" -- so unlike the fields above it is
        // always applied. Profiles saved before this field existed read as null, which is the default.
        preferredIntensity = parseIntensity(settings.preferredIntensity);
    }

    private static ActivityIntensity parseIntensity(String name) {
        if (name == null) {
            return null;
        }
        try {
            return ActivityIntensity.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static boolean actionCooldownActive = false;
    public static boolean microBreakActive = false;
    public static boolean antibanEnabled = true;
    public static boolean usePlayStyle = false;
    public static boolean randomIntervals = false;
    public static boolean simulateFatigue = false;
    public static boolean simulateAttentionSpan = false;
    public static boolean behavioralVariability = false;
    public static boolean nonLinearIntervals = false;
    public static boolean profileSwitching = false;
    public static boolean timeOfDayAdjust = false;
    public static boolean simulateMistakes = false; //Handled by the natural mouse
    public static boolean naturalMouse = true;
    public static boolean moveMouseOffScreen = false;
    public static boolean moveMouseRandomly = false;
    public static boolean contextualVariability = false; // Not read by anything; kept for the scripts that set it.
    public static boolean dynamicIntensity = false;
    public static boolean dynamicActivity = false;
    public static boolean devDebug = false;
    public static boolean overwriteScriptSettings = false;

    public static boolean takeMicroBreaks = false; // will take micro breaks lasting 3-15 minutes at random intervals by default.
    public static boolean playSchedule = false; // Not read by anything; the Break Handler owns the play schedule.
    public static boolean universalAntiban = false; // Will attempt to use the same antiban settings for all plugins that has not yet implemented their own antiban settings.
    public static int microBreakDurationLow = AntibanPlugin.MICRO_BREAK_DURATION_LOW_DEFAULT; // 3 minutes
    public static int microBreakDurationHigh = AntibanPlugin.MICRO_BREAK_DURATION_HIGH_DEFAULT; // 15 minutes
    public static int fatigueSlowdownPerHour = FATIGUE_SLOWDOWN_PER_HOUR_DEFAULT; // percent slower per hour played
    public static int fatigueMaxSlowdown = FATIGUE_MAX_SLOWDOWN_DEFAULT; // percent; fatigue never slows the bot more than this
    public static double actionCooldownChance = 0.1; // 10% chance of activating the action cooldown by default
    public static double microBreakChance = 0.1; // 10% chance of taking a micro break by default
    public static double moveMouseRandomlyChance = 0.1; // 10% chance of moving the mouse randomly by default
    public static double moveMouseOffScreenChance = 0.1; // 10% chance of moving the mouse off screen by default
    public static ActivityIntensity preferredIntensity = null; // picked by hand on the Mouse tab; null lets the activity decide

    /**
     * Resets every setting to its default, except {@code overwriteScriptSettings}: scripts call this through
     * {@link Rs2Antiban#resetAntibanSettings()}, and a script must not be able to switch off the user's override.
     */
    public static synchronized void reset() {
        actionCooldownActive = false;
        microBreakActive = false;
        PersistentSettings defaults = defaults();
        defaults.overwriteScriptSettings = null;
        apply(defaults);
    }

    /** Resets every setting the panel shows to its default, the override included. */
    public static synchronized void resetEverything() {
        apply(defaults());
    }
}

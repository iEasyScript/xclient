package net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2;

import java.util.Objects;

/**
 * Simple DTO representing a selectable plugin target.
 * Uses plain class form for Java 11 compatibility (no records).
 */
public final class PluginStopOption {
    public static final String NONE_VALUE = "NONE";

    private final String displayName;
    private final String className;
    private final boolean external;

    private PluginStopOption(String displayName, String className, boolean external) {
        this.displayName = displayName;
        this.className = className;
        this.external = external;
    }

    public static PluginStopOption none() {
        return new PluginStopOption("None", NONE_VALUE, false);
    }

    public static PluginStopOption builtIn(String displayName, String className) {
        return new PluginStopOption(displayName, className, false);
    }

    public static PluginStopOption external(String displayName, String className) {
        return new PluginStopOption(displayName, className, true);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getClassName() {
        return className;
    }

    public boolean isExternal() {
        return external;
    }

    public boolean isNone() {
        return NONE_VALUE.equalsIgnoreCase(className);
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginStopOption)) return false;
        PluginStopOption that = (PluginStopOption) o;
        return external == that.external &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, className, external);
    }
}

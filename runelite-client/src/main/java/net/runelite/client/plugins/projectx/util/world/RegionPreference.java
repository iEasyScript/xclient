package net.runelite.client.plugins.projectx.util.world;

import net.runelite.http.api.worlds.WorldRegion;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Where the break handler and auto login look for a world.
 *
 * Old School worlds are hosted in four countries, so a continent is a group of those
 * countries rather than a region of its own. When the chosen area has no world that
 * fits (full, or the wrong membership), the pickers fall back to any region.
 *
 * The country constants keep their old names so existing saved settings still load.
 */
public enum RegionPreference {
    ANY_REGION("Any region"),
    EUROPE("Europe (UK & Germany)", WorldRegion.UNITED_KINGDOM, WorldRegion.GERMANY),
    UNITED_STATES("America (United States)", WorldRegion.UNITED_STATES_OF_AMERICA),
    AUSTRALIA("Oceania (Australia)", WorldRegion.AUSTRALIA),
    UNITED_KINGDOM("United Kingdom", WorldRegion.UNITED_KINGDOM),
    GERMANY("Germany", WorldRegion.GERMANY);

    private final String displayName;
    private final Set<WorldRegion> worldRegions;

    RegionPreference(String displayName, WorldRegion... worldRegions) {
        this.displayName = displayName;
        this.worldRegions = worldRegions.length == 0
                ? EnumSet.noneOf(WorldRegion.class)
                : EnumSet.of(worldRegions[0], worldRegions);
    }

    /**
     * The single region, for callers that only understand one. Null for "any" and for
     * a group; use {@link #regionFilter()} to honour a group.
     */
    public WorldRegion getWorldRegion() {
        return worldRegions.size() == 1 ? worldRegions.iterator().next() : null;
    }

    public Set<WorldRegion> getWorldRegions() {
        return worldRegions;
    }

    /** Which regions count as preferred, or null when any region will do. */
    public Predicate<WorldRegion> regionFilter() {
        return worldRegions.isEmpty() ? null : worldRegions::contains;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

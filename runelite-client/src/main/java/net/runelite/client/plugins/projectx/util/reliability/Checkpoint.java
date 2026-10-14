package net.runelite.client.plugins.projectx.util.reliability;

import net.runelite.api.coords.WorldPoint;

/**
 * The last step a script knows went well, and optionally where the player was. A recovery
 * handler reads it from {@link RecoveryContext#getCheckpoint()} to resume from there, for
 * example by walking back to {@link #getLocation()} and starting that step again.
 */
public final class Checkpoint
{
    private final String name;
    private final WorldPoint location;
    private final long atMillis;

    Checkpoint(String name, WorldPoint location, long atMillis)
    {
        this.name = name;
        this.location = location;
        this.atMillis = atMillis;
    }

    public String getName()
    {
        return name;
    }

    /** Where the checkpoint was taken, or null when the script did not give one. */
    public WorldPoint getLocation()
    {
        return location;
    }

    public long getAtMillis()
    {
        return atMillis;
    }

    @Override
    public String toString()
    {
        return location == null ? name : name + " @ " + location.getX() + "," + location.getY() + "," + location.getPlane();
    }
}

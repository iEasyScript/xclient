package net.runelite.client.plugins.projectx.util.poh.data;

import net.runelite.api.coords.WorldPoint;

public interface PohTeleport {

    /**
     * Executes the teleport.
     *
     * @return true if successful, false otherwise.
     */
    boolean execute();

    WorldPoint getDestination();

    /**
     * Gets the duration of the teleport equal to the amount of tiles that can be ran in the same time.
     *
     * @return number of tiles that can be ran in the same time.
     */
    int getDuration();

    /**
     * Returns a string containing information about the PohTransport, including its Method and Destination.,
     *
     * @return PohTeleportMethod -> Destination.toString()
     */
    String displayInfo();

    /**
     * Enum value used to trace back the PohTransport from the Enum class it originates from
     *
     * @return Enum's value.name()
     */
    String name();
}

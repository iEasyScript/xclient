package net.runelite.client.plugins.projectx.util.poh.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.DecorativeObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.worldmap.TeleportLocationData;


@Getter
@RequiredArgsConstructor
public enum MountedMythical implements PohTeleport {
    MYTHS_GUILD(ObjectID.POH_TROPHY_MYTHICAL_CAPE, "Myths guild", TeleportLocationData.MYTHS_GUILD.getLocation()),
    ;

    private final int objectId;
    private final String destinationName;
    private final WorldPoint destination;

    private final int duration = 4;

    @Override
    public boolean execute() {
        DecorativeObject cape = getObject();
        if (cape == null) {
            return false;
        }
        return Rs2GameObject.interact(cape, "Teleport");
    }

    public static DecorativeObject getObject() {
        return Rs2GameObject.getDecorativeObject(MYTHS_GUILD.getObjectId());
    }

    public static boolean isMountedMythsCape(DecorativeObject go) {
        if (go == null) return false;
        return go.getId() == MYTHS_GUILD.getObjectId();
    }

    @Override
    public String displayInfo() {
        return getClass().getSimpleName() + " -> " + destinationName;
    }

}

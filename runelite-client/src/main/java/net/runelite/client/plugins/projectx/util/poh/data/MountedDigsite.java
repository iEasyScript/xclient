package net.runelite.client.plugins.projectx.util.poh.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.DecorativeObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;
import net.runelite.client.plugins.worldmap.TeleportLocationData;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;


@Getter
@RequiredArgsConstructor
public enum MountedDigsite implements PohTeleport {
    FOSSIL_ISLAND(ObjectID.POH_AMULET_DIG_FOSSIL, "Fossil Island", TeleportLocationData.HOUSE_ON_THE_HILL.getLocation()),
    DIGSITE(ObjectID.POH_AMULET_DIG_DIGSITE, "Digsite", TeleportLocationData.DIGSITE.getLocation()),
    LITHKREN(ObjectID.POH_AMULET_DIG_LITHKREN, "Lithkren", TeleportLocationData.LITHKREN.getLocation()),
    ;

    private final int objectId;
    private final String destinationName;
    private final WorldPoint destination;

    private final int duration = 4;

    @Override
    public boolean execute() {
        DecorativeObject pendant = getObject();
        if (pendant == null) {
            return false;
        }
        if (pendant.getId() == objectId) {
            // The correct id for the object means it has the right left click option, so we can just use that.
            return Rs2GameObject.interact(pendant, destinationName);
        }
        Widget widget = getWidget();
        if (widget == null) {
            if (Rs2GameObject.interact(pendant, "Teleport menu")) {
                if (!sleepUntil(() -> getWidget() != null, 10000)) {
                    return false;
                }
            }
        }
        return Rs2Widget.clickWidget(destinationName);
    }

    private static Widget getWidget() {
        return Rs2Widget.getWidget(InterfaceID.MENU, 3);
    }

    public static final Integer[] IDS = {ObjectID.POH_AMULET_DIGSITE, ObjectID.POH_AMULET_DIG_LITHKREN, ObjectID.POH_AMULET_DIG_FOSSIL, ObjectID.POH_AMULET_DIG_DIGSITE};

    public static DecorativeObject getObject() {
        return Rs2GameObject.getDecorativeObject(IDS);
    }

    public static boolean isMountedDigsite(DecorativeObject go) {
        if (go == null) return false;
        for (int objId : IDS) {
            if (objId == go.getId()) return true;
        }
        return false;
    }

    @Override
    public String displayInfo() {
        return "MountedDigsite -> " + destinationName;
    }

}

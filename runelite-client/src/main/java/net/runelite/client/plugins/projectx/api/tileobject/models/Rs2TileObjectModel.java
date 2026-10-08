package net.runelite.client.plugins.projectx.api.tileobject.models;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.projectx.util.tile.Rs2Tile;
import net.runelite.client.plugins.projectx.api.IEntity;
import net.runelite.client.plugins.projectx.api.boat.Rs2BoatCache;
import net.runelite.client.plugins.projectx.util.camera.Rs2Camera;
import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.projectx.util.menu.NewMenuEntry;
import net.runelite.client.plugins.projectx.util.misc.Rs2UiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;
import java.util.Arrays;

@Slf4j
public class Rs2TileObjectModel implements TileObject, IEntity {

    public Rs2TileObjectModel(GameObject gameObject) {
        this.tileObject = gameObject;
        this.tileObjectType = TileObjectType.GAME;
    }

    public Rs2TileObjectModel(DecorativeObject tileObject) {
        this.tileObject = tileObject;
        this.tileObjectType = TileObjectType.DECORATIVE;
    }

    public Rs2TileObjectModel(WallObject tileObject) {
        this.tileObject = tileObject;
        this.tileObjectType = TileObjectType.WALL;
    }

    public Rs2TileObjectModel(GroundObject tileObject) {
        this.tileObject = tileObject;
        this.tileObjectType = TileObjectType.GROUND;
    }

    public Rs2TileObjectModel(TileObject tileObject) {
        this.tileObject = tileObject;
        this.tileObjectType = TileObjectType.GENERIC;
    }

    @Getter
    private final TileObjectType tileObjectType;
    private final TileObject tileObject;
    private String[] actions;


    @Override
    public long getHash() {
        return tileObject.getHash();
    }

    @Override
    public int getX() {
        return tileObject.getX();
    }

    @Override
    public int getY() {
        return tileObject.getY();
    }

    @Override
    public int getZ() {
        return tileObject.getZ();
    }

    @Override
    public int getPlane() {
        return tileObject.getPlane();
    }

    @Override
    public WorldView getWorldView() {
        return tileObject.getWorldView();
    }

    public int getId() {
        return tileObject.getId();
    }

    @Override
    public @NotNull WorldPoint getWorldLocation() {
        WorldPoint worldLocation = tileObject.getWorldLocation();

        if (!(tileObject instanceof GameObject)) {
            return worldLocation;
        }

        GameObject go = (GameObject) tileObject;
        WorldView wv = getWorldView();
        Point sceneMin = go.getSceneMinLocation();

        if (wv == null || sceneMin == null) {
            return worldLocation;
        }

        return WorldPoint.fromScene(wv, sceneMin.getX(), sceneMin.getY(), wv.getPlane());
    }

    public String getName() {
        return ProjectX.getClientThread().invoke(() -> {
            ObjectComposition composition = ProjectX.getClient().getObjectDefinition(tileObject.getId());
            if (composition.getImpostorIds() != null) {
                composition = composition.getImpostor();
            }
            if (composition == null)
                return null;
            return Rs2UiHelper.stripColTags(composition.getName());
        });
    }

    @Override
    public @NotNull LocalPoint getLocalLocation() {
        return tileObject.getLocalLocation();
    }

    @Override
    public @Nullable Point getCanvasLocation() {
        return tileObject.getCanvasLocation();
    }

    @Override
    public @Nullable Point getCanvasLocation(int zOffset) {
        return tileObject.getCanvasLocation();
    }

    @Override
    public @Nullable Polygon getCanvasTilePoly() {
        return tileObject.getCanvasTilePoly();
    }

    @Override
    public @Nullable Point getCanvasTextLocation(Graphics2D graphics, String text, int zOffset) {
        return tileObject.getCanvasTextLocation(graphics, text, zOffset);
    }

    @Override
    public @Nullable Point getMinimapLocation() {
        return tileObject.getMinimapLocation();
    }

    @Override
    public @Nullable Shape getClickbox() {
        return tileObject.getClickbox();
    }

    @Override
    public @Nullable String getOpOverride(int index) {
        return tileObject.getOpOverride(index);
    }

    @Override
    public boolean isOpShown(int index) {
        return tileObject.isOpShown(index);
    }

    public ObjectComposition getObjectComposition() {
        return ProjectX.getClientThread().invoke(() -> {
            ObjectComposition composition = ProjectX.getClient().getObjectDefinition(tileObject.getId());
            if (composition.getImpostorIds() != null) {
                composition = composition.getImpostor();
            }
            return composition;
        });
    }

    @Override
    public boolean isReachable() {
        WorldView objectWorldView = getWorldView();
        if (objectWorldView == null) {
            return false;
        }

        WorldView playerWorldView = ProjectX.getClientThread().runOnClientThreadOptional(() -> {
            Player player = ProjectX.getClient().getLocalPlayer();
            return player != null ? player.getWorldView() : null;
        }).orElse(null);

        if (playerWorldView == null) {
            return false;
        }

        // Objects in different world views are not connected by walking, so there is no path.
        if (objectWorldView.getId() != playerWorldView.getId()) {
            return false;
        }

        // Ask whether a tile BESIDE this object can be stood on - not whether the object's own
        // tile can be. You interact with a booth, a tree or a rock from an adjacent tile and never
        // from the tile it occupies, so a solid object's own tile is never walkable and the
        // IEntity default (Rs2Reachable on getWorldLocation()) answers "no" for every one of them.
        // Rs2GameObject.isReachable asks the right question: it builds the object's WorldArea from
        // its size, takes the interactable tiles around it, and looks for one that is walkable and
        // reachable. Reused here rather than reimplemented so the two paths cannot drift.
        if (tileObject instanceof GameObject) {
            return Rs2GameObject.isReachable((GameObject) tileObject);
        }

        return Rs2Tile.isTileObjectReachable(tileObject);
    }

    public boolean click() {
        return click("");
    }

    /**
     * Clicks on the specified tile object with no specific action.
     * Delegates to Rs2GameObject.clickObject.
     *
     * @param action the action to perform (e.g., "Open", "Climb")
     * @return true if the interaction was successful, false otherwise
     */
    public boolean click(String action) {
        try {

            int param0;
            int param1;
            MenuAction menuAction = MenuAction.WALK;


            ProjectX.status = action + " " + getName();

            if (getTileObjectType() == TileObjectType.GAME) {
                GameObject obj = (GameObject) tileObject;
                if (obj.sizeX() > 1) {
                    param0 = obj.getLocalLocation().getSceneX() - obj.sizeX() / 2;
                } else {
                    param0 = obj.getLocalLocation().getSceneX();
                }

                if (obj.sizeY() > 1) {
                    param1 = obj.getLocalLocation().getSceneY() - obj.sizeY() / 2;
                } else {
                    param1 = obj.getLocalLocation().getSceneY();
                }
            } else {
                // Default objects like walls, groundobjects, decorationobjects etc...
                param0 = getLocalLocation().getSceneX();
                param1 = getLocalLocation().getSceneY();
            }


            // With an item or spell selected, the click is "use the selected thing on this object":
            // the menu entry is the widget's, not one of the object's own options, so there is no
            // option to look up. Read once, here, because the option check below must not fail it.
            boolean widgetSelected = ProjectX.getClient().isWidgetSelected();

            int index = 0;
            String objName = "";
            if (action != null) {
                //performance improvement to only get compoisiton if action has been specified
                var objComp = getObjectComposition();
                String[] actions;
                if (objComp.getImpostorIds() != null && objComp.getImpostor() != null) {
                    actions = objComp.getImpostor().getActions();
                } else {
                    actions = objComp.getActions();
                }

                boolean found = false;
                for (int i = 0; i < actions.length; i++) {
                    if (actions[i] == null) continue;
                    if (action.equalsIgnoreCase(Rs2UiHelper.stripColTags(actions[i]))) {
                        index = i;
                        found = true;
                        break;
                    }
                }

                /*
                 * An action that does not exist is a failure, not a reason to click something
                 * else. index starts at 0 and was only ever assigned on a match, so the
                 * "index == actions.length" guard below could never be true and the "index ==
                 * -1" warning further down could never fire: a misspelt or outdated action
                 * silently invoked menu option one instead. That is how a script ends up
                 * opening a teleport menu when it asked for a specific destination, and why
                 * these mistakes survive for so long without anything in the log.
                 *
                 * An empty action is the deliberate default-click path from click(), and still
                 * means option one. Using an item or spell on the object is not a lookup at all
                 * (see widgetSelected above); failing it here broke every "use X on object" call,
                 * such as redeeming tickets on the Wilderness Agility dispenser.
                 */
                if (!found && !action.isEmpty() && !widgetSelected) {
                    log.warn("Object {} has no \"{}\" option; it offers {}",
                            getId(), action, Arrays.toString(actions));
                    return false;
                }

                objName = objComp.getName();

                // both hands must be free before using MINECART
                if (objComp.getName().toLowerCase().contains("train cart")) {
                    Rs2Equipment.unEquip(EquipmentInventorySlot.WEAPON);
                    Rs2Equipment.unEquip(EquipmentInventorySlot.SHIELD);
                    sleepUntil(() -> Rs2Equipment.get(EquipmentInventorySlot.WEAPON) == null && Rs2Equipment.get(EquipmentInventorySlot.SHIELD) == null);
                }
            }



            if (widgetSelected) {
                menuAction = MenuAction.WIDGET_TARGET_ON_GAME_OBJECT;
            } else if (index == 0) {
                menuAction = MenuAction.GAME_OBJECT_FIRST_OPTION;
            } else if (index == 1) {
                menuAction = MenuAction.GAME_OBJECT_SECOND_OPTION;
            } else if (index == 2) {
                menuAction = MenuAction.GAME_OBJECT_THIRD_OPTION;
            } else if (index == 3) {
                menuAction = MenuAction.GAME_OBJECT_FOURTH_OPTION;
            } else if (index == 4) {
                menuAction = MenuAction.GAME_OBJECT_FIFTH_OPTION;
            }

            if (!Rs2Camera.isTileOnScreen(getLocalLocation())) {
                Rs2Camera.turnTo(tileObject);
            }


            ProjectX.doInvoke(new NewMenuEntry()
                            .param0(param0)
                            .param1(param1)
                            .opcode(menuAction.getId())
                            .identifier(getId())
                            .itemId(-1)
                            .option(action)
                            .target(objName)
                            .setWorldViewId(getWorldView().getId())
                            .gameObject(tileObject)
                    ,
                    Rs2UiHelper.getObjectClickbox(tileObject));
// MenuEntryImpl(getOption=Use, getTarget=Barrier, getIdentifier=43700, getType=GAME_OBJECT_THIRD_OPTION, getParam0=53, getParam1=51, getItemId=-1, isForceLeftClick=true, getWorldViewId=-1, isDeprioritized=false)
            //Rs2Reflection.invokeMenu(param0, param1, menuAction.getId(), object.getId(),-1, "", "", -1, -1);

        } catch (Exception ex) {
            log.error("Failed to interact with object: ", ex);
        }

        return true;
    }

}

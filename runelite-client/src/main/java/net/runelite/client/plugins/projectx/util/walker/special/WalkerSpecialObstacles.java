package net.runelite.client.plugins.projectx.util.walker.special;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.projectx.shortestpath.Transport;
import net.runelite.client.plugins.projectx.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.AreaWarning;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

/**
 * Obstacles that need more than one click: a dialogue or warning between the click and the far
 * side, or a cobweb that needs a blade. Logic ported from the DAX Web Walker (Apache-2.0, see
 * shortestpath/THIRD_PARTY_DAX.txt): PathObjectHandler's special objects and NavigationSpecialCase.
 *
 * <p>{@link #handle} runs on the script thread in place of the plain object click. It returns null
 * for objects it does not know, so the caller carries on as before.
 */
@Slf4j
public final class WalkerSpecialObstacles {

    private static final String[] NO_EXTRA = new String[0];

    /**
     * Objects whose click opens a dialogue or warning before the player moves, with the answers only
     * that object may give (the shared ones are in {@link WalkerDialogueAnswers}).
     */
    static final Map<Integer, String[]> DIALOGUE_OBSTACLES;

    static {
        Map<Integer, String[]> m = new HashMap<>();
        // Revenant caves: low and high cavern ask "don't ask again?", the mid crevice opens
        // interface 720 (DAX PathObjectHandler WILDERNESS_CAVERN, WILDERNESS_CAVERN_2, WILDERNESS_CREVICE)
        m.put(ObjectID.WILD_CAVE_ENTRANCE_LOW, NO_EXTRA);
        m.put(ObjectID.WILD_CAVE_ENTRANCE_HIGH, NO_EXTRA);
        m.put(ObjectID.WILD_CAVE_ENTRANCE_MID, NO_EXTRA);
        // Killerwatt plane rift in Draynor Manor and its way home (DAX KILLERWAT_PLANE_ENTRANCE/EXIT)
        m.put(ObjectID.DRAYNOR_KILLERWATT_PORTAL, NO_EXTRA);
        m.put(ObjectID.DRAYNOR_KILLERWATT_PORTAL_HIDDEN, NO_EXTRA);
        m.put(ObjectID.DRAYNOR_KILLERWATT_PORTAL2, NO_EXTRA);
        // Lunar Isle return orb asks to confirm (DAX LUNAR_ISLE_RETURN_ORB)
        m.put(ObjectID.FREMENNIK_DIARY_ELITE_TELEPORT, new String[]{"Yes."});
        // Khazard battlement crumbled wall: a few lines of chat before the climb (DAX KHAZARD_BATTLEMENT_*)
        m.put(ObjectID.KHAZZACKLOWWALL, NO_EXTRA);
        DIALOGUE_OBSTACLES = Collections.unmodifiableMap(m);
    }

    /** How long to keep answering after the click before handing back to the landing wait. */
    static final int DIALOGUE_DRIVE_TIMEOUT_MS = 9_000;
    /** Most dialogue steps (continues and answers) taken for one obstacle. */
    static final int DIALOGUE_DRIVE_MAX_STEPS = 10;

    private WalkerSpecialObstacles() {
    }

    /** True when {@link #handle} takes over this object. Pure. */
    public static boolean isSpecial(int objectId) {
        return objectId == ObjectID.BIGWEB_SLASHABLE || DIALOGUE_OBSTACLES.containsKey(objectId);
    }

    /**
     * Handles the click on a special object instead of the plain interact.
     *
     * @return null when the object is not special (caller clicks it as usual), else whether the
     * obstacle was handled (the caller then waits for the landing as for any object)
     */
    public static Boolean handle(Transport transport, TileObject object, String action) {
        if (transport == null || object == null) {
            return null;
        }
        int id = object.getId();
        if (id == ObjectID.BIGWEB_SLASHABLE) {
            return WebCutting.cut(object, transport.getDestination());
        }
        String[] extra = DIALOGUE_OBSTACLES.get(id);
        if (extra == null) {
            return null;
        }
        if (!Rs2GameObject.interact(object, action)) {
            log.warn("[Walker] special obstacle click failed id={} action={} at={}", id, action, object.getWorldLocation());
            return false;
        }
        driveDialogueUntilMoved(transport.getOrigin(), transport.getDestination(), extra);
        return true;
    }

    /**
     * Answers the warning or dialogue an obstacle opens until the player leaves the origin, reaches
     * the destination, or a dialogue offers nothing the walker knows. Bounded by
     * {@link #DIALOGUE_DRIVE_TIMEOUT_MS} and {@link #DIALOGUE_DRIVE_MAX_STEPS}. Script thread only.
     */
    public static void driveDialogueUntilMoved(WorldPoint origin, WorldPoint destination, String... extraAnswers) {
        long deadline = System.currentTimeMillis() + DIALOGUE_DRIVE_TIMEOUT_MS;
        int steps = 0;
        while (System.currentTimeMillis() < deadline && steps < DIALOGUE_DRIVE_MAX_STEPS) {
            if (arrived(destination)) {
                return;
            }
            boolean waiting = sleepUntil(() -> arrived(destination)
                    || Rs2Dialogue.isInDialogue()
                    || AreaWarning.isOpen(), 1800);
            if (!waiting) {
                // Nothing came up and the player has not arrived: the click is still walking the player
                // over, or the obstacle needs no answer. Leave the rest to the caller's landing wait
                // once the player is moving away from the origin.
                if (Rs2Player.isMoving() || Rs2Player.isAnimating()) {
                    continue;
                }
                return;
            }
            if (arrived(destination)) {
                return;
            }
            if (AreaWarning.acceptIfOpen()) {
                steps++;
                continue;
            }
            if (Rs2Dialogue.hasSelectAnOption()) {
                if (WalkerDialogueAnswers.answerIfOpen(extraAnswers)) {
                    steps++;
                    sleepUntil(() -> !Rs2Dialogue.hasSelectAnOption(), 1800);
                    continue;
                }
                log.warn("[Walker] obstacle dialogue has no known answer, options={} origin={}",
                        Rs2Dialogue.getDialogueOptionTexts(), origin);
                return;
            }
            if (Rs2Dialogue.hasContinue()) {
                Rs2Dialogue.clickContinue();
                steps++;
                sleepUntil(() -> !Rs2Dialogue.hasContinue() || Rs2Dialogue.hasSelectAnOption(), 1800);
                continue;
            }
            // A dialogue frame with neither options nor continue (a cutscene line): give it a moment.
            sleepUntil(() -> !Rs2Dialogue.isInDialogue() || arrived(destination), 1200);
            steps++;
        }
        if (steps >= DIALOGUE_DRIVE_MAX_STEPS) {
            log.warn("[Walker] obstacle dialogue still open after {} steps origin={} dest={}",
                    steps, origin, destination);
        }
    }

    private static boolean arrived(WorldPoint destination) {
        WorldPoint now = Rs2Player.getWorldLocation();
        return now != null && destination != null
                && now.getPlane() == destination.getPlane()
                && now.distanceTo2D(destination) <= 2;
    }
}

package net.runelite.client.plugins.projectx.util.walker.stall;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.api.boat.Rs2BoatNavigator;
import net.runelite.client.plugins.projectx.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.projectx.util.camera.Rs2Camera;
import net.runelite.client.plugins.projectx.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.projectx.util.math.Rs2Random;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.AreaWarning;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

/**
 * Fast "the walk click did nothing" detector with DAX's recovery. Ported from the DAX Web Walker
 * (Apache-2.0, see shortestpath/THIRD_PARTY_DAX.txt): MoveTaskHandler (a step fails when the player
 * stays still for {@value #STILL_MS} ms after a {@value #GRACE_MS} ms grace) and WalkerEngine
 * (raise the camera, turn it at random from the second failure, back off 450-850 ms times the
 * attempt, rebuild collision).
 *
 * <p>It never clicks: the walk loop re-clicks on its own, and this only makes the next click land
 * better (camera), later (backoff) and on fresher collision. It stands aside while the player has a
 * reason to stand still: moving, animating, interacting, in a dialogue or cutscene, on a boat, or
 * looking at an area warning. The walker's slower stall net ({@code isStuckTooLong}) is unchanged.
 */
@Slf4j
public final class WalkClickStallRecovery {

    /** Time after a click before stillness counts (DAX 1.75 s). */
    static final long GRACE_MS = 1_750L;
    /** Stillness after the grace that makes a failed step (DAX 1.25 s). */
    static final long STILL_MS = 1_250L;
    /** Backoff grows with the attempt up to this many times the base. */
    static final int MAX_BACKOFF_MULTIPLIER = 4;
    static final int BACKOFF_MIN_MS = 450;
    static final int BACKOFF_MAX_MS = 850;
    /** Failures further apart than this start over. */
    static final long FAILURE_MEMORY_MS = 30_000L;
    /** Pitch fraction the camera is raised to (top-down view sees past walls and roofs). */
    static final float RAISED_PITCH = 0.85f;

    private static volatile int failures;
    private static volatile long lastFailureAtMs;

    private WalkClickStallRecovery() {
    }

    /** Backoff for the n-th consecutive failure, given a base in [450, 850]. Pure. */
    static long backoffMs(int failureCount, int baseMs) {
        int n = Math.max(1, Math.min(failureCount, MAX_BACKOFF_MULTIPLIER));
        return (long) baseMs * n;
    }

    /** How much longer to watch after a click made {@code sinceClickMs} ago. Pure. */
    static long remainingWatchMs(long sinceClickMs) {
        return Math.max(0L, GRACE_MS + STILL_MS - sinceClickMs);
    }

    /** True while the player has a legitimate reason not to be walking. */
    static boolean legitimatelyStill() {
        return Rs2Player.isMoving()
                || Rs2Player.isAnimating()
                || Rs2Player.isInteracting()
                || Rs2Dialogue.isInDialogue()
                || Rs2Dialogue.isInCutScene()
                || Rs2BoatNavigator.isAboard()
                || AreaWarning.isOpen();
    }

    /** Forgets failures; call when the player made progress. */
    public static void onProgress() {
        failures = 0;
    }

    /**
     * Call after a walk click (and the loop's own retry) once the player has not moved. Watches out
     * the grace and stillness windows, and on a failed step applies the recovery. Script thread only.
     *
     * @param clickedAtMs when the walk click was sent
     * @param before      where the player stood when it was sent
     * @param cancelled   true when the walk was cancelled (ends any wait early)
     * @return true when the step failed and the recovery ran
     */
    public static boolean afterWalkClick(long clickedAtMs, WorldPoint before, BooleanSupplier cancelled) {
        if (before == null) {
            return false;
        }
        long remaining = remainingWatchMs(System.currentTimeMillis() - clickedAtMs);
        if (remaining > 0) {
            sleepUntil(() -> cancelled.getAsBoolean() || moved(before) || legitimatelyStill(), (int) remaining);
        }
        if (cancelled.getAsBoolean()) {
            return false;
        }
        if (moved(before)) {
            onProgress();
            return false;
        }
        if (legitimatelyStill()) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastFailureAtMs > FAILURE_MEMORY_MS) {
            failures = 0;
        }
        failures++;
        lastFailureAtMs = now;
        recover(failures, before, cancelled);
        return true;
    }

    private static boolean moved(WorldPoint before) {
        WorldPoint now = Rs2Player.getWorldLocation();
        return now != null && !Objects.equals(now, before);
    }

    private static void recover(int failureCount, WorldPoint at, BooleanSupplier cancelled) {
        long backoff = backoffMs(failureCount, Rs2Random.between(BACKOFF_MIN_MS, BACKOFF_MAX_MS));
        log.info("[Walker] walk click did not move the player at {} (failure {}): camera up{}, backoff {}ms, collision refresh",
                at, failureCount, failureCount >= 2 ? " and turned" : "", backoff);
        try {
            if (Rs2Camera.cameraPitchPercentage() < RAISED_PITCH - 0.05f) {
                Rs2Camera.adjustPitch(RAISED_PITCH);
            }
            if (failureCount >= 2) {
                Rs2Camera.setYaw(Rs2Random.between(0, 2047));
            }
        } catch (RuntimeException e) {
            log.debug("[Walker] stall camera recovery skipped: {}", e.toString());
        }
        ShortestPathPlugin.requestLiveCollisionRefresh();
        // Hold off the next click; stop early if the player starts moving or the walk ends.
        sleepUntil(() -> cancelled.getAsBoolean() || Rs2Player.isMoving(), (int) backoff);
    }
}

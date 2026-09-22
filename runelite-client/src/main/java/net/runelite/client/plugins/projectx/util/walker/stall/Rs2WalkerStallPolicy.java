package net.runelite.client.plugins.projectx.util.walker.stall;

import net.runelite.api.widgets.ComponentID;
import net.runelite.client.plugins.projectx.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.projectx.util.leaguetransport.Rs2LeaguesTransport;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

public final class Rs2WalkerStallPolicy {
    private Rs2WalkerStallPolicy() {
    }

    /**
     * Determines whether stall accounting should be bypassed for the current tick.
     * Bypasses when a leagues teleport is running, a leagues area teleport is still pending within the provided age window,
     * a dialogue is open, or the fairy-ring teleport widget is visible.
     *
     * @param leaguesPendingMaxAgeMs max age in milliseconds for treating a leagues teleport as still pending
     * @return true when stall accounting should be skipped
     */
    public static boolean shouldSkipStallAccounting(long leaguesPendingMaxAgeMs) {
        if (Rs2LeaguesTransport.isTeleportInProgress()) {
            return true;
        }
        if (Rs2LeaguesTransport.isLeaguesAreaTeleportPending(leaguesPendingMaxAgeMs)) {
            return true;
        }
        if (Rs2Dialogue.isInDialogue()) {
            return true;
        }
        return !Rs2Widget.isHidden(ComponentID.FAIRY_RING_TELEPORT_BUTTON);
    }

    /**
     * Computes the stall threshold by multiplying {@code baseMs} by the maximum applicable multiplier.
     * Result uses {@link Math#round(double)}.
     *
     * @param baseMs base stall threshold in milliseconds
     * @param combatMultiplier multiplier applied when {@code inCombat} is true
     * @param animatingMultiplier multiplier applied when {@code animating} is true
     * @param movingMultiplier multiplier applied when {@code moving} is true
     * @param interimMultiplier multiplier applied when {@code hasInterimTarget} is true
     * @param interactingMultiplier multiplier applied when {@code interactingNearPath} is true
     * @param inCombat whether player is currently in combat
     * @param animating whether player is currently animating
     * @param moving whether player is currently moving
     * @param hasInterimTarget whether walker currently tracks an interim target
     * @param interactingNearPath whether interacting with an entity near path progression
     * @return rounded threshold in milliseconds
     */
    public static long computeThresholdMs(long baseMs,
                                          double combatMultiplier,
                                          double animatingMultiplier,
                                          double movingMultiplier,
                                          double interimMultiplier,
                                          double interactingMultiplier,
                                          boolean inCombat,
                                          boolean animating,
                                          boolean moving,
                                          boolean hasInterimTarget,
                                          boolean interactingNearPath) {
        double multiplier = 1.0;
        if (inCombat) {
            multiplier = Math.max(multiplier, combatMultiplier);
        }
        if (animating) {
            multiplier = Math.max(multiplier, animatingMultiplier);
        }
        if (moving) {
            multiplier = Math.max(multiplier, movingMultiplier);
        }
        if (hasInterimTarget) {
            multiplier = Math.max(multiplier, interimMultiplier);
        }
        if (interactingNearPath) {
            multiplier = Math.max(multiplier, interactingMultiplier);
        }
        return Math.round(baseMs * multiplier);
    }
}

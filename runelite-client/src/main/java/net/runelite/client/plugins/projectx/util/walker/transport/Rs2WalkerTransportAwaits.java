package net.runelite.client.plugins.projectx.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.shared.Rs2WalkerProgress;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

public final class Rs2WalkerTransportAwaits {
    private Rs2WalkerTransportAwaits() {
    }

    public static boolean didCurrentTileTransportProgress(WorldPoint before, WorldPoint expectedDestination, WorldPoint target) {
        if (before == null) {
            return false;
        }
        sleepUntil(() -> {
            WorldPoint now = Rs2Player.getWorldLocation();
            return Rs2WalkerProgress.hasMovementOrProgress(before, now, expectedDestination, target);
        }, 1800);
        WorldPoint after = Rs2Player.getWorldLocation();
        return Rs2WalkerProgress.hasMovementOrProgress(before, after, expectedDestination, target);
    }
}

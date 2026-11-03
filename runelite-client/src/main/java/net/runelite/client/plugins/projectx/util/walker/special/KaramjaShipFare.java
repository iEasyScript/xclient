package net.runelite.client.plugins.projectx.util.walker.special;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.shortestpath.Transport;
import net.runelite.client.plugins.projectx.util.npc.Rs2Npc;
import net.runelite.client.plugins.projectx.util.npc.Rs2NpcModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The Port Sarim - Musa Point ship before Pirate's Treasure. Ported from the DAX Web Walker
 * (Apache-2.0, see shortestpath/THIRD_PARTY_DAX.txt), NavigationSpecialCase.handleKaramjaShip.
 *
 * <p>Until Pirate's Treasure is done the sailors and the customs officer lack the quick fare option,
 * so the ships.tsv action is not on the menu and the walker gave up at the dock. Then the trip is a
 * conversation: ask to journey, let the officer search you, accept the fare.
 */
@Slf4j
public final class KaramjaShipFare {

    /** NPCs of the Port Sarim - Musa Point ship rows in ships.tsv. */
    static final Set<String> FARE_NPCS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "Captain Tobias", "Seaman Lorris", "Seaman Thresnor", "Customs officer")));

    /** Pirate's Treasure progress varp; 4 and above is finished (DAX: Game.getSetting(71) >= 4). */
    static final int PIRATES_TREASURE_DONE = 4;

    private KaramjaShipFare() {
    }

    /**
     * Interacts with a ship/NPC transport, falling back to the conversation on the Karamja ship when
     * the fare option is missing. Script thread only.
     *
     * @return true when the NPC was clicked (and, for the conversation, the dialogue was driven)
     */
    public static boolean interact(Rs2NpcModel npc, Transport transport) {
        if (npc == null || transport == null) {
            return false;
        }
        if (Rs2Npc.interact(npc, transport.getAction())) {
            return true;
        }
        if (!FARE_NPCS.contains(transport.getName())) {
            return false;
        }
        int progress = ProjectX.getVarbitPlayerValue(VarPlayerID.HUNT);
        if (progress >= PIRATES_TREASURE_DONE) {
            return false;
        }
        log.info("[Walker] {} has no \"{}\" before Pirate's Treasure (varp {}={}), talking instead",
                transport.getName(), transport.getAction(), VarPlayerID.HUNT, progress);
        if (!Rs2Npc.interact(npc, "Talk-to")) {
            log.warn("[Walker] could not talk to {} for the Karamja ship", transport.getName());
            return false;
        }
        WalkerSpecialObstacles.driveDialogueUntilMoved(transport.getOrigin(), transport.getDestination(),
                "Yes please.", "Ok.");
        return true;
    }
}

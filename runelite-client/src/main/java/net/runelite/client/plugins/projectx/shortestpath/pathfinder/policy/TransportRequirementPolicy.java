package net.runelite.client.plugins.projectx.shortestpath.pathfinder.policy;

import net.runelite.api.QuestState;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.shortestpath.Transport;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.util.List;

public final class TransportRequirementPolicy {
    private TransportRequirementPolicy() {
    }

    public static boolean completedQuests(Transport transport, List<QuestState> questStateOrder) {
        return transport.getQuests().entrySet().stream()
                .allMatch(entry -> {
                    QuestState playerState = Rs2Player.getQuestState(entry.getKey());
                    QuestState requiredState = entry.getValue();
                    int playerIndex = questStateOrder.indexOf(playerState);
                    int requiredIndex = questStateOrder.indexOf(requiredState);
                    if (requiredIndex < 0 || playerIndex < 0) {
                        return false;
                    }
                    return playerIndex >= requiredIndex;
                });
    }

    public static boolean varbitChecks(Transport transport) {
        return transport.getVarbits().isEmpty()
                || transport.getVarbits().stream()
                .allMatch(varbitCheck -> varbitCheck.matches(ProjectX.getVarbitValue(varbitCheck.getVarbitId())));
    }

    public static boolean varplayerChecks(Transport transport) {
        return transport.getVarplayers().isEmpty()
                || transport.getVarplayers().stream()
                .allMatch(varplayerCheck -> varplayerCheck.matches(ProjectX.getVarbitPlayerValue(varplayerCheck.getVarplayerId())));
    }
}

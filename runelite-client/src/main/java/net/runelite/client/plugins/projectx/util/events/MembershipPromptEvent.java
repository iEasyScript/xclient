package net.runelite.client.plugins.projectx.util.events;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.BlockingEvent;
import net.runelite.client.plugins.projectx.BlockingEventPriority;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.Global;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

public class MembershipPromptEvent implements BlockingEvent {

    static final int[][] PROMPTS = {
            {InterfaceID.F2PBondRedeem.UNIVERSE, InterfaceID.F2PBondRedeem.CLOSE},
            {InterfaceID.BondPrompt.UNIVERSE, InterfaceID.BondPrompt.CLOSE},
            {InterfaceID.MembershipBenefitsPromptHop.UNIVERSE, InterfaceID.MembershipBenefitsPromptHop.CLOSE_BUTTON},
    };

    private final CachedClientCheck visible = new CachedClientCheck(() -> findCloseButton(ProjectX.getClient()) != null);

    @Override
    public boolean validate() {
        return visible.get();
    }

    @Override
    public boolean execute() {
        Widget closeButton = findCloseButton();
        if (closeButton == null) return true;

        Rs2Widget.clickWidget(closeButton);
        return Global.sleepUntil(() -> findCloseButton() == null, 10000);
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.NORMAL;
    }

    private static Widget findCloseButton() {
        if (!ProjectX.isLoggedIn()) return null;
        return ProjectX.getClientThread()
                .runOnClientThreadOptional(() -> findCloseButton(ProjectX.getClient()))
                .orElse(null);
    }

    static Widget findCloseButton(Client client) {
        if (client == null) return null;
        for (int[] prompt : PROMPTS) {
            Widget root = client.getWidget(prompt[0]);
            if (root == null || root.isHidden()) continue;
            Widget close = client.getWidget(prompt[1]);
            if (close != null && !close.isHidden()) return close;
        }
        return null;
    }
}

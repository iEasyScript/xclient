package net.runelite.client.plugins.projectx.util.events;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.BlockingEvent;
import net.runelite.client.plugins.projectx.BlockingEventPriority;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.Global;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

public class BankTutorialEvent implements BlockingEvent {

    static final String CLOSE_TEXT = "Close";

    @Override
    public boolean validate() {
        return findCloseButton() != null;
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
        return BlockingEventPriority.HIGH;
    }

    private static Widget findCloseButton() {
        if (!ProjectX.isLoggedIn()) return null;
        return ProjectX.getClientThread()
                .runOnClientThreadOptional(() -> findCloseButton(ProjectX.getClient()))
                .orElse(null);
    }

    static Widget findCloseButton(Client client) {
        if (client == null) return null;
        Widget informationBox = client.getWidget(InterfaceID.Screenhighlight.INFORMATION_BOX);
        if (informationBox == null || informationBox.isHidden()) return null;
        return Rs2Widget.searchChildren(CLOSE_TEXT, informationBox, false);
    }
}

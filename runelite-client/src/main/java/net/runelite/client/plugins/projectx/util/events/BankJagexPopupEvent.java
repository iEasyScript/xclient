package net.runelite.client.plugins.projectx.util.events;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.BlockingEvent;
import net.runelite.client.plugins.projectx.BlockingEventPriority;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.Global;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

public class BankJagexPopupEvent implements BlockingEvent {

    static final String NOT_NOW_TEXT = "Not now";

    @Override
    public boolean validate() {
        return findNotNowButton() != null;
    }

    @Override
    public boolean execute() {
        Widget notNowButton = findNotNowButton();
        if (notNowButton == null) return true;

        Rs2Widget.clickWidget(notNowButton);
        return Global.sleepUntil(() -> findNotNowButton() == null, 10000);
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.NORMAL;
    }

    private static Widget findNotNowButton() {
        if (!ProjectX.isLoggedIn()) return null;
        return ProjectX.getClientThread()
                .runOnClientThreadOptional(() -> findNotNowButton(ProjectX.getClient()))
                .orElse(null);
    }

    static Widget findNotNowButton(Client client) {
        if (client == null) return null;
        Widget popup = client.getWidget(InterfaceID.Popupoverlay.CONTAINER);
        if (popup == null || popup.isHidden()) return null;
        return Rs2Widget.searchChildren(NOT_NOW_TEXT, popup, false);
    }
}

package net.runelite.client.plugins.projectx.alerts;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** What an alert is about. Each type is rate-limited on its own. */
@Getter
@RequiredArgsConstructor
public enum AlertType
{
    SCRIPT_STOPPED("Script stopped", 0xE67E22),
    DEATH("Died", 0xC0392B),
    LEVEL_UP("Level up", 0x27AE60),
    VALUABLE_DROP("Valuable drop", 0xF1C40F),
    QUEUE_FINISHED("Script queue finished", 0x3498DB),
    LOGGED_OUT("Logged out", 0x8E44AD),
    TEST("Test alert", 0x95A5A6);

    /** Short label, used as the title when several alerts are collapsed into one. */
    private final String label;
    /** Discord embed colour, 0xRRGGBB. */
    private final int color;
}

package net.runelite.client.plugins.projectx.remote;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The status line each script's on-screen panel last showed, by panel title, so
 * the Control Panel can show the same words. Filled in by
 * {@link net.runelite.client.plugins.projectx.util.overlay.ScriptPanel#status}
 * as the panel is drawn; read from the heartbeat thread.
 */
public final class ScriptStatusBoard
{
    /** A line older than this is from a panel no longer drawn. */
    static final long FRESH_MS = 15_000;
    private static final int MAX_ENTRIES = 50;

    private static final Map<String, Entry> LINES = new ConcurrentHashMap<>();

    private ScriptStatusBoard()
    {
    }

    static final class Entry
    {
        final String title;
        final String status;
        final long at;

        Entry(String title, String status, long at)
        {
            this.title = title;
            this.status = status;
            this.at = at;
        }
    }

    /** Called every frame a panel is drawn, so it does nothing more than a map put. */
    public static void note(String title, String status)
    {
        if (title == null || status == null || status.isBlank())
        {
            return;
        }
        if (LINES.size() >= MAX_ENTRIES && !LINES.containsKey(title))
        {
            LINES.clear();
        }
        LINES.put(title, new Entry(title, status, System.currentTimeMillis()));
    }

    /**
     * The status of the panel titled after this script ("AIO Mining V 2.1" for
     * "AIO Mining"), or with only one script running and one panel showing, that
     * panel's. Null when there is none.
     */
    static String statusFor(String scriptName, int scriptsRunning, long now)
    {
        String wanted = scriptName == null ? "" : scriptName.toLowerCase(Locale.ROOT).trim();
        Entry only = null;
        int fresh = 0;
        for (Entry e : LINES.values())
        {
            if (now - e.at > FRESH_MS)
            {
                continue;
            }
            fresh++;
            only = e;
            if (!wanted.isEmpty() && e.title.toLowerCase(Locale.ROOT).startsWith(wanted))
            {
                return e.status;
            }
        }
        return scriptsRunning == 1 && fresh == 1 ? only.status : null;
    }

    static void clearForTests()
    {
        LINES.clear();
    }
}

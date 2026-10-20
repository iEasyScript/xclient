package net.runelite.client.plugins.projectx.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The live state this client sends the Bots page in each heartbeat, built from
 * plain inputs so it can be tested without a game.
 *
 * <p>Deliberately small: what is running and how it is going, never where the
 * player is standing, and never anything from settings, the account token or a
 * webhook. Free text (script status lines, queue descriptions, plan names) comes
 * from scripts, so anything in it that looks like a token or a link is blanked.
 * The whole thing is kept under {@link #MAX_BYTES}; the site refuses more.
 */
public final class RemoteState
{
    /** A little under the site's 16 KB cap, so rounding on either side never matters. */
    static final int MAX_BYTES = 15_000;
    static final int MAX_SCRIPTS = 10;
    static final int MAX_PLANS = 30;

    private static final Pattern TOKEN = Pattern.compile("px_[A-Za-z0-9_-]{6,}");
    private static final Pattern LINK = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}]");
    private static final Pattern DISPLAY_NAME = Pattern.compile("[A-Za-z0-9 _-]{1,12}");

    private RemoteState()
    {
    }

    @Value
    @Builder
    public static class Run
    {
        String name;
        String internalName;
        String version;
        long startedAt;
        /** Skill name (MINING) to experience gained this run. */
        @Singular("xp")
        Map<String, Integer> xpGained;
        String status;
    }

    @Value
    @Builder
    public static class Queue
    {
        boolean running;
        String plan;
        /** From 1; 0 when not known. */
        int step;
        int steps;
        String description;
        @Singular("planName")
        List<String> plans;
    }

    @Value
    @Builder
    public static class Inputs
    {
        String clientVersion;
        /** Null before the game has been read at all. */
        GameSnapshot game;
        boolean shareName;
        @Singular
        List<Run> runs;
        boolean breakActive;
        /** Epoch millis; 0 when not known. */
        long breakUntil;
        /** "v1", "v2" or null. */
        String breakHandler;
        Queue queue;
    }

    /** The state, already cut to fit. */
    public static JsonObject build(Inputs in, long now)
    {
        JsonObject json = full(in, now);
        if (bytes(json) <= MAX_BYTES)
        {
            return json;
        }
        // Too big: give up the least useful parts first.
        for (int round = 0; round < 4 && bytes(json) > MAX_BYTES; round++)
        {
            shrink(json, round);
        }
        return json;
    }

    private static JsonObject full(Inputs in, long now)
    {
        JsonObject json = new JsonObject();
        json.addProperty("v", 1);
        json.addProperty("clientVersion", clip(in.getClientVersion(), 40));

        GameSnapshot g = in.getGame() == null ? GameSnapshot.LOGGED_OUT : in.getGame();
        json.addProperty("loggedIn", g.isLoggedIn());
        if (g.isLoggedIn())
        {
            if (g.getWorld() > 0)
            {
                json.addProperty("world", g.getWorld());
            }
            json.addProperty("members", g.isMembers());
            if (in.isShareName())
            {
                String name = displayName(g.getName());
                if (name != null)
                {
                    json.addProperty("name", name);
                }
            }
            if (g.getTotalLevel() > 0)
            {
                json.addProperty("totalLevel", g.getTotalLevel());
            }
            if (g.getHpPercent() >= 0)
            {
                json.addProperty("hp", g.getHpPercent());
            }
            if (g.getPrayerPercent() >= 0)
            {
                json.addProperty("prayer", g.getPrayerPercent());
            }
        }

        JsonArray scripts = new JsonArray();
        List<Run> runs = in.getRuns();
        for (int i = 0; i < runs.size() && i < MAX_SCRIPTS; i++)
        {
            scripts.add(run(runs.get(i), now));
        }
        json.add("scripts", scripts);

        JsonObject brk = new JsonObject();
        brk.addProperty("active", in.isBreakActive());
        if (in.isBreakActive() && in.getBreakUntil() > now)
        {
            brk.addProperty("until", in.getBreakUntil());
        }
        else
        {
            brk.add("until", JsonNull.INSTANCE);
        }
        if (in.getBreakHandler() != null)
        {
            brk.addProperty("handler", in.getBreakHandler());
        }
        json.add("break", brk);

        Queue q = in.getQueue();
        if (q != null)
        {
            JsonObject queue = new JsonObject();
            queue.addProperty("running", q.isRunning());
            if (q.isRunning())
            {
                queue.addProperty("plan", clean(q.getPlan(), 60));
                if (q.getStep() > 0 && q.getSteps() > 0)
                {
                    queue.addProperty("step", q.getStep());
                    queue.addProperty("steps", q.getSteps());
                }
                queue.addProperty("description", clean(q.getDescription(), 200));
            }
            JsonArray plans = new JsonArray();
            for (String p : q.getPlans())
            {
                String name = clean(p, 60);
                if (name != null && plans.size() < MAX_PLANS)
                {
                    plans.add(name);
                }
            }
            queue.add("plans", plans);
            json.add("queue", queue);
        }
        return json;
    }

    private static JsonObject run(Run r, long now)
    {
        JsonObject json = new JsonObject();
        json.addProperty("name", clean(r.getName(), 120));
        json.addProperty("internalName", clip(r.getInternalName(), 80));
        json.addProperty("version", clip(r.getVersion(), 40));
        json.addProperty("startedAt", r.getStartedAt());
        json.addProperty("status", clean(r.getStatus(), 160));

        long total = 0;
        String best = null;
        int bestXp = 0;
        for (Map.Entry<String, Integer> e : r.getXpGained().entrySet())
        {
            int xp = e.getValue() == null ? 0 : Math.max(0, e.getValue());
            total += xp;
            if (xp > bestXp)
            {
                bestXp = xp;
                best = e.getKey();
            }
        }
        json.addProperty("xp", total);
        json.addProperty("xpPerHour", perHour(total, now - r.getStartedAt()));
        if (best != null)
        {
            json.addProperty("skill", best);
        }
        return json;
    }

    /** Experience an hour; nothing until a minute has passed, when the rate is still mostly noise. */
    static long perHour(long xp, long elapsedMs)
    {
        if (elapsedMs < 60_000 || xp <= 0)
        {
            return 0;
        }
        return Math.round(xp * 3_600_000d / elapsedMs);
    }

    private static void shrink(JsonObject json, int round)
    {
        JsonArray scripts = json.getAsJsonArray("scripts");
        JsonObject queue = json.has("queue") ? json.getAsJsonObject("queue") : null;
        switch (round)
        {
            case 0:
                for (int i = 0; i < scripts.size(); i++)
                {
                    scripts.get(i).getAsJsonObject().remove("status");
                }
                if (queue != null)
                {
                    queue.remove("description");
                }
                break;
            case 1:
                if (queue != null)
                {
                    queue.add("plans", new JsonArray());
                }
                break;
            case 2:
                while (scripts.size() > 3)
                {
                    scripts.remove(scripts.size() - 1);
                }
                break;
            default:
                json.add("scripts", new JsonArray());
                json.remove("queue");
                break;
        }
    }

    static int bytes(JsonObject json)
    {
        return json.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    /** OSRS names use a no-break space where the player typed a space. */
    static String displayName(String raw)
    {
        if (raw == null)
        {
            return null;
        }
        String name = raw.replace(' ', ' ').trim();
        return DISPLAY_NAME.matcher(name).matches() ? name : null;
    }

    /** Script-written text: tokens and links blanked, control characters gone, cut to size. */
    static String clean(String text, int max)
    {
        if (text == null)
        {
            return null;
        }
        String t = text.replaceAll("<[^>]*>", "");
        t = TOKEN.matcher(t).replaceAll("[hidden]");
        t = LINK.matcher(t).replaceAll("[link]");
        t = CONTROL.matcher(t).replaceAll(" ").replaceAll("\\s+", " ").trim();
        return clip(t, max);
    }

    private static String clip(String text, int max)
    {
        if (text == null || text.isEmpty())
        {
            return null;
        }
        return text.length() > max ? text.substring(0, max) : text;
    }
}

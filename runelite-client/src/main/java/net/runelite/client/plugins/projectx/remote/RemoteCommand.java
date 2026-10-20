package net.runelite.client.plugins.projectx.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * One thing the Bots page asked this client to do, as read from a heartbeat
 * answer. A fixed set of actions, each naming at most a script or a plan: there
 * is nothing here that could carry code or a path.
 */
@Value
public class RemoteCommand
{
    public enum Type
    {
        START_SCRIPT,
        STOP_SCRIPT,
        STOP_ALL,
        QUEUE_START,
        QUEUE_STOP,
        BREAK_START,
        BREAK_END,
        LOGOUT,
        /** Sent by a newer site; answered with "update the client". */
        UNKNOWN
    }

    /** At most this many from one answer; the site never sends more than five. */
    static final int MAX_PER_ANSWER = 10;

    private static final Pattern ID = Pattern.compile("[a-z0-9]{10,40}");
    private static final Pattern INTERNAL_NAME = Pattern.compile("[A-Za-z0-9_]{2,80}");

    String id;
    Type type;
    /** START_SCRIPT and STOP_SCRIPT: the plugin's class name, as the store lists it. */
    String internalName;
    /** QUEUE_START: the plan, or null for the one showing. */
    String plan;

    /**
     * The commands in a heartbeat answer, skipping anything without a usable id.
     * A command with an id but a bad script name or an unknown type is kept, so
     * it can be answered with a failure rather than left to expire.
     */
    public static List<RemoteCommand> parseAll(JsonObject answer)
    {
        List<RemoteCommand> out = new ArrayList<>();
        if (answer == null || !answer.has("commands") || !answer.get("commands").isJsonArray())
        {
            return out;
        }
        JsonArray commands = answer.getAsJsonArray("commands");
        for (JsonElement e : commands)
        {
            if (out.size() >= MAX_PER_ANSWER)
            {
                break;
            }
            RemoteCommand c = parse(e);
            if (c != null)
            {
                out.add(c);
            }
        }
        return out;
    }

    static RemoteCommand parse(JsonElement element)
    {
        if (element == null || !element.isJsonObject())
        {
            return null;
        }
        JsonObject o = element.getAsJsonObject();
        String id = string(o, "id");
        if (id == null || !ID.matcher(id).matches())
        {
            return null;
        }
        Type type = Type.UNKNOWN;
        String raw = string(o, "type");
        if (raw != null)
        {
            for (Type t : Type.values())
            {
                if (t != Type.UNKNOWN && t.name().equals(raw))
                {
                    type = t;
                }
            }
        }
        String internalName = string(o, "internalName");
        if (internalName != null && !INTERNAL_NAME.matcher(internalName).matches())
        {
            internalName = null;
        }
        String plan = string(o, "plan");
        if (plan != null)
        {
            plan = plan.trim();
            if (plan.isEmpty() || plan.length() > 60)
            {
                plan = null;
            }
        }
        return new RemoteCommand(id, type, internalName, plan);
    }

    private static String string(JsonObject o, String key)
    {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    /** For the log: the action and what it names, nothing else. */
    public String describe()
    {
        if (internalName != null)
        {
            return type + " " + internalName;
        }
        if (plan != null)
        {
            return type + " (" + plan + ")";
        }
        return type.toString();
    }
}

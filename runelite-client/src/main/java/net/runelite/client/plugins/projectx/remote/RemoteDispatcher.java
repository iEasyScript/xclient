package net.runelite.client.plugins.projectx.remote;

import com.google.gson.JsonObject;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Carries out one website command and says how it went. Checks, in order: that
 * the command has not been run already (a heartbeat answer can be read twice),
 * that control from the website is on, and that the command names what it
 * needs. Then hands it to {@link RemoteActions}.
 *
 * <p>Logs every command it runs at INFO: the action, the script or plan it names
 * and the outcome. Nothing else is in a command to log.
 */
@Slf4j
public class RemoteDispatcher
{
    /** Exactly the words the Control Panel shows. */
    static final String CONTROL_OFF = "remote control is off in this client";
    private static final int REMEMBERED_IDS = 200;

    private final BooleanSupplier controlAllowed;
    private final RemoteActions actions;

    /** Ids already run, oldest first, so a repeat is answered without being run twice. */
    private final Set<String> seen = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>()
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest)
        {
            return size() > REMEMBERED_IDS;
        }
    });

    public RemoteDispatcher(BooleanSupplier controlAllowed, RemoteActions actions)
    {
        this.controlAllowed = controlAllowed;
        this.actions = actions;
    }

    /** How a command went, for the next heartbeat. */
    @Value
    public static class Ack
    {
        String id;
        boolean ok;
        String message;

        public JsonObject toJson()
        {
            JsonObject json = new JsonObject();
            json.addProperty("id", id);
            json.addProperty("ok", ok);
            json.addProperty("message", message);
            return json;
        }
    }

    /** Whether this command is new. Marks it as seen, so call once per command. */
    public synchronized boolean firstTime(RemoteCommand command)
    {
        return seen.add(command.getId());
    }

    /** Runs a command and returns its answer. Never throws. */
    public Ack run(RemoteCommand command)
    {
        Ack ack = attempt(command);
        log.info("Website command {}: {}{}", command.describe(), ack.isOk() ? "done" : "failed",
            ack.getMessage() == null ? "" : " (" + ack.getMessage() + ")");
        return ack;
    }

    private Ack attempt(RemoteCommand c)
    {
        if (!controlAllowed.getAsBoolean())
        {
            return fail(c, CONTROL_OFF);
        }
        try
        {
            switch (c.getType())
            {
                case START_SCRIPT:
                    return done(c, actions.startScript(needName(c)));
                case STOP_SCRIPT:
                    return done(c, actions.stopScript(needName(c)));
                case STOP_ALL:
                    return done(c, actions.stopAll());
                case QUEUE_START:
                    return done(c, actions.startQueue(c.getPlan()));
                case QUEUE_STOP:
                    return done(c, actions.stopQueue());
                case BREAK_START:
                    return done(c, actions.startBreak());
                case BREAK_END:
                    return done(c, actions.endBreak());
                case LOGOUT:
                    return done(c, actions.logout());
                default:
                    return fail(c, "this client doesn't know that command; update the client");
            }
        }
        catch (RemoteActions.Refused e)
        {
            return fail(c, e.getMessage());
        }
        catch (RuntimeException e)
        {
            log.warn("Website command {} failed", c.describe(), e);
            return fail(c, "something went wrong in the client");
        }
    }

    private static String needName(RemoteCommand c) throws RemoteActions.Refused
    {
        if (c.getInternalName() == null)
        {
            throw new RemoteActions.Refused("no script was named");
        }
        return c.getInternalName();
    }

    private static Ack done(RemoteCommand c, String message)
    {
        return new Ack(c.getId(), true, message);
    }

    private static Ack fail(RemoteCommand c, String message)
    {
        return new Ack(c.getId(), false, message);
    }
}

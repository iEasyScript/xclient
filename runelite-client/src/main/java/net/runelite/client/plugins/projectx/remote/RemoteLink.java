package net.runelite.client.plugins.projectx.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXInstances;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Website control's part of the instance heartbeat.
 *
 * <p>Each beat carries this client's settings, its live state (when monitoring is
 * on) and the answers to commands it has run. Each answer can carry new commands;
 * they are run one at a time on their own thread, never the heartbeat's, the
 * client thread or the Swing thread, and their answers go back with the next beat,
 * which is brought forward so the website hears quickly.
 *
 * <p>An answer stays queued until a beat carrying it has been accepted, so a beat
 * lost to the network does not lose it.
 */
@Slf4j
public class RemoteLink implements ProjectXInstances.Extension
{
    /** The protocol this client speaks; the site sends commands only to clients that send one. */
    static final int PROTOCOL = 1;
    private static final int ACKS_PER_BEAT = 20;
    private static final int MAX_QUEUED_ACKS = 50;

    private final BooleanSupplier control;
    private final BooleanSupplier monitoring;
    private final Supplier<JsonObject> state;
    private final RemoteDispatcher dispatcher;
    private final Executor commands;
    private final Runnable beatSoon;

    private final ConcurrentLinkedDeque<RemoteDispatcher.Ack> acks = new ConcurrentLinkedDeque<>();
    /** The answers in the beat on its way; dropped from the queue once the site has it. */
    private volatile List<RemoteDispatcher.Ack> inFlight = Collections.emptyList();

    public RemoteLink(BooleanSupplier control, BooleanSupplier monitoring, Supplier<JsonObject> state,
                      RemoteDispatcher dispatcher, Executor commands, Runnable beatSoon)
    {
        this.control = control;
        this.monitoring = monitoring;
        this.state = state;
        this.dispatcher = dispatcher;
        this.commands = commands;
        this.beatSoon = beatSoon;
    }

    @Override
    public void addTo(JsonObject body)
    {
        JsonObject remote = new JsonObject();
        remote.addProperty("v", PROTOCOL);
        remote.addProperty("control", control.getAsBoolean());
        remote.addProperty("monitor", monitoring.getAsBoolean());
        body.add("remote", remote);

        if (monitoring.getAsBoolean())
        {
            try
            {
                JsonObject live = state.get();
                if (live != null)
                {
                    body.add("state", live);
                }
            }
            catch (RuntimeException e)
            {
                // A beat without details beats no beat.
                log.debug("Live state not built: {}", e.toString());
            }
        }

        List<RemoteDispatcher.Ack> sending = new ArrayList<>();
        for (RemoteDispatcher.Ack a : acks)
        {
            if (sending.size() >= ACKS_PER_BEAT)
            {
                break;
            }
            sending.add(a);
        }
        inFlight = sending;
        if (!sending.isEmpty())
        {
            JsonArray array = new JsonArray();
            sending.forEach(a -> array.add(a.toJson()));
            body.add("acks", array);
        }
    }

    @Override
    public void answered(JsonObject answer)
    {
        List<RemoteDispatcher.Ack> delivered = inFlight;
        inFlight = Collections.emptyList();
        acks.removeAll(delivered);

        for (RemoteCommand command : RemoteCommand.parseAll(answer))
        {
            if (!dispatcher.firstTime(command))
            {
                continue;
            }
            commands.execute(() ->
            {
                RemoteDispatcher.Ack ack = dispatcher.run(command);
                acks.add(ack);
                while (acks.size() > MAX_QUEUED_ACKS)
                {
                    acks.pollFirst();
                }
                beatSoon.run();
            });
        }
    }

    @Override
    public boolean wantsLiveHeartbeat()
    {
        return control.getAsBoolean() || monitoring.getAsBoolean();
    }

    /** Answers not yet accepted by the site, oldest first. For tests. */
    List<RemoteDispatcher.Ack> queuedAcks()
    {
        return new ArrayList<>(acks);
    }
}

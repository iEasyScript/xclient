package net.runelite.client.plugins.projectx.remote;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RemoteLinkTest
{
    private RemoteDispatcherTest.FakeActions actions;
    private boolean control;
    private boolean monitoring;
    private AtomicInteger beats;
    private AtomicInteger stateBuilt;
    private RemoteLink link;

    @Before
    public void setUp()
    {
        actions = new RemoteDispatcherTest.FakeActions();
        control = true;
        monitoring = true;
        beats = new AtomicInteger();
        stateBuilt = new AtomicInteger();
        RemoteDispatcher dispatcher = new RemoteDispatcher(() -> control, actions);
        link = new RemoteLink(() -> control, () -> monitoring, () ->
        {
            stateBuilt.incrementAndGet();
            JsonObject s = new JsonObject();
            s.addProperty("loggedIn", true);
            return s;
        }, dispatcher, Runnable::run, beats::incrementAndGet);
    }

    private static JsonObject json(String s)
    {
        return new Gson().fromJson(s, JsonObject.class);
    }

    private JsonObject beat()
    {
        JsonObject body = new JsonObject();
        link.addTo(body);
        return body;
    }

    @Test
    public void saysWhatItSpeaksAndWhatIsAllowed()
    {
        JsonObject remote = beat().getAsJsonObject("remote");
        assertEquals(RemoteLink.PROTOCOL, remote.get("v").getAsInt());
        assertTrue(remote.get("control").getAsBoolean());
        assertTrue(remote.get("monitor").getAsBoolean());

        control = false;
        assertFalse(beat().getAsJsonObject("remote").get("control").getAsBoolean());
    }

    @Test
    public void sendsStateOnlyWithMonitoringOn()
    {
        assertTrue(beat().has("state"));
        monitoring = false;
        assertFalse(beat().has("state"));
        assertEquals(1, stateBuilt.get());
    }

    @Test
    public void heartbeatsFastOnlyWhileSomethingIsOn()
    {
        assertTrue(link.wantsLiveHeartbeat());
        control = false;
        assertTrue(link.wantsLiveHeartbeat());
        monitoring = false;
        assertFalse(link.wantsLiveHeartbeat());
    }

    @Test
    public void runsCommandsAndAnswersInTheNextBeat()
    {
        link.answered(json("{\"commands\":[{\"id\":\"cmabcdefghij1\",\"type\":\"START_SCRIPT\",\"internalName\":\"AutoMiningPlugin\"}]}"));

        assertEquals(1, actions.calls.size());
        // The answer is sent soon rather than at the next tick.
        assertEquals(1, beats.get());
        JsonObject body = beat();
        assertEquals(1, body.getAsJsonArray("acks").size());
        JsonObject ack = body.getAsJsonArray("acks").get(0).getAsJsonObject();
        assertEquals("cmabcdefghij1", ack.get("id").getAsString());
        assertTrue(ack.get("ok").getAsBoolean());
    }

    /** An answer stays queued until a beat carrying it is accepted. */
    @Test
    public void keepsAnswersUntilTheSiteHasThem()
    {
        link.answered(json("{\"commands\":[{\"id\":\"cmabcdefghij1\",\"type\":\"STOP_ALL\"}]}"));

        beat();
        // That beat was lost: no answer came back. The next one carries it again.
        assertEquals(1, beat().getAsJsonArray("acks").size());
        link.answered(json("{\"ok\":true}"));

        assertFalse(beat().has("acks"));
        assertTrue(link.queuedAcks().isEmpty());
    }

    @Test
    public void aCommandHandedOverTwiceRunsOnce()
    {
        String answer = "{\"commands\":[{\"id\":\"cmabcdefghij1\",\"type\":\"STOP_ALL\"}]}";
        link.answered(json(answer));
        link.answered(json(answer));
        assertEquals(1, actions.calls.size());
    }

    @Test
    public void controlOffAnswersEveryCommandWithoutRunningIt()
    {
        control = false;
        link.answered(json("{\"commands\":[{\"id\":\"cmabcdefghij1\",\"type\":\"LOGOUT\"}]}"));

        assertTrue(actions.calls.isEmpty());
        JsonObject ack = beat().getAsJsonArray("acks").get(0).getAsJsonObject();
        assertFalse(ack.get("ok").getAsBoolean());
        assertEquals("remote control is off in this client", ack.get("message").getAsString());
    }

    /** A broken state builder costs the details, never the heartbeat. */
    @Test
    public void aFailingStateLeavesTheBeatAlone()
    {
        RemoteLink broken = new RemoteLink(() -> true, () -> true, () ->
        {
            throw new IllegalStateException("no game");
        }, new RemoteDispatcher(() -> true, actions), Runnable::run, () -> { });
        JsonObject body = new JsonObject();
        broken.addTo(body);
        assertTrue(body.has("remote"));
        assertFalse(body.has("state"));
    }
}

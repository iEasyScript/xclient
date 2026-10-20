package net.runelite.client.plugins.projectx.remote;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RemoteDispatcherTest
{
    /** Records what it was asked, and refuses or blows up when told to. */
    static final class FakeActions implements RemoteActions
    {
        final List<String> calls = new ArrayList<>();
        String refuse;
        boolean explode;

        private String call(String what) throws Refused
        {
            calls.add(what);
            if (explode)
            {
                throw new IllegalStateException("boom: px_secret_token");
            }
            if (refuse != null)
            {
                throw new Refused(refuse);
            }
            return "did " + what;
        }

        @Override
        public String startScript(String internalName) throws Refused
        {
            return call("start " + internalName);
        }

        @Override
        public String stopScript(String internalName) throws Refused
        {
            return call("stop " + internalName);
        }

        @Override
        public String stopAll() throws Refused
        {
            return call("stopAll");
        }

        @Override
        public String startQueue(String plan) throws Refused
        {
            return call("queue " + plan);
        }

        @Override
        public String stopQueue() throws Refused
        {
            return call("stopQueue");
        }

        @Override
        public String startBreak() throws Refused
        {
            return call("break");
        }

        @Override
        public String endBreak() throws Refused
        {
            return call("endBreak");
        }

        @Override
        public String logout() throws Refused
        {
            return call("logout");
        }
    }

    private FakeActions actions;
    private boolean control;
    private RemoteDispatcher dispatcher;

    @Before
    public void setUp()
    {
        actions = new FakeActions();
        control = true;
        dispatcher = new RemoteDispatcher(() -> control, actions);
    }

    private static RemoteCommand command(RemoteCommand.Type type, String internalName, String plan)
    {
        return new RemoteCommand("cmabcdefghij1", type, internalName, plan);
    }

    @Test
    public void sendsEachCommandToItsAction()
    {
        assertEquals("did start AutoMiningPlugin",
            dispatcher.run(command(RemoteCommand.Type.START_SCRIPT, "AutoMiningPlugin", null)).getMessage());
        dispatcher.run(command(RemoteCommand.Type.STOP_SCRIPT, "AutoMiningPlugin", null));
        dispatcher.run(command(RemoteCommand.Type.STOP_ALL, null, null));
        dispatcher.run(command(RemoteCommand.Type.QUEUE_START, null, "Night"));
        dispatcher.run(command(RemoteCommand.Type.QUEUE_START, null, null));
        dispatcher.run(command(RemoteCommand.Type.QUEUE_STOP, null, null));
        dispatcher.run(command(RemoteCommand.Type.BREAK_START, null, null));
        dispatcher.run(command(RemoteCommand.Type.BREAK_END, null, null));
        dispatcher.run(command(RemoteCommand.Type.LOGOUT, null, null));

        assertEquals(List.of("start AutoMiningPlugin", "stop AutoMiningPlugin", "stopAll", "queue Night", "queue null",
            "stopQueue", "break", "endBreak", "logout"), actions.calls);
    }

    @Test
    public void answersWithTheCommandsId()
    {
        RemoteDispatcher.Ack ack = dispatcher.run(command(RemoteCommand.Type.STOP_ALL, null, null));
        assertEquals("cmabcdefghij1", ack.getId());
        assertTrue(ack.isOk());
        assertEquals("cmabcdefghij1", ack.toJson().get("id").getAsString());
        assertTrue(ack.toJson().get("ok").getAsBoolean());
    }

    /** With control off nothing runs, whatever was asked, and the website is told why in those words. */
    @Test
    public void controlOffRefusesEverything()
    {
        control = false;
        for (RemoteCommand.Type type : RemoteCommand.Type.values())
        {
            RemoteDispatcher.Ack ack = dispatcher.run(command(type, "AutoMiningPlugin", "Night"));
            assertFalse(ack.isOk());
            assertEquals("remote control is off in this client", ack.getMessage());
        }
        assertTrue(actions.calls.isEmpty());
    }

    /** Turning control off takes effect for the very next command. */
    @Test
    public void controlIsReadForEachCommand()
    {
        assertTrue(dispatcher.run(command(RemoteCommand.Type.STOP_ALL, null, null)).isOk());
        control = false;
        assertFalse(dispatcher.run(command(RemoteCommand.Type.STOP_ALL, null, null)).isOk());
        assertEquals(1, actions.calls.size());
    }

    @Test
    public void aRefusalIsPassedOn()
    {
        actions.refuse = "missing requirements: Rune pickaxe";
        RemoteDispatcher.Ack ack = dispatcher.run(command(RemoteCommand.Type.START_SCRIPT, "AutoMiningPlugin", null));
        assertFalse(ack.isOk());
        assertEquals("missing requirements: Rune pickaxe", ack.getMessage());
    }

    /** An exception's text could hold anything; the website only hears that it went wrong. */
    @Test
    public void aCrashIsAPlainFailure()
    {
        actions.explode = true;
        RemoteDispatcher.Ack ack = dispatcher.run(command(RemoteCommand.Type.STOP_ALL, null, null));
        assertFalse(ack.isOk());
        assertEquals("something went wrong in the client", ack.getMessage());
        assertFalse(ack.toJson().toString().contains("px_secret"));
    }

    @Test
    public void scriptCommandsNeedAScript()
    {
        assertFalse(dispatcher.run(command(RemoteCommand.Type.START_SCRIPT, null, null)).isOk());
        assertFalse(dispatcher.run(command(RemoteCommand.Type.STOP_SCRIPT, null, null)).isOk());
        assertTrue(actions.calls.isEmpty());
    }

    @Test
    public void unknownCommandsAskForAnUpdate()
    {
        RemoteDispatcher.Ack ack = dispatcher.run(command(RemoteCommand.Type.UNKNOWN, null, null));
        assertFalse(ack.isOk());
        assertTrue(ack.getMessage().contains("update"));
    }

    @Test
    public void eachIdIsNewOnlyOnce()
    {
        RemoteCommand c = command(RemoteCommand.Type.STOP_ALL, null, null);
        assertTrue(dispatcher.firstTime(c));
        assertFalse(dispatcher.firstTime(c));
        assertTrue(dispatcher.firstTime(new RemoteCommand("cmabcdefghij2", RemoteCommand.Type.STOP_ALL, null, null)));
    }
}

package net.runelite.client.plugins.projectx.remote;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RemoteCommandTest
{
    private static List<RemoteCommand> parse(String json)
    {
        return RemoteCommand.parseAll(new Gson().fromJson(json, JsonObject.class));
    }

    @Test
    public void readsEachKindOfCommand()
    {
        List<RemoteCommand> commands = parse("{\"commands\":["
            + "{\"id\":\"cmabcdefghij1\",\"type\":\"START_SCRIPT\",\"internalName\":\"AutoMiningPlugin\"},"
            + "{\"id\":\"cmabcdefghij2\",\"type\":\"QUEUE_START\",\"plan\":\" Morning \"},"
            + "{\"id\":\"cmabcdefghij3\",\"type\":\"STOP_ALL\"}]}");

        assertEquals(3, commands.size());
        assertEquals(RemoteCommand.Type.START_SCRIPT, commands.get(0).getType());
        assertEquals("AutoMiningPlugin", commands.get(0).getInternalName());
        assertEquals("Morning", commands.get(1).getPlan());
        assertEquals(RemoteCommand.Type.STOP_ALL, commands.get(2).getType());
    }

    @Test
    public void nothingToReadIsNoCommands()
    {
        assertTrue(RemoteCommand.parseAll(null).isEmpty());
        assertTrue(parse("{\"ok\":true}").isEmpty());
        assertTrue(parse("{\"commands\":\"START_SCRIPT\"}").isEmpty());
        assertTrue(parse("{\"commands\":[1,\"x\",null,[]]}").isEmpty());
    }

    /** Without a usable id there is nothing to answer, so it is dropped rather than run. */
    @Test
    public void dropsCommandsWithoutAUsableId()
    {
        assertTrue(parse("{\"commands\":[{\"type\":\"STOP_ALL\"}]}").isEmpty());
        assertTrue(parse("{\"commands\":[{\"id\":\"short\",\"type\":\"STOP_ALL\"}]}").isEmpty());
        assertTrue(parse("{\"commands\":[{\"id\":\"../../etc/passwd\",\"type\":\"STOP_ALL\"}]}").isEmpty());
        assertTrue(parse("{\"commands\":[{\"id\":12345678901,\"type\":\"STOP_ALL\"}]}").isEmpty());
    }

    /** Kept, so the website hears "update the client" instead of waiting for it to expire. */
    @Test
    public void unknownTypesAreKeptToBeRefused()
    {
        List<RemoteCommand> commands = parse("{\"commands\":[{\"id\":\"cmabcdefghij1\",\"type\":\"RUN_CODE\"},"
            + "{\"id\":\"cmabcdefghij2\",\"type\":\"unknown\"},{\"id\":\"cmabcdefghij3\"}]}");

        assertEquals(3, commands.size());
        commands.forEach(c -> assertEquals(RemoteCommand.Type.UNKNOWN, c.getType()));
    }

    @Test
    public void badScriptNamesAndPlansAreDropped()
    {
        List<RemoteCommand> commands = parse("{\"commands\":["
            + "{\"id\":\"cmabcdefghij1\",\"type\":\"START_SCRIPT\",\"internalName\":\"net.runelite.client.RuneLite\"},"
            + "{\"id\":\"cmabcdefghij2\",\"type\":\"START_SCRIPT\",\"internalName\":\"../x.jar\"},"
            + "{\"id\":\"cmabcdefghij3\",\"type\":\"START_SCRIPT\",\"internalName\":{\"a\":1}},"
            + "{\"id\":\"cmabcdefghij4\",\"type\":\"QUEUE_START\",\"plan\":\"" + "p".repeat(61) + "\"}]}");

        assertEquals(4, commands.size());
        assertNull(commands.get(0).getInternalName());
        assertNull(commands.get(1).getInternalName());
        assertNull(commands.get(2).getInternalName());
        assertNull(commands.get(3).getPlan());
    }

    @Test
    public void readsAtMostTenFromOneAnswer()
    {
        StringBuilder json = new StringBuilder("{\"commands\":[");
        for (int i = 0; i < 30; i++)
        {
            json.append(i == 0 ? "" : ",").append("{\"id\":\"cmabcdefgh").append(String.format("%03d", i)).append("\",\"type\":\"STOP_ALL\"}");
        }
        assertEquals(RemoteCommand.MAX_PER_ANSWER, parse(json.append("]}").toString()).size());
    }

    @Test
    public void describesOnlyTheActionAndWhatItNames()
    {
        assertEquals("START_SCRIPT AutoMiningPlugin",
            new RemoteCommand("cmabcdefghij1", RemoteCommand.Type.START_SCRIPT, "AutoMiningPlugin", null).describe());
        assertEquals("QUEUE_START (Night)",
            new RemoteCommand("cmabcdefghij1", RemoteCommand.Type.QUEUE_START, null, "Night").describe());
        assertEquals("LOGOUT", new RemoteCommand("cmabcdefghij1", RemoteCommand.Type.LOGOUT, null, null).describe());
    }
}

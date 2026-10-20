package net.runelite.client.plugins.projectx.remote;

import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RemoteStateTest
{
    private static final long NOW = 1_800_000_000_000L;
    private static final GameSnapshot IN_GAME = new GameSnapshot(true, 420, true, "Zezima", 1500, 87, 40);

    @After
    public void clearBoard()
    {
        ScriptStatusBoard.clearForTests();
    }

    private static RemoteState.Run.RunBuilder mining()
    {
        return RemoteState.Run.builder()
            .name("AIO Mining")
            .internalName("AutoMiningPlugin")
            .version("2.1.0")
            .startedAt(NOW - 3_600_000)
            .xp("MINING", 50_000)
            .xp("SMITHING", 1_000)
            .status("Mining iron");
    }

    @Test
    public void describesWhatIsRunning()
    {
        JsonObject s = RemoteState.build(RemoteState.Inputs.builder()
            .clientVersion("1.0.52")
            .game(IN_GAME)
            .shareName(true)
            .run(mining().build())
            .breakHandler("v1")
            .queue(RemoteState.Queue.builder().running(true).plan("Default").step(2).steps(5)
                .description("AIO Mining until Mining 70").planName("Default").planName("Night").build())
            .build(), NOW);

        assertEquals("1.0.52", s.get("clientVersion").getAsString());
        assertTrue(s.get("loggedIn").getAsBoolean());
        assertEquals(420, s.get("world").getAsInt());
        assertTrue(s.get("members").getAsBoolean());
        assertEquals("Zezima", s.get("name").getAsString());
        assertEquals(1500, s.get("totalLevel").getAsInt());
        assertEquals(87, s.get("hp").getAsInt());
        assertEquals(40, s.get("prayer").getAsInt());

        JsonObject run = s.getAsJsonArray("scripts").get(0).getAsJsonObject();
        assertEquals("AutoMiningPlugin", run.get("internalName").getAsString());
        assertEquals(51_000, run.get("xp").getAsLong());
        assertEquals(51_000, run.get("xpPerHour").getAsLong());
        assertEquals("MINING", run.get("skill").getAsString());
        assertEquals("Mining iron", run.get("status").getAsString());

        assertFalse(s.getAsJsonObject("break").get("active").getAsBoolean());
        assertEquals("v1", s.getAsJsonObject("break").get("handler").getAsString());
        JsonObject queue = s.getAsJsonObject("queue");
        assertEquals(2, queue.get("step").getAsInt());
        assertEquals(2, queue.getAsJsonArray("plans").size());
    }

    @Test
    public void leavesTheNameOutWhenNotAllowed()
    {
        JsonObject s = RemoteState.build(RemoteState.Inputs.builder().game(IN_GAME).shareName(false).build(), NOW);
        assertFalse(s.has("name"));
        assertFalse(s.toString().contains("Zezima"));
    }

    /** No coordinates, ever: nothing in the inputs carries them, and nothing in the output is named like them. */
    @Test
    public void neverSaysWhereThePlayerIs()
    {
        JsonObject s = RemoteState.build(RemoteState.Inputs.builder().game(IN_GAME).shareName(true).run(mining().build()).build(), NOW);
        for (String key : new String[]{"x", "y", "plane", "location", "tile", "region", "worldPoint"})
        {
            assertFalse(key, s.has(key));
        }
    }

    @Test
    public void loggedOutSaysOnlyThat()
    {
        JsonObject s = RemoteState.build(RemoteState.Inputs.builder().game(GameSnapshot.LOGGED_OUT).shareName(true).build(), NOW);
        assertFalse(s.get("loggedIn").getAsBoolean());
        assertFalse(s.has("world"));
        assertFalse(s.has("name"));
        assertFalse(s.has("hp"));

        JsonObject unread = RemoteState.build(RemoteState.Inputs.builder().build(), NOW);
        assertFalse(unread.get("loggedIn").getAsBoolean());
    }

    /** Scripts write their own status lines; a token or a webhook in one must not reach the website. */
    @Test
    public void blanksTokensAndLinksInScriptText()
    {
        JsonObject s = RemoteState.build(RemoteState.Inputs.builder()
            .run(mining().status("Posting to https://discord.com/api/webhooks/123/abc with px_AbCdEf123456").build())
            .queue(RemoteState.Queue.builder().running(true).plan("www.example.com").description("<col=ff0000>red</col> step").build())
            .build(), NOW);

        String text = s.toString();
        assertFalse(text.contains("discord.com"));
        assertFalse(text.contains("px_AbCdEf"));
        assertFalse(text.contains("example.com"));
        assertFalse(text.contains("<col"));
        assertTrue(text.contains("[hidden]"));
        assertTrue(text.contains("[link]"));
    }

    @Test
    public void staysUnderTheSizeCap()
    {
        RemoteState.Inputs.InputsBuilder in = RemoteState.Inputs.builder().game(IN_GAME).shareName(true);
        for (int i = 0; i < 50; i++)
        {
            RemoteState.Run.RunBuilder r = mining().name("Script " + i + " " + "n".repeat(500))
                .status("s".repeat(5000)).internalName("Script" + i + "Plugin");
            for (String skill : new String[]{"ATTACK", "STRENGTH", "DEFENCE", "RANGED", "PRAYER", "MAGIC", "MINING"})
            {
                r.xp(skill + i, 10);
            }
            in.run(r.build());
        }
        RemoteState.Queue.QueueBuilder q = RemoteState.Queue.builder().running(true).description("d".repeat(5000));
        for (int i = 0; i < 100; i++)
        {
            q.planName("Plan " + i + " " + "p".repeat(100));
        }
        JsonObject s = RemoteState.build(in.queue(q.build()).build(), NOW);

        assertTrue(RemoteState.bytes(s) <= RemoteState.MAX_BYTES);
        assertTrue(s.getAsJsonArray("scripts").size() <= RemoteState.MAX_SCRIPTS);
        assertTrue(s.getAsJsonObject("queue").getAsJsonArray("plans").size() <= RemoteState.MAX_PLANS);
    }

    /** Even far past the cap, it still says the client is there and logged in. */
    @Test
    public void shrinksTheLeastUsefulPartsFirst()
    {
        // Three bytes a character, so every field at its longest goes over the cap.
        String wide = "€".repeat(500);
        RemoteState.Inputs.InputsBuilder in = RemoteState.Inputs.builder().game(IN_GAME).shareName(true);
        for (int i = 0; i < 10; i++)
        {
            in.run(mining().internalName("Script" + i + "Plugin").name(wide).version(wide).status(wide).build());
        }
        RemoteState.Queue.QueueBuilder q = RemoteState.Queue.builder().running(true).plan(wide).description(wide);
        for (int i = 0; i < 30; i++)
        {
            q.planName(i + wide);
        }
        JsonObject s = RemoteState.build(in.queue(q.build()).build(), NOW);

        assertTrue(RemoteState.bytes(s) <= RemoteState.MAX_BYTES);
        assertTrue(s.get("loggedIn").getAsBoolean());
        assertEquals(420, s.get("world").getAsInt());
        // The status lines went first.
        assertFalse(s.getAsJsonArray("scripts").get(0).getAsJsonObject().has("status"));
    }

    @Test
    public void ratesWaitForAMinute()
    {
        assertEquals(0, RemoteState.perHour(1_000, 30_000));
        assertEquals(60_000, RemoteState.perHour(1_000, 60_000));
        assertEquals(0, RemoteState.perHour(0, 3_600_000));
    }

    @Test
    public void displayNamesAreTidiedOrDropped()
    {
        assertEquals("Iron Man", RemoteState.displayName("Iron Man"));
        assertNull(RemoteState.displayName("<img=2>Zezima"));
        assertNull(RemoteState.displayName("A name far too long for the game"));
        assertNull(RemoteState.displayName(null));
    }

    @Test
    public void percentsAreWholeAndCapped()
    {
        assertEquals(50, GameSnapshot.percent(50, 100));
        assertEquals(100, GameSnapshot.percent(120, 99));
        assertEquals(-1, GameSnapshot.percent(10, 0));
    }

    @Test
    public void statusComesFromTheScriptsOwnPanel()
    {
        // The board is static and every ScriptPanel in the suite writes to it.
        ScriptStatusBoard.clearForTests();
        ScriptStatusBoard.note("AIO Mining V 2.1.0", "Banking ore");
        ScriptStatusBoard.note("Agility V 1.0", "Running laps");
        // Read after noting, so "fresh plus one" really is past the window however slow the run.
        long now = System.currentTimeMillis();
        assertEquals("Banking ore", ScriptStatusBoard.statusFor("AIO Mining", 2, now));
        assertNull(ScriptStatusBoard.statusFor("Woodcutting", 2, now));
        // A panel not drawn for a while says nothing about what the script is doing now.
        assertNull(ScriptStatusBoard.statusFor("AIO Mining", 2, now + ScriptStatusBoard.FRESH_MS + 1));

        ScriptStatusBoard.clearForTests();
        ScriptStatusBoard.note("Mining Pro V 3", "Walking to the mine");
        // One script and one panel: that panel is its, whatever it is titled.
        assertEquals("Walking to the mine", ScriptStatusBoard.statusFor("AIO Mining", 1, now));
    }
}

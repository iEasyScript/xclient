package net.runelite.client.plugins.projectx.scriptqueue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Saving and loading plans, and reading the single queue saved before there were plans. */
public class ScriptQueueStoreTest
{
    /** Exactly what the Script Queue saved before steps had several conditions. */
    private static final String OLD_STEPS = "["
        + "{\"pluginClass\":\"net.runelite.client.plugins.projectx.aiomagic.AioMagicPlugin\",\"pluginName\":\"AIO Magic\","
        + "\"until\":\"LEVEL\",\"minutes\":60,\"skill\":\"Magic\",\"level\":75},"
        + "{\"pluginClass\":\"net.runelite.client.plugins.projectx.woodcutting.WoodcuttingPlugin\",\"pluginName\":\"Woodcutter\","
        + "\"until\":\"MINUTES\",\"minutes\":90,\"skill\":\"Attack\",\"level\":70},"
        + "{\"pluginClass\":\"net.runelite.client.plugins.projectx.fishing.FishingPlugin\",\"pluginName\":\"Fisher\","
        + "\"until\":\"STOPS\",\"minutes\":60,\"skill\":\"Attack\",\"level\":70}"
        + "]";

    private final Gson gson = new Gson();
    private Map<String, String> saved;
    private ScriptQueueStore store;

    @Before
    public void setUp()
    {
        saved = new HashMap<>();
        store = new ScriptQueueStore(new ScriptQueueStore.Config()
        {
            @Override
            public String get(String key)
            {
                return saved.get(key);
            }

            @Override
            public void set(String key, String value)
            {
                saved.put(key, value);
            }
        }, gson);
    }

    @Test
    public void anOldQueueBecomesTheDefaultPlan()
    {
        saved.put("steps", OLD_STEPS);
        saved.put("loop", "true");

        ScriptQueueStore.Plans plans = store.load();
        assertEquals(1, plans.plans.size());
        QueuePlan plan = plans.activePlan();
        assertEquals(QueuePlan.DEFAULT_NAME, plan.getName());
        assertTrue(plan.isLoop());
        assertEquals("an old loop went round for ever", 0, plan.totalRounds());

        List<QueueStep> steps = plan.steps();
        assertEquals(3, steps.size());
        assertEquals(List.of(StopCondition.level("Magic", 75)), steps.get(0).conditions());
        assertEquals(List.of(StopCondition.minutes(90)), steps.get(1).conditions());
        assertTrue(steps.get(2).conditions().isEmpty());
        assertEquals("AIO Magic until Magic 75", steps.get(0).describe());
        assertEquals("Woodcutter for 1h 30m", steps.get(1).describe());
        assertEquals("Fisher until it stops", steps.get(2).describe());
        for (QueueStep s : steps)
        {
            assertEquals(QueueStep.OnFailure.SKIP, s.onFailure());
        }
    }

    @Test
    public void anOldQueueWithoutLoopRunsOnce()
    {
        saved.put("steps", OLD_STEPS);
        QueuePlan plan = store.load().activePlan();
        assertFalse(plan.isLoop());
        assertEquals(1, plan.totalRounds());
    }

    @Test
    public void nothingSavedIsOneEmptyPlan()
    {
        ScriptQueueStore.Plans plans = store.load();
        assertEquals(1, plans.plans.size());
        assertEquals(QueuePlan.DEFAULT_NAME, plans.active);
        assertTrue(plans.activePlan().steps().isEmpty());
    }

    @Test
    public void anUnreadableOldQueueIsAnEmptyOne()
    {
        saved.put("steps", "{not json");
        assertTrue(store.load().activePlan().steps().isEmpty());
    }

    @Test
    public void aStepMissingTheNewFieldsGetsTheirDefaults()
    {
        saved.put("plans", "[{\"name\":\"Main\",\"steps\":[{\"pluginClass\":\"x.Y\",\"pluginName\":\"Y\"}]}]");
        QueuePlan plan = store.load().activePlan();
        assertEquals("Main", plan.getName());
        assertFalse(plan.isLoop());
        assertEquals(0, plan.getRounds());
        QueueStep step = plan.steps().get(0);
        // The old default: an hour.
        assertEquals(List.of(StopCondition.minutes(60)), step.conditions());
        assertEquals(QueueStep.OnFailure.SKIP, step.onFailure());
    }

    @Test
    public void plansRoundTrip()
    {
        QueuePlan a = new QueuePlan("Main");
        QueueStep magic = QueueFakes.step("x.Magic", "AIO Magic",
            StopCondition.level("Magic", 75), StopCondition.minutes(120), StopCondition.xp("Magic", 50_000),
            StopCondition.totalLevel(1_000), StopCondition.combatLevel(70),
            StopCondition.item("Nature rune", 1_000, StopCondition.Place.BOTH), StopCondition.clock("23:00"));
        magic.setOnFailure(QueueStep.OnFailure.RETRY);
        a.steps().add(magic);
        a.setLoop(true);
        a.setRounds(3);
        QueuePlan b = new QueuePlan("Ironman start");
        b.steps().add(QueueFakes.step("x.Fish", "Fisher"));

        store.save(new ArrayList<>(List.of(a, b)), "Ironman start");
        ScriptQueueStore.Plans loaded = store.load();

        assertEquals(2, loaded.plans.size());
        assertEquals("Ironman start", loaded.active);
        QueuePlan main = ScriptQueueStore.find(loaded.plans, "main");
        assertNotNull(main);
        assertTrue(main.isLoop());
        assertEquals(3, main.getRounds());
        assertEquals(3, main.totalRounds());
        assertEquals(magic.conditions(), main.steps().get(0).conditions());
        assertEquals(QueueStep.OnFailure.RETRY, main.steps().get(0).onFailure());
        assertTrue(loaded.activePlan().steps().get(0).conditions().isEmpty());
    }

    @Test
    public void theShowingPlanIsAlsoSavedTheOldWay()
    {
        QueuePlan plan = new QueuePlan("Main");
        plan.steps().add(QueueFakes.step("x.Magic", "AIO Magic", StopCondition.xp("Magic", 1), StopCondition.level("Magic", 75)));
        plan.steps().add(QueueFakes.step("x.Wc", "Woodcutter", StopCondition.minutes(45), StopCondition.clock("23:00")));
        plan.steps().add(QueueFakes.step("x.Rc", "Runecrafter", StopCondition.clock("23:00")));
        plan.setLoop(true);
        store.save(new ArrayList<>(List.of(plan)), "Main");

        assertEquals("true", saved.get("loop"));
        JsonArray old = gson.fromJson(saved.get("steps"), JsonArray.class);
        JsonObject magic = old.get(0).getAsJsonObject();
        assertEquals("LEVEL", magic.get("until").getAsString());
        assertEquals("Magic", magic.get("skill").getAsString());
        assertEquals(75, magic.get("level").getAsInt());
        JsonObject wc = old.get(1).getAsJsonObject();
        assertEquals("MINUTES", wc.get("until").getAsString());
        assertEquals(45, wc.get("minutes").getAsInt());
        // Nothing an older client understands: it runs until it stops.
        assertEquals("STOPS", old.get(2).getAsJsonObject().get("until").getAsString());
    }

    @Test
    public void savedPlansWinOverTheOldQueue()
    {
        saved.put("steps", OLD_STEPS);
        QueuePlan plan = new QueuePlan("Main");
        store.save(new ArrayList<>(List.of(plan)), "Main");
        // The old keys now hold the showing plan, but they would be ignored anyway.
        saved.put("steps", OLD_STEPS);
        ScriptQueueStore.Plans loaded = store.load();
        assertEquals(1, loaded.plans.size());
        assertEquals("Main", loaded.active);
        assertTrue(loaded.activePlan().steps().isEmpty());
    }

    @Test
    public void unreadablePlansFallBackToTheOldQueue()
    {
        saved.put("plans", "[{broken");
        saved.put("steps", OLD_STEPS);
        ScriptQueueStore.Plans loaded = store.load();
        assertEquals(QueuePlan.DEFAULT_NAME, loaded.active);
        assertEquals(3, loaded.activePlan().steps().size());
    }

    @Test
    public void aMissingActivePlanFallsBackToTheFirst()
    {
        store.save(new ArrayList<>(List.of(new QueuePlan("One"), new QueuePlan("Two"))), "Two");
        saved.put("activePlan", "Gone");
        ScriptQueueStore.Plans loaded = store.load();
        assertEquals("One", loaded.active);
        assertSame(loaded.plans.get(0), loaded.activePlan());
    }

    @Test
    public void planNames()
    {
        List<QueuePlan> plans = List.of(new QueuePlan("Main"), new QueuePlan("Other"));
        assertEquals("New plan", ScriptQueueStore.validName(plans, "  New   plan ", null));
        assertNull("taken, whatever the case", ScriptQueueStore.validName(plans, "main", null));
        assertNull(ScriptQueueStore.validName(plans, "   ", null));
        assertNull(ScriptQueueStore.validName(plans, null, null));
        assertEquals("renaming to its own name is fine", "MAIN", ScriptQueueStore.validName(plans, "MAIN", plans.get(0)));
        assertNull("but not to another's", ScriptQueueStore.validName(plans, "Other", plans.get(0)));
        assertEquals(ScriptQueueStore.MAX_NAME, ScriptQueueStore.validName(plans, "x".repeat(100), null).length());
    }

    @Test
    public void aCopyIsSeparate()
    {
        QueuePlan plan = new QueuePlan("Main");
        plan.steps().add(QueueFakes.step("x.Magic", "AIO Magic", StopCondition.level("Magic", 75)));
        QueuePlan copy = store.copy(plan);
        copy.steps().get(0).setConditions(List.of(StopCondition.minutes(5)));
        copy.steps().add(QueueFakes.step("x.Fish", "Fisher"));
        assertEquals(1, plan.steps().size());
        assertEquals(List.of(StopCondition.level("Magic", 75)), plan.steps().get(0).conditions());
    }
}

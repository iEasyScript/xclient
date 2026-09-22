package net.runelite.client.plugins.projectx.agentserver.handler;

import org.junit.After;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ScriptResultStoreTest {

	@After
	public void tearDown() {
		ScriptResultStore.clearAll();
	}

	@Test
	public void testSubmitAndGet() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("passed", true);
		result.put("kills", 10);

		ScriptResultStore.submit("com.test.MyPlugin", result);

		List<Map<String, Object>> stored = ScriptResultStore.get("com.test.MyPlugin");
		assertEquals(1, stored.size());
		assertEquals(true, stored.get(0).get("passed"));
		assertEquals(10, stored.get(0).get("kills"));
	}

	@Test
	public void testSubmitAddsTimestamp() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("passed", false);

		ScriptResultStore.submit("com.test.Plugin", result);

		List<Map<String, Object>> stored = ScriptResultStore.get("com.test.Plugin");
		assertNotNull(stored.get(0).get("timestamp"));
	}

	@Test
	public void testSubmitPreservesExistingTimestamp() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("timestamp", "2026-01-01T00:00:00Z");
		result.put("passed", true);

		ScriptResultStore.submit("com.test.Plugin", result);

		List<Map<String, Object>> stored = ScriptResultStore.get("com.test.Plugin");
		assertEquals("2026-01-01T00:00:00Z", stored.get(0).get("timestamp"));
	}

	@Test
	public void testMultipleSubmitsAccumulate() {
		ScriptResultStore.submit("com.test.Plugin", Map.of("run", 1));
		ScriptResultStore.submit("com.test.Plugin", Map.of("run", 2));
		ScriptResultStore.submit("com.test.Plugin", Map.of("run", 3));

		List<Map<String, Object>> stored = ScriptResultStore.get("com.test.Plugin");
		assertEquals(3, stored.size());
	}

	@Test
	public void testGetUnknownClassReturnsEmpty() {
		List<Map<String, Object>> stored = ScriptResultStore.get("com.nonexistent.Plugin");
		assertNotNull(stored);
		assertTrue(stored.isEmpty());
	}

	@Test
	public void testClearRemovesResults() {
		ScriptResultStore.submit("com.test.A", Map.of("x", 1));
		ScriptResultStore.submit("com.test.B", Map.of("x", 2));

		ScriptResultStore.clear("com.test.A");

		assertTrue(ScriptResultStore.get("com.test.A").isEmpty());
		assertEquals(1, ScriptResultStore.get("com.test.B").size());
	}

	@Test
	public void testClearAllRemovesEverything() {
		ScriptResultStore.submit("com.test.A", Map.of("x", 1));
		ScriptResultStore.submit("com.test.B", Map.of("x", 2));

		ScriptResultStore.clearAll();

		assertTrue(ScriptResultStore.get("com.test.A").isEmpty());
		assertTrue(ScriptResultStore.get("com.test.B").isEmpty());
	}

	@Test
	public void testSubmitDoesNotMutateOriginalMap() {
		Map<String, Object> original = new LinkedHashMap<>();
		original.put("passed", true);

		ScriptResultStore.submit("com.test.Plugin", original);

		assertFalse(original.containsKey("timestamp"));
	}

	@Test
	public void testIsolationBetweenPlugins() {
		ScriptResultStore.submit("com.test.A", Map.of("value", "a"));
		ScriptResultStore.submit("com.test.B", Map.of("value", "b"));

		List<Map<String, Object>> aResults = ScriptResultStore.get("com.test.A");
		List<Map<String, Object>> bResults = ScriptResultStore.get("com.test.B");

		assertEquals(1, aResults.size());
		assertEquals("a", aResults.get(0).get("value"));
		assertEquals(1, bResults.size());
		assertEquals("b", bResults.get(0).get("value"));
	}
}

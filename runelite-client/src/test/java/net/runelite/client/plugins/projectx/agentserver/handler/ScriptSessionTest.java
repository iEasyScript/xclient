package net.runelite.client.plugins.projectx.agentserver.handler;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ScriptSessionTest {

	@Test
	public void testNewSessionIsRunning() {
		ScriptSession session = new ScriptSession("Test Plugin", "com.test.TestPlugin");

		assertEquals(ScriptSession.Status.RUNNING, session.getStatus());
		assertEquals("com.test.TestPlugin", session.getPluginClassName());
	}

	@Test
	public void testNewSessionToMapContainsRequiredFields() {
		ScriptSession session = new ScriptSession("Test Plugin", "com.test.TestPlugin");
		Map<String, Object> map = session.toMap(true);

		assertEquals("Test Plugin", map.get("name"));
		assertEquals("com.test.TestPlugin", map.get("className"));
		assertEquals(true, map.get("active"));
		assertEquals("RUNNING", map.get("status"));
		assertNotNull(map.get("startedAt"));
		assertNotNull(map.get("runtimeMs"));
		assertNull(map.get("stoppedAt"));
		assertNull(map.get("error"));
	}

	@Test
	public void testRuntimeMsIsPositive() throws InterruptedException {
		ScriptSession session = new ScriptSession("Test", "com.test.Test");
		Thread.sleep(50);
		Map<String, Object> map = session.toMap(true);

		long runtimeMs = ((Number) map.get("runtimeMs")).longValue();
		assertTrue("runtimeMs should be >= 50 but was " + runtimeMs, runtimeMs >= 40);
	}

	@Test
	public void testMarkStopped() {
		ScriptSession session = new ScriptSession("Test", "com.test.Test");
		session.markStopped();

		assertEquals(ScriptSession.Status.STOPPED, session.getStatus());

		Map<String, Object> map = session.toMap(false);
		assertEquals("STOPPED", map.get("status"));
		assertEquals(false, map.get("active"));
		assertNotNull(map.get("stoppedAt"));
		assertNotNull(map.get("runtimeMs"));
		assertNull(map.get("error"));
	}

	@Test
	public void testMarkError() {
		ScriptSession session = new ScriptSession("Test", "com.test.Test");
		session.markError("NullPointerException in main loop");

		assertEquals(ScriptSession.Status.ERROR, session.getStatus());

		Map<String, Object> map = session.toMap(false);
		assertEquals("ERROR", map.get("status"));
		assertNotNull(map.get("stoppedAt"));
		assertEquals("NullPointerException in main loop", map.get("error"));
	}

	@Test
	public void testActiveFieldReflectsParameter() {
		ScriptSession session = new ScriptSession("Test", "com.test.Test");

		Map<String, Object> activeMap = session.toMap(true);
		assertEquals(true, activeMap.get("active"));

		Map<String, Object> inactiveMap = session.toMap(false);
		assertEquals(false, inactiveMap.get("active"));
	}

	@Test
	public void testStoppedSessionHasRuntimeMs() {
		ScriptSession session = new ScriptSession("Test", "com.test.Test");
		session.markStopped();

		Map<String, Object> map = session.toMap(false);
		assertTrue(map.containsKey("runtimeMs"));
		long runtimeMs = ((Number) map.get("runtimeMs")).longValue();
		assertTrue("runtimeMs should be >= 0", runtimeMs >= 0);
	}

	@Test
	public void testErrorSessionHasRuntimeMs() {
		ScriptSession session = new ScriptSession("Test", "com.test.Test");
		session.markError("crash");

		Map<String, Object> map = session.toMap(false);
		assertTrue(map.containsKey("runtimeMs"));
		long runtimeMs = ((Number) map.get("runtimeMs")).longValue();
		assertTrue("runtimeMs should be >= 0", runtimeMs >= 0);
	}
}

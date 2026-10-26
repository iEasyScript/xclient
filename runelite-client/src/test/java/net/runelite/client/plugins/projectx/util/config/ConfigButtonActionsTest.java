package net.runelite.client.plugins.projectx.util.config;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ConfigButtonActionsTest {
	private static final String GROUP = "configButtonActionsTestGroup";
	private static final String KEY = "someButton";

	@After
	public void cleanUp() {
		ConfigButtonActions.unregister(GROUP, KEY);
		ConfigButtonActions.unregister(GROUP, "other");
	}

	@Test
	public void nothingRegisteredFallsThrough() {
		assertNull(ConfigButtonActions.lookup(GROUP, KEY));
		assertFalse(ConfigButtonActions.run(GROUP, KEY, null));
	}

	@Test
	public void registeredActionRunsWithTheParent() {
		AtomicReference<Component> seen = new AtomicReference<>();
		ConfigButtonActions.register(GROUP, KEY, seen::set);
		Component parent = new java.awt.Panel();
		assertTrue(ConfigButtonActions.run(GROUP, KEY, parent));
		assertSame(parent, seen.get());
	}

	@Test
	public void actionsAreKeyedByGroupAndKey() {
		ConfigButtonActions.register(GROUP, KEY, c -> { });
		assertNull(ConfigButtonActions.lookup(GROUP, "other"));
		assertNull(ConfigButtonActions.lookup("anotherGroup", KEY));
	}

	@Test
	public void registeringAgainReplaces() {
		AtomicReference<String> hit = new AtomicReference<>();
		ConfigButtonActions.register(GROUP, KEY, c -> hit.set("first"));
		ConfigButtonActions.register(GROUP, KEY, c -> hit.set("second"));
		ConfigButtonActions.run(GROUP, KEY, null);
		assertSame("second", hit.get());
	}

	@Test
	public void unregisterRemoves() {
		ConfigButtonActions.register(GROUP, KEY, c -> { });
		ConfigButtonActions.unregister(GROUP, KEY);
		assertFalse(ConfigButtonActions.run(GROUP, KEY, null));
	}
}

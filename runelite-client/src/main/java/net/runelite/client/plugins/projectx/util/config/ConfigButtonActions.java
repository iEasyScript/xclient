package net.runelite.client.plugins.projectx.util.config;

import java.awt.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lets a plugin attach an action to a {@code ConfigButton} setting, so clicking the button in the
 * settings panel can open something (for example a pop-out window) even while the plugin is not running.
 *
 * <p>A plugin class is instantiated even when its script is stopped, but it only hears config changes
 * while running. The settings panel therefore asks this registry first: if an action is registered for the
 * button's (group, key) it runs that, otherwise the panel behaves as before.
 */
public final class ConfigButtonActions {
	private static final Map<String, Consumer<Component>> ACTIONS = new ConcurrentHashMap<>();

	private ConfigButtonActions() {
	}

	private static String id(String group, String key) {
		return group + '\u0000' + key;
	}

	/** Registers (or replaces) the action for a button. The action gets the clicked button as its parent component. */
	public static void register(String group, String key, Consumer<Component> action) {
		if (group == null || key == null || action == null) {
			throw new IllegalArgumentException("group, key and action are required");
		}
		ACTIONS.put(id(group, key), action);
	}

	/** Removes the action for a button, if any. */
	public static void unregister(String group, String key) {
		if (group != null && key != null) {
			ACTIONS.remove(id(group, key));
		}
	}

	/** Returns the registered action, or null when there is none. */
	public static Consumer<Component> lookup(String group, String key) {
		if (group == null || key == null) {
			return null;
		}
		return ACTIONS.get(id(group, key));
	}

	/**
	 * Runs the registered action for a button on the calling thread (the settings panel calls this on the Swing thread).
	 *
	 * @return true when an action existed and was run, false when the caller should fall back to its default behaviour
	 */
	public static boolean run(String group, String key, Component parent) {
		Consumer<Component> action = lookup(group, key);
		if (action == null) {
			return false;
		}
		action.accept(parent);
		return true;
	}
}

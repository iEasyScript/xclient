package net.runelite.client.plugins.projectx.agentserver.handler;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ScriptResultStore {

	private static final ConcurrentHashMap<String, List<Map<String, Object>>> results = new ConcurrentHashMap<>();

	private ScriptResultStore() {}

	public static void submit(String className, Map<String, Object> result) {
		Map<String, Object> entry = new LinkedHashMap<>(result);
		entry.putIfAbsent("timestamp", Instant.now().toString());
		results.computeIfAbsent(className, k -> new CopyOnWriteArrayList<>()).add(entry);
	}

	public static List<Map<String, Object>> get(String className) {
		List<Map<String, Object>> stored = results.get(className);
		return stored != null ? stored : new ArrayList<>();
	}

	public static void clear(String className) {
		results.remove(className);
	}

	public static void clearAll() {
		results.clear();
	}
}

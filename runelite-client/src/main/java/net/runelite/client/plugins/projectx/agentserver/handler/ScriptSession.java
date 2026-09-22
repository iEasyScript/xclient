package net.runelite.client.plugins.projectx.agentserver.handler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScriptSession {

	public enum Status { RUNNING, STOPPED, ERROR }

	private final String pluginName;
	private final String pluginClassName;
	private final Instant startedAt;
	private Instant stoppedAt;
	private Status status;
	private String error;

	public ScriptSession(String pluginName, String pluginClassName) {
		this.pluginName = pluginName;
		this.pluginClassName = pluginClassName;
		this.startedAt = Instant.now();
		this.status = Status.RUNNING;
	}

	public void markStopped() {
		this.stoppedAt = Instant.now();
		this.status = Status.STOPPED;
	}

	public void markError(String error) {
		this.stoppedAt = Instant.now();
		this.status = Status.ERROR;
		this.error = error;
	}

	public Map<String, Object> toMap(boolean active) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("name", pluginName);
		map.put("className", pluginClassName);
		map.put("active", active);
		map.put("status", status.name());
		map.put("startedAt", startedAt.toString());
		if (stoppedAt != null) {
			map.put("stoppedAt", stoppedAt.toString());
		}
		if (status == Status.RUNNING) {
			map.put("runtimeMs", Instant.now().toEpochMilli() - startedAt.toEpochMilli());
		} else if (stoppedAt != null) {
			map.put("runtimeMs", stoppedAt.toEpochMilli() - startedAt.toEpochMilli());
		}
		if (error != null) {
			map.put("error", error);
		}
		return map;
	}

	public String getPluginClassName() {
		return pluginClassName;
	}

	public Status getStatus() {
		return status;
	}
}

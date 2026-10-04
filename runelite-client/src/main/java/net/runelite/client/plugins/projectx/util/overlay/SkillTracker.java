package net.runelite.client.plugins.projectx.util.overlay;

import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Experience gained since a script run started, per skill: XP, XP an hour, levels gained and time
 * to the next level.
 *
 * <p>Keep one per overlay and call {@link #observe} at the top of every render, on the client
 * thread. A new start time -- the script was stopped and started again -- resets every skill, so
 * the numbers always describe the current run.
 */
public final class SkillTracker {
	private static final Skill[] SKILLS = Skill.values();

	private final Map<Skill, Integer> startXp = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> nowXp = new EnumMap<>(Skill.class);
	private long trackedStart = Long.MIN_VALUE;
	private long elapsedMs = 0;

	/**
	 * Reads every skill's XP. Call once a frame, on the client thread.
	 *
	 * @param startedAt when the run began, in epoch millis; 0 if it has not started
	 */
	public void observe(Client client, long startedAt) {
		for (Skill skill : SKILLS) {
			if (skill == Skill.OVERALL) continue;
			nowXp.put(skill, client.getSkillExperience(skill));
		}
		if (startedAt != trackedStart) {
			trackedStart = startedAt;
			startXp.clear();
			startXp.putAll(nowXp);
		}
		elapsedMs = startedAt > 0 ? Math.max(0, System.currentTimeMillis() - startedAt) : 0;
	}

	public int xp(Skill skill) {
		return nowXp.getOrDefault(skill, 0);
	}

	public int gained(Skill skill) {
		return Math.max(0, xp(skill) - startXp.getOrDefault(skill, xp(skill)));
	}

	public long perHour(Skill skill) {
		return elapsedMs > 0 ? (long) (gained(skill) * 3_600_000d / elapsedMs) : 0;
	}

	public int level(Skill skill) {
		return Experience.getLevelForXp(xp(skill));
	}

	public int levelsGained(Skill skill) {
		return level(skill) - Experience.getLevelForXp(startXp.getOrDefault(skill, xp(skill)));
	}

	/** Milliseconds to the next level at the current rate; -1 if not gaining or already 99. */
	public long msToLevel(Skill skill) {
		int level = level(skill);
		long rate = perHour(skill);
		if (level >= Experience.MAX_REAL_LEVEL || rate <= 0) return -1;
		return (long) ((Experience.getXpForLevel(level + 1) - xp(skill)) * 3_600_000d / rate);
	}

	/** The skills that have gained any XP this run, most gained first. */
	public List<Skill> trained() {
		List<Skill> out = new ArrayList<>();
		for (Skill skill : SKILLS) {
			if (skill != Skill.OVERALL && gained(skill) > 0) out.add(skill);
		}
		out.sort(Comparator.comparingInt(this::gained).reversed());
		return out;
	}

	public long elapsedMs() {
		return elapsedMs;
	}
}

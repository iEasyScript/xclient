package net.runelite.client.plugins.projectx.questhelper;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.runelite.client.plugins.projectx.questhelper.steps.QuestStep;

final class MultiNpcTargetTracker
{
	static final long COMMIT_TIMEOUT_MS = 30_000L;

	private QuestStep step;
	private int npcIndex = -1;
	private long expiresAt;

	<T> T select(QuestStep currentStep, List<T> npcs, ToIntFunction<T> indexOf, Set<Integer> handled, Comparator<T> nearestFirst, long now)
	{
		if (currentStep != null && currentStep == step && now < expiresAt)
		{
			for (T npc : npcs)
			{
				if (indexOf.applyAsInt(npc) == npcIndex)
				{
					return npc;
				}
			}
		}

		for (T npc : npcs)
		{
			if (!handled.contains(indexOf.applyAsInt(npc)))
			{
				return npc;
			}
		}

		return npcs.stream().min(nearestFirst).orElse(null);
	}

	void commit(QuestStep currentStep, int index, long now)
	{
		if (currentStep == step && index == npcIndex && now < expiresAt)
		{
			return;
		}

		step = currentStep;
		npcIndex = index;
		expiresAt = now + COMMIT_TIMEOUT_MS;
	}

	void release()
	{
		step = null;
		npcIndex = -1;
		expiresAt = 0;
	}
}

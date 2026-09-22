package net.runelite.client.plugins.projectx.util.magic;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

public interface Spell
{
	MagicAction getMagicAction();

	HashMap<Runes, Integer> getRequiredRunes();

	default HashMap<Runes, Integer> getRequiredRunes(int casts)
	{
		final HashMap<Runes, Integer> reqRunes = getRequiredRunes();
		if (casts != 1)
		{
			reqRunes.replaceAll((key, value) -> value * casts);
		}
		return reqRunes;
	}

	Rs2Spellbook getSpellbook();

	int getRequiredLevel();

	static Map<Integer, Integer> convertRequiredRunes(Map<Runes, Integer> map)
	{
		return map.entrySet().stream()
			.collect(Collectors.toMap(entry -> entry.getKey().getItemId(), Map.Entry::getValue));
	}

	default boolean hasRequiredLevel()
	{
		return Rs2Player.getSkillRequirement(Skill.MAGIC, getRequiredLevel());
	}

	default boolean hasRequiredSpellbook()
	{
		return Rs2Magic.isSpellbook(getSpellbook());
	}

	default boolean hasRequirements()
	{
		return hasRequiredLevel() && hasRequiredSpellbook();
	}
}

package net.runelite.client.plugins.projectx.util.magic.thralls;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.magic.Rs2Magic;
import net.runelite.client.plugins.projectx.util.magic.Rs2Spellbook;
import net.runelite.client.plugins.projectx.util.magic.Runes;
import net.runelite.client.plugins.projectx.util.magic.Spell;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

/**
 * Represents the different types of thralls that can be summoned using the Arceuus spellbook.
 * <p>
 * Each thrall has a corresponding spell and type (Magic, Ranged, or Melee).
 * Thralls vary in strength from Lesser to Superior to Greater.
 */
@Getter
public enum Rs2Thrall implements Spell
{
	LESSER_GHOST(MagicAction.RESURRECT_LESSER_GHOST, Map.of(
		Runes.AIR, 10,
		Runes.COSMIC, 1,
		Runes.MIND, 5
	), Rs2Spellbook.ARCEUUS, ThrallType.MAGIC),
	LESSER_SKELETON(MagicAction.RESURRECT_LESSER_SKELETON, Map.of(
		Runes.AIR, 10,
		Runes.COSMIC, 1,
		Runes.MIND, 5
	), Rs2Spellbook.ARCEUUS, ThrallType.RANGED),
	LESSER_ZOMBIE(MagicAction.RESURRECT_LESSER_ZOMBIE, Map.of(
		Runes.AIR, 10,
		Runes.COSMIC, 1,
		Runes.MIND, 5
	), Rs2Spellbook.ARCEUUS, ThrallType.MELEE),
	SUPERIOR_GHOST(MagicAction.RESURRECT_SUPERIOR_GHOST, Map.of(
		Runes.EARTH, 10,
		Runes.COSMIC, 1,
		Runes.DEATH, 5
	), Rs2Spellbook.ARCEUUS, ThrallType.MAGIC),
	SUPERIOR_SKELETON(MagicAction.RESURRECT_SUPERIOR_SKELETON, Map.of(
		Runes.EARTH, 10,
		Runes.COSMIC, 1,
		Runes.DEATH, 5
	), Rs2Spellbook.ARCEUUS, ThrallType.RANGED),
	SUPERIOR_ZOMBIE(MagicAction.RESURRECT_SUPERIOR_ZOMBIE, Map.of(
		Runes.EARTH, 10,
		Runes.COSMIC, 1,
		Runes.DEATH, 5
	), Rs2Spellbook.ARCEUUS, ThrallType.MELEE),
	GREATER_GHOST(MagicAction.RESURRECT_GREATER_GHOST, Map.of(
		Runes.FIRE, 10,
		Runes.COSMIC, 1,
		Runes.BLOOD, 5
	), Rs2Spellbook.ARCEUUS, ThrallType.MAGIC),
	GREATER_SKELETON(MagicAction.RESURRECT_GREATER_SKELETON, Map.of(
		Runes.FIRE, 10,
		Runes.COSMIC, 1,
		Runes.BLOOD, 5
	), Rs2Spellbook.ARCEUUS, ThrallType.RANGED),
	GREATER_ZOMBIE(MagicAction.RESURRECT_GREATER_ZOMBIE, Map.of(
		Runes.FIRE, 10,
		Runes.COSMIC, 1,
		Runes.BLOOD, 5
	), Rs2Spellbook.ARCEUUS, ThrallType.MELEE),
	;

	private final String name;
	private final MagicAction magicAction;
	private final Map<Runes, Integer> requiredRunes;
	private final Rs2Spellbook spellbook;
	private final int requiredLevel;
	private final ThrallType thrallType;

	Rs2Thrall(MagicAction magicAction, Map<Runes, Integer> requiredRunes, Rs2Spellbook spellbook, ThrallType thrallType)
	{
		this.magicAction = magicAction;
		this.requiredRunes = requiredRunes;
		this.spellbook = spellbook;
		this.name = magicAction.getName();
		this.requiredLevel = magicAction.getLevel();
		this.thrallType = thrallType;
	}

	/**
	 * Checks if the player meets all requirements to cast the thrall spell.
	 * <p>
	 * Requirements:
	 * <ul>
	 *   <li>The player must have the Book of the Dead in their inventory or equipped.</li>
	 *   <li>The player must meet the spell's level and spellbook requirements (see {@link Spell#hasRequirements()}).</li>
	 * </ul>
	 *
	 * @return true if all requirements are met, false otherwise
	 */
	@Override
	public boolean hasRequirements()
	{
		return (Rs2Inventory.hasItem(ItemID.BOOK_OF_THE_DEAD) || Rs2Equipment.isWearing(ItemID.BOOK_OF_THE_DEAD)) && Spell.super.hasRequirements();
	}

	/**
	 * Checks if the given thrall can be cast based on current game state.
	 *
	 * @param rs2Thrall the thrall to check
	 * @return true if the thrall can be summoned, false otherwise
	 */
	public static boolean canCast(Rs2Thrall rs2Thrall)
	{
		if (isActive())
		{
			return false;
		}
		return rs2Thrall.hasRequirements() && Rs2Magic.hasRequiredRunes(rs2Thrall);
	}

	/**
	 * Returns whether a thrall is currently active or on cooldown.
	 *
	 * @return true if a thrall is active or cooling down, false otherwise
	 */
	public static boolean isActive()
	{
		return (ProjectX.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_ACTIVE) == 1 || ProjectX.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_COOLDOWN) >= 1);
	}

	/**
	 * Attempts to summon the given thrall if casting conditions are met.
	 *
	 * @param rs2Thrall the thrall to summon
	 * @return true if the cast was successful, false otherwise
	 */
	public static boolean cast(Rs2Thrall rs2Thrall)
	{
		if (!canCast(rs2Thrall))
		{
			return false;
		}
		return Rs2Magic.cast(rs2Thrall);
	}

	/**
	 * Gets the best available thrall of the given type that the player can currently cast.
	 *
	 * @param type the desired ThrallType (MAGIC, RANGED, or MELEE)
	 * @return the best available Thrall of that type, or null if none can be cast
	 */
	public static Rs2Thrall getBestThrall(ThrallType type)
	{
		return Arrays.stream(Rs2Thrall.values()).filter(rs2Thrall -> rs2Thrall.getThrallType() == type).filter(Rs2Thrall::canCast).findFirst().orElse(null);
	}

	@Override
	public HashMap<Runes, Integer> getRequiredRunes() {
		return new HashMap<>(requiredRunes);
	}
}

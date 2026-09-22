package net.runelite.client.plugins.projectx.util.combat.weapons;

import java.util.List;

public class Dart extends Weapon
{
	public Dart(int id)
	{
		super(id);
		range = 3;
	}

	public Dart(List<Integer> weaponData)
	{
		super(weaponData);
	}
}

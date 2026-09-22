package net.runelite.client.plugins.projectx.pouch;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class ClickOperation
{
	Pouch pouch;
	int tick; // timeout for operation
	int delta;

	ClickOperation(Pouch pouch, int tick)
	{
		this(pouch, tick, 0);
	}
}
package net.runelite.client.plugins.projectx.mouserecorder;

import lombok.Getter;

@Getter
public class MouseModifiers
{
	private final boolean shift;
	private final boolean ctrl;
	private final boolean alt;

	public MouseModifiers(boolean shift, boolean ctrl, boolean alt)
	{
		this.shift = shift;
		this.ctrl = ctrl;
		this.alt = alt;
	}
}

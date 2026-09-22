package net.runelite.client.plugins.projectx.mouserecorder;

import lombok.Getter;

@Getter
public class MouseMacroEvent
{
	private final MouseMacroEventType type;
	private final long offsetMs;
	private final int x;
	private final int y;
	private final MouseButton button;
	private final MouseModifiers modifiers;
	private final Integer scrollDeltaX;
	private final Integer scrollDeltaY;
	private final MenuEntrySnapshot menuEntry;

	public MouseMacroEvent(
		MouseMacroEventType type,
		long offsetMs,
		int x,
		int y,
		MouseButton button,
		MouseModifiers modifiers,
		Integer scrollDeltaX,
		Integer scrollDeltaY,
		MenuEntrySnapshot menuEntry)
	{
		this.type = type;
		this.offsetMs = offsetMs;
		this.x = x;
		this.y = y;
		this.button = button;
		this.modifiers = modifiers;
		this.scrollDeltaX = scrollDeltaX;
		this.scrollDeltaY = scrollDeltaY;
		this.menuEntry = menuEntry;
	}
}

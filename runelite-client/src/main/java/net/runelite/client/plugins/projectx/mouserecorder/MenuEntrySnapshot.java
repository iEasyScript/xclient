package net.runelite.client.plugins.projectx.mouserecorder;

import lombok.Getter;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.Widget;

@Getter
public class MenuEntrySnapshot
{
	private final String option;
	private final String target;
	private final MenuAction type;
	private final int identifier;
	private final int param0;
	private final int param1;
	private final int itemId;
	private final int itemOp;
	private final int worldViewId;
	private final boolean forceLeftClick;
	private final boolean deprioritized;
	private final Integer widgetId;

	public MenuEntrySnapshot(MenuEntry entry)
	{
		this.option = entry.getOption();
		this.target = entry.getTarget();
		this.type = entry.getType();
		this.identifier = entry.getIdentifier();
		this.param0 = entry.getParam0();
		this.param1 = entry.getParam1();
		this.itemId = entry.getItemId();
		this.itemOp = entry.getItemOp();
		this.worldViewId = entry.getWorldViewId();
		this.forceLeftClick = entry.isForceLeftClick();
		this.deprioritized = entry.isDeprioritized();
		Widget widget = entry.getWidget();
		this.widgetId = widget != null ? widget.getId() : null;
	}
}

package net.runelite.client.plugins.projectx.questhelper.steps;

import net.runelite.client.plugins.projectx.questhelper.questhelpers.QuestHelper;
import net.runelite.client.plugins.projectx.questhelper.requirements.Requirement;
import net.runelite.client.plugins.projectx.questhelper.requirements.player.ShipInPortRequirement;
import net.runelite.client.plugins.projectx.questhelper.requirements.util.Port;
import net.runelite.client.plugins.projectx.questhelper.requirements.zone.Zone;
import net.runelite.client.plugins.projectx.questhelper.requirements.zone.ZoneRequirement;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public class SailStep extends DetailedQuestStep
{
	@Getter
	private final ZoneRequirement zoneRequirement;

	public SailStep(QuestHelper questHelper, Port toPort){
		this(questHelper, new ShipInPortRequirement(toPort));
	}
  
	public SailStep(QuestHelper questHelper, ShipInPortRequirement toPort)
	{
		super(questHelper, "Sail to " + toPort.getPort().getName() + ".");
		Zone zone = toPort.getPort().getDockZone();
		setHighlightZone(zone);
		this.zoneRequirement = new ZoneRequirement(zone);
		setWorldPoint(toPort.getPort().getGangplankLocation());
	}
  
	public SailStep(QuestHelper questHelper, Port toPort, Requirement... requirements){
		this(questHelper, new ShipInPortRequirement(toPort), requirements);
	}
  
	public SailStep(QuestHelper questHelper, ShipInPortRequirement toPort, Requirement... requirements)
	{
		this(questHelper,toPort);
		this.addRequirement(requirements);
	}
  
	public SailStep(QuestHelper questHelper, WorldPoint toPoint, String text, Requirement... requirements)
	{
		super(questHelper, text);
		this.addRequirement(requirements);
		Zone zone = new Zone(toPoint.dx(-5).dy(-5), toPoint.dx(5).dy(5));
		setHighlightZone(zone);
		this.zoneRequirement = new ZoneRequirement(zone);
		setWorldPoint(toPoint);
	}
  
	public SailStep(QuestHelper questHelper, WorldPoint toPoint, Requirement... requirements)
	{
		this(questHelper, toPoint, "Sail to the location on your map.");
		this.addRequirement(requirements);
	}
}


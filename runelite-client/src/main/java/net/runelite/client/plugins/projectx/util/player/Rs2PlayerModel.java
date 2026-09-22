package net.runelite.client.plugins.projectx.util.player;

import java.awt.Polygon;
import lombok.Getter;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.IEntity;
import net.runelite.client.plugins.projectx.util.ActorModel;
import org.apache.commons.lang3.NotImplementedException;

@Getter
public class Rs2PlayerModel extends ActorModel implements Player {

	private final Player player;

	public Rs2PlayerModel()
	{
		super(ProjectX.getClient().getLocalPlayer());
		this.player = ProjectX.getClient().getLocalPlayer();
	}

	public Rs2PlayerModel(final Player player)
	{
		super(player);
		this.player = player;
	}

	@Override
	public int getId()
	{
		return player.getId();
	}

    @Override
	public PlayerComposition getPlayerComposition()
	{
		return player.getPlayerComposition();
	}

	@Override
	public int getTeam()
	{
		return player.getTeam();
	}

	@Override
	public boolean isFriendsChatMember()
	{
		return player.isFriendsChatMember();
	}

	@Override
	public boolean isFriend()
	{
		return player.isFriend();
	}

	@Override
	public boolean isClanMember()
	{
		return player.isClanMember();
	}

	@Override
	public HeadIcon getOverheadIcon()
	{
		return player.getOverheadIcon();
	}

	@Override
	public int getSkullIcon()
	{
		return player.getSkullIcon();
	}

	@Override
	public void setSkullIcon(int skullIcon)
	{
		player.setSkullIcon(skullIcon);
	}

	@Override
	public int getFootprintSize()
	{
		return 0;
	}
}

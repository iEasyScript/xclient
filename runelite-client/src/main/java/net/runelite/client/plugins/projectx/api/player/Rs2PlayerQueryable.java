package net.runelite.client.plugins.projectx.api.player;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.AbstractEntityQueryable;
import net.runelite.client.plugins.projectx.api.IEntityQueryable;
import net.runelite.client.plugins.projectx.api.player.models.Rs2PlayerModel;

import java.util.stream.Stream;

public final class Rs2PlayerQueryable extends AbstractEntityQueryable<Rs2PlayerQueryable, Rs2PlayerModel>
        implements IEntityQueryable<Rs2PlayerQueryable, Rs2PlayerModel> {

    public Rs2PlayerQueryable() {
        super();
    }

    @Override
    protected Stream<Rs2PlayerModel> initialSource() {
        return ProjectX.getRs2PlayerCache().getStream();
    }
}

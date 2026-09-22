package net.runelite.client.plugins.projectx.api.tileitem;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.AbstractEntityQueryable;
import net.runelite.client.plugins.projectx.api.IEntityQueryable;
import net.runelite.client.plugins.projectx.api.tileitem.models.Rs2TileItemModel;

import java.util.stream.Stream;

public final class Rs2TileItemQueryable extends AbstractEntityQueryable<Rs2TileItemQueryable, Rs2TileItemModel>
        implements IEntityQueryable<Rs2TileItemQueryable, Rs2TileItemModel> {

    public Rs2TileItemQueryable() {
        super();
    }

    @Override
    protected Stream<Rs2TileItemModel> initialSource() {
        return ProjectX.getRs2TileItemCache().getStream();
    }
}

package net.runelite.client.plugins.projectx.api.tileobject;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.AbstractEntityQueryable;
import net.runelite.client.plugins.projectx.api.IEntityQueryable;
import net.runelite.client.plugins.projectx.api.tileobject.models.Rs2TileObjectModel;

import java.util.stream.Stream;

public final class Rs2TileObjectQueryable extends AbstractEntityQueryable<Rs2TileObjectQueryable, Rs2TileObjectModel>
        implements IEntityQueryable<Rs2TileObjectQueryable, Rs2TileObjectModel> {

    public Rs2TileObjectQueryable() {
        super();
    }

    @Override
    protected Stream<Rs2TileObjectModel> initialSource() {
        return ProjectX.getRs2TileObjectCache().getStream();
    }
}

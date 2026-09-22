package net.runelite.client.plugins.projectx.api.npc;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.AbstractEntityQueryable;
import net.runelite.client.plugins.projectx.api.IEntityQueryable;
import net.runelite.client.plugins.projectx.api.npc.models.Rs2NpcModel;

import java.util.stream.Stream;

public final class Rs2NpcQueryable extends AbstractEntityQueryable<Rs2NpcQueryable, Rs2NpcModel>
        implements IEntityQueryable<Rs2NpcQueryable, Rs2NpcModel> {

    public Rs2NpcQueryable() {
        super();
    }

    @Override
    protected Stream<Rs2NpcModel> initialSource() {
        return ProjectX.getRs2NpcCache().getStream();
    }
}

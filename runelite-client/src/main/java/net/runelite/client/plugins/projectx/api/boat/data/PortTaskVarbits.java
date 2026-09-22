package net.runelite.client.plugins.projectx.api.boat.data;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import net.runelite.api.gameval.VarbitID;

public enum PortTaskVarbits
{
    TASK_SLOT_0_ID(VarbitID.PORT_TASK_SLOT_0_ID, TaskType.ID, 0),
    TASK_SLOT_0_TAKEN(VarbitID.PORT_TASK_SLOT_0_CARGO_TAKEN,  TaskType.TAKEN, 0),
    TASK_SLOT_0_DELIVERED(VarbitID.PORT_TASK_SLOT_0_CARGO_DELIVERED, TaskType.DELIVERED, 0),
    TASK_SLOT_1_ID(VarbitID.PORT_TASK_SLOT_1_ID, TaskType.ID, 1),
    TASK_SLOT_1_TAKEN(VarbitID.PORT_TASK_SLOT_1_CARGO_TAKEN,  TaskType.TAKEN, 1),
    TASK_SLOT_1_DELIVERED(VarbitID.PORT_TASK_SLOT_1_CARGO_DELIVERED, TaskType.DELIVERED, 1),
    TASK_SLOT_2_ID(VarbitID.PORT_TASK_SLOT_2_ID,  TaskType.ID, 2),
    TASK_SLOT_2_TAKEN(VarbitID.PORT_TASK_SLOT_2_CARGO_TAKEN, TaskType.TAKEN, 2),
    TASK_SLOT_2_DELIVERED(VarbitID.PORT_TASK_SLOT_2_CARGO_DELIVERED, TaskType.DELIVERED, 2),
    TASK_SLOT_3_ID(VarbitID.PORT_TASK_SLOT_3_ID, TaskType.ID, 3),
    TASK_SLOT_3_TAKEN(VarbitID.PORT_TASK_SLOT_3_CARGO_TAKEN,  TaskType.TAKEN, 3),
    TASK_SLOT_3_DELIVERED(VarbitID.PORT_TASK_SLOT_3_CARGO_DELIVERED,  TaskType.DELIVERED, 3),
    TASK_SLOT_4_ID(VarbitID.PORT_TASK_SLOT_4_ID, TaskType.ID, 4),
    TASK_SLOT_4_TAKEN(VarbitID.PORT_TASK_SLOT_4_CARGO_TAKEN, TaskType.TAKEN, 4),
    TASK_SLOT_4_DELIVERED(VarbitID.PORT_TASK_SLOT_4_CARGO_DELIVERED, TaskType.DELIVERED, 4),
    LAST_CARGO_TAKEN(VarbitID.PORT_TASK_LAST_CARGO_TAKEN, TaskType.OTHER, -1);

    private final int id;
    @Getter
    private final TaskType type;
    private final int slot;

    public enum TaskType
    {
        ID, TAKEN, DELIVERED, OTHER
    }

    PortTaskVarbits(int id, TaskType type, int slot)
    {
        this.id = id;
        this.type = type;
        this.slot = slot;
    }

    public int getId()
    {
        return id;
    }

    public TaskType getType()
    {
        return type;
    }

    public int getSlot()
    {
        return slot;
    }

    private static final Map<Integer, PortTaskVarbits> lookup = new HashMap<>();
    static
    {
        for (PortTaskVarbits v : values())
        {
            lookup.put(v.id, v);
        }
    }

    public static PortTaskVarbits fromId(int id)
    {
        return lookup.get(id);
    }

    public static boolean contains(int id)
    {
        return lookup.containsKey(id);
    }

    @Override
    public String toString()
    {
        return String.format("%s (Type: %s, Slot: %d)", name(), type, slot);
    }

}

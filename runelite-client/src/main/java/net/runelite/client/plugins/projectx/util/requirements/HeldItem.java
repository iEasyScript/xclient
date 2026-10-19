package net.runelite.client.plugins.projectx.util.requirements;

import java.util.Objects;

/**
 * An item stack the player has somewhere: id, name and quantity. A plain snapshot, so the
 * requirement logic can be tested without a game client.
 */
public final class HeldItem
{
    private final int id;
    private final String name;
    private final int quantity;

    public HeldItem(int id, String name, int quantity)
    {
        this.id = id;
        this.name = name == null ? "" : name;
        this.quantity = quantity;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public int getQuantity()
    {
        return quantity;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof HeldItem))
        {
            return false;
        }
        HeldItem other = (HeldItem) o;
        return id == other.id && quantity == other.quantity && name.equals(other.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, name, quantity);
    }

    @Override
    public String toString()
    {
        return name + " (" + id + ") x" + quantity;
    }
}

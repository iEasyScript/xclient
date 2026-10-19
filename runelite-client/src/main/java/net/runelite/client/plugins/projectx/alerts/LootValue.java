package net.runelite.client.plugins.projectx.alerts;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.Value;

/** Whether a drop is worth an alert, and how to describe it. Prices are looked up by the caller. */
final class LootValue
{
    /** The most valuable stacks listed in an alert; the rest are summed up in one line. */
    static final int LISTED = 5;

    private LootValue()
    {
    }

    @Value
    static class Stack
    {
        String name;
        long quantity;
        /** Price of one, in gp; 0 or less for untradeable or unknown. */
        long unitPrice;

        long value()
        {
            if (quantity <= 0 || unitPrice <= 0)
            {
                return 0;
            }
            // Saturate rather than wrap: a max stack of something dear must not read as negative.
            return unitPrice > Long.MAX_VALUE / quantity ? Long.MAX_VALUE : unitPrice * quantity;
        }
    }

    static long total(List<Stack> stacks)
    {
        long total = 0;
        for (Stack s : stacks)
        {
            long v = s.value();
            total = Long.MAX_VALUE - total < v ? Long.MAX_VALUE : total + v;
        }
        return total;
    }

    /** At or above the threshold, and worth something at all. */
    static boolean isValuable(long total, long threshold)
    {
        return total > 0 && total >= Math.max(1, threshold);
    }

    /** The priced stacks, dearest first, one per line; the rest folded into a last line. */
    static String describe(List<Stack> stacks)
    {
        List<Stack> priced = new ArrayList<>();
        for (Stack s : stacks)
        {
            if (s.value() > 0)
            {
                priced.add(s);
            }
        }
        priced.sort(Comparator.comparingLong(Stack::value).reversed());
        StringBuilder sb = new StringBuilder();
        long rest = 0;
        int restCount = 0;
        for (int i = 0; i < priced.size(); i++)
        {
            Stack s = priced.get(i);
            if (i >= LISTED)
            {
                rest += s.value();
                restCount++;
                continue;
            }
            if (sb.length() > 0)
            {
                sb.append('\n');
            }
            sb.append(s.getQuantity() > 1 ? formatGp(s.getQuantity()) + " x " : "")
                .append(s.getName()).append(" (").append(formatGp(s.value())).append(" gp)");
        }
        if (restCount > 0)
        {
            sb.append('\n').append(restCount).append(restCount == 1 ? " other item (" : " other items (")
                .append(formatGp(rest)).append(" gp)");
        }
        return sb.toString();
    }

    static String formatGp(long gp)
    {
        return NumberFormat.getIntegerInstance(Locale.UK).format(gp);
    }
}

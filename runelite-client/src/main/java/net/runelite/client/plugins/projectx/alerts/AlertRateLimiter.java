package net.runelite.client.plugins.projectx.alerts;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * At most one alert per type per window. Alerts are queued with {@link #offer}
 * and handed out by {@link #drain}; everything of one type that arrives before
 * its window is open again goes out as a single collapsed alert, so a burst
 * (three level-ups on one tick, a script stop storm) is one message, not many.
 *
 * <p>Thread-safe. Time is passed in, so it is testable without a clock.
 */
final class AlertRateLimiter
{
    /** Alerts kept per type while waiting; more than this are only counted. */
    static final int MAX_KEPT = 10;

    private final long windowMs;
    private final Map<AlertType, List<Alert>> pending = new EnumMap<>(AlertType.class);
    private final Map<AlertType, Integer> dropped = new EnumMap<>(AlertType.class);
    private final Map<AlertType, Long> lastSent = new EnumMap<>(AlertType.class);

    AlertRateLimiter(long windowMs)
    {
        this.windowMs = windowMs;
    }

    synchronized void offer(Alert alert)
    {
        List<Alert> list = pending.computeIfAbsent(alert.getType(), t -> new ArrayList<>());
        if (list.size() < MAX_KEPT)
        {
            list.add(alert);
        }
        else
        {
            dropped.merge(alert.getType(), 1, Integer::sum);
        }
    }

    /** The alerts that may go out now, one per type at most, each collapsed from what was waiting. */
    synchronized List<Alert> drain(long now)
    {
        List<Alert> out = new ArrayList<>();
        for (AlertType type : AlertType.values())
        {
            List<Alert> list = pending.get(type);
            if (list == null || list.isEmpty())
            {
                continue;
            }
            Long last = lastSent.get(type);
            if (last != null && now - last < windowMs)
            {
                continue;
            }
            int extra = dropped.getOrDefault(type, 0);
            out.add(collapse(list, extra));
            list.clear();
            dropped.remove(type);
            lastSent.put(type, now);
        }
        return out;
    }

    synchronized void clear()
    {
        pending.clear();
        dropped.clear();
    }

    /** One alert standing for all of {@code alerts} (same type), plus {@code uncounted} not kept. */
    static Alert collapse(List<Alert> alerts, int uncounted)
    {
        Alert last = alerts.get(alerts.size() - 1);
        int total = alerts.size() + uncounted;
        if (total == 1)
        {
            return last;
        }
        StringBuilder description = new StringBuilder();
        for (Alert a : alerts)
        {
            if (description.length() > 0)
            {
                description.append('\n');
            }
            description.append("- ").append(a.getTitle());
            if (!a.getDescription().isEmpty())
            {
                description.append(": ").append(a.getDescription().replaceAll("\\s*\\R\\s*", " "));
            }
        }
        if (uncounted > 0)
        {
            description.append("\n...and ").append(uncounted).append(" more");
        }
        return new Alert(last.getType(), last.getType().getLabel() + " (x" + total + ")",
            description.toString(), null, last.getPlayer(), last.getAt());
    }
}

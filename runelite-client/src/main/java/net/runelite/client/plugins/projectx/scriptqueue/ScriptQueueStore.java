package net.runelite.client.plugins.projectx.scriptqueue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Saves and loads the named plans.
 *
 * <p>Keys, in the Script Queue's config group:
 * <ul>
 *   <li>{@code plans}: every plan, as a JSON list of {@link QueuePlan};</li>
 *   <li>{@code activePlan}: the name of the one showing;</li>
 *   <li>{@code steps} and {@code loop}: the single queue from before named plans. Read once,
 *   when there are no plans yet, to become the {@value QueuePlan#DEFAULT_NAME} plan; still
 *   written with the showing plan, so an older client finds a queue it can read.</li>
 * </ul>
 */
@Slf4j
final class ScriptQueueStore
{
    static final String PLANS = "plans";
    static final String ACTIVE = "activePlan";
    static final String LEGACY_STEPS = "steps";
    static final String LEGACY_LOOP = "loop";
    static final int MAX_NAME = 40;

    /** Where it is kept: the client's config, or a map in tests. */
    interface Config
    {
        String get(String key);

        void set(String key, String value);
    }

    /** The plans, and which one is showing. */
    static final class Plans
    {
        final List<QueuePlan> plans;
        final String active;

        Plans(List<QueuePlan> plans, String active)
        {
            this.plans = plans;
            this.active = active;
        }

        QueuePlan activePlan()
        {
            QueuePlan found = find(plans, active);
            return found != null ? found : plans.get(0);
        }
    }

    private final Config config;
    private final Gson gson;

    ScriptQueueStore(Config config, Gson gson)
    {
        this.config = config;
        this.gson = gson;
    }

    /** Never empty: with nothing saved there is one empty plan. */
    Plans load()
    {
        List<QueuePlan> plans = new ArrayList<>();
        String json = config.get(PLANS);
        if (json != null && !json.isEmpty())
        {
            try
            {
                List<QueuePlan> saved = gson.fromJson(json, new TypeToken<List<QueuePlan>>() { }.getType());
                if (saved != null)
                {
                    for (QueuePlan p : saved)
                    {
                        if (p != null && p.getName() != null && !p.getName().trim().isEmpty() && find(plans, p.getName()) == null)
                        {
                            plans.add(tidy(p));
                        }
                    }
                }
            }
            catch (RuntimeException e)
            {
                log.warn("Script queue: saved plans unreadable, starting from the single queue");
            }
        }
        if (plans.isEmpty())
        {
            plans.add(legacyPlan());
        }
        String active = config.get(ACTIVE);
        return new Plans(plans, find(plans, active) != null ? find(plans, active).getName() : plans.get(0).getName());
    }

    void save(List<QueuePlan> plans, String active)
    {
        for (QueuePlan p : plans)
        {
            tidy(p);
        }
        config.set(PLANS, gson.toJson(plans));
        config.set(ACTIVE, active);
        QueuePlan showing = find(plans, active);
        if (showing != null)
        {
            config.set(LEGACY_STEPS, gson.toJson(showing.steps()));
            config.set(LEGACY_LOOP, Boolean.toString(showing.isLoop()));
        }
    }

    /** The queue from before named plans, as the default plan; empty if there was none. */
    private QueuePlan legacyPlan()
    {
        QueuePlan plan = new QueuePlan(QueuePlan.DEFAULT_NAME);
        String json = config.get(LEGACY_STEPS);
        if (json != null && !json.isEmpty())
        {
            try
            {
                List<QueueStep> saved = gson.fromJson(json, new TypeToken<List<QueueStep>>() { }.getType());
                if (saved != null)
                {
                    for (QueueStep s : saved)
                    {
                        if (s != null)
                        {
                            plan.steps().add(s);
                        }
                    }
                }
            }
            catch (RuntimeException ignored)
            {
                // An unreadable saved queue is just an empty one.
            }
        }
        plan.setLoop(Boolean.parseBoolean(config.get(LEGACY_LOOP)));
        return tidy(plan);
    }

    /** Fills in what an older save left out: the conditions list and the failure setting. */
    private static QueuePlan tidy(QueuePlan plan)
    {
        plan.setName(plan.getName() == null ? QueuePlan.DEFAULT_NAME : plan.getName().trim());
        plan.steps().removeIf(s -> s == null);
        for (QueueStep s : plan.steps())
        {
            List<StopCondition> conditions = new ArrayList<>(s.conditions());
            conditions.removeIf(c -> c == null || c.getType() == null);
            // Also brings the old single condition into line, for an older client.
            s.setConditions(conditions);
            if (s.getOnFailure() == null)
            {
                s.setOnFailure(QueueStep.OnFailure.SKIP);
            }
        }
        plan.setRounds(Math.max(0, plan.getRounds()));
        return plan;
    }

    static QueuePlan find(List<QueuePlan> plans, String name)
    {
        if (name == null)
        {
            return null;
        }
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        for (QueuePlan p : plans)
        {
            if (p.getName() != null && p.getName().trim().toLowerCase(Locale.ROOT).equals(wanted))
            {
                return p;
            }
        }
        return null;
    }

    /** A name for a plan, tidied, or null if it can't be one: blank, or another plan's. */
    static String validName(List<QueuePlan> plans, String name, QueuePlan renaming)
    {
        if (name == null)
        {
            return null;
        }
        String n = name.trim().replaceAll("\\s+", " ");
        if (n.isEmpty())
        {
            return null;
        }
        if (n.length() > MAX_NAME)
        {
            n = n.substring(0, MAX_NAME).trim();
        }
        QueuePlan same = find(plans, n);
        return same == null || same == renaming ? n : null;
    }

    /** A deep copy, through JSON: what a running queue works from, so edits don't reach it. */
    QueuePlan copy(QueuePlan plan)
    {
        return tidy(gson.fromJson(gson.toJson(plan), QueuePlan.class));
    }
}

package net.runelite.client.plugins.projectx.util.requirements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Every requirement of one script and what its check found, worst first:
 * <ol>
 *   <li>required and unmet,</li>
 *   <li>required and unknown,</li>
 *   <li>recommended and unmet,</li>
 *   <li>recommended and unknown,</li>
 *   <li>met.</li>
 * </ol>
 * Within each group the order the script declared them in is kept.
 */
public final class RequirementReport
{
    private final String scriptName;
    private final List<RequirementOutcome> outcomes;

    private RequirementReport(String scriptName, List<RequirementOutcome> outcomes)
    {
        this.scriptName = scriptName == null ? "" : scriptName;
        List<RequirementOutcome> sorted = new ArrayList<>(outcomes);
        // List.sort is stable, so declaration order survives within a group.
        sorted.sort(Comparator.comparingInt(RequirementReport::rank));
        this.outcomes = Collections.unmodifiableList(sorted);
    }

    public static RequirementReport of(String scriptName, List<RequirementOutcome> outcomes)
    {
        return new RequirementReport(scriptName, outcomes == null ? Collections.emptyList() : outcomes);
    }

    public static RequirementReport empty(String scriptName)
    {
        return of(scriptName, Collections.emptyList());
    }

    /** Runs every check against {@code context}. Pure: no client needed. */
    public static RequirementReport evaluate(String scriptName, List<Requirement> requirements, RequirementContext context)
    {
        List<RequirementOutcome> results = new ArrayList<>();
        for (Requirement r : Requirements.nonNull(requirements))
        {
            results.add(new RequirementOutcome(r, r.evaluate(context)));
        }
        return of(scriptName, results);
    }

    /** Every requirement reported unknown, with the same reason; for when the game can't be read at all. */
    public static RequirementReport allUnknown(String scriptName, List<Requirement> requirements, String reason)
    {
        List<RequirementOutcome> results = new ArrayList<>();
        for (Requirement r : Requirements.nonNull(requirements))
        {
            results.add(new RequirementOutcome(r, RequirementResult.unknown(reason)));
        }
        return of(scriptName, results);
    }

    static int rank(RequirementOutcome o)
    {
        if (o.getStatus() == RequirementStatus.MET)
        {
            return 4;
        }
        int base = o.getSeverity() == RequirementSeverity.REQUIRED ? 0 : 2;
        return base + (o.getStatus() == RequirementStatus.UNKNOWN ? 1 : 0);
    }

    public String getScriptName()
    {
        return scriptName;
    }

    /** Worst first; see the class comment. */
    public List<RequirementOutcome> getOutcomes()
    {
        return outcomes;
    }

    /** Nothing was declared or inferred. */
    public boolean isEmpty()
    {
        return outcomes.isEmpty();
    }

    public boolean allMet()
    {
        return outcomes.stream().allMatch(o -> o.getStatus() == RequirementStatus.MET);
    }

    public long count(RequirementStatus status)
    {
        return outcomes.stream().filter(o -> o.getStatus() == status).count();
    }

    public long count(RequirementStatus status, RequirementSeverity severity)
    {
        return outcomes.stream().filter(o -> o.getStatus() == status && o.getSeverity() == severity).count();
    }

    /** A required requirement is known to be missing. */
    public boolean hasUnmetRequired()
    {
        return count(RequirementStatus.UNMET, RequirementSeverity.REQUIRED) > 0;
    }

    /**
     * Whether the player should see the checklist before the script starts: a required
     * requirement is unmet or unknown, or (when {@code includeRecommended}) a recommended one
     * is unmet. A recommended requirement that is only unknown never stops a start.
     */
    public boolean needsAttention(boolean includeRecommended)
    {
        for (RequirementOutcome o : outcomes)
        {
            if (o.getStatus() == RequirementStatus.MET)
            {
                continue;
            }
            if (o.getSeverity() == RequirementSeverity.REQUIRED)
            {
                return true;
            }
            if (includeRecommended && o.getStatus() == RequirementStatus.UNMET)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * One line for the player: "All 5 requirements met", or "2 missing, 1 recommended missing,
     * 1 unknown, 3 met".
     */
    public String summary()
    {
        if (outcomes.isEmpty())
        {
            return "Nothing to check";
        }
        if (allMet())
        {
            return outcomes.size() == 1 ? "Requirement met" : "All " + outcomes.size() + " requirements met";
        }
        List<String> parts = new ArrayList<>();
        long missingRequired = count(RequirementStatus.UNMET, RequirementSeverity.REQUIRED);
        long missingRecommended = count(RequirementStatus.UNMET, RequirementSeverity.RECOMMENDED);
        long unknown = count(RequirementStatus.UNKNOWN);
        long met = count(RequirementStatus.MET);
        if (missingRequired > 0)
        {
            parts.add(missingRequired + " missing");
        }
        if (missingRecommended > 0)
        {
            parts.add(missingRecommended + " recommended missing");
        }
        if (unknown > 0)
        {
            parts.add(unknown + " unknown");
        }
        if (met > 0)
        {
            parts.add(met + " met");
        }
        return String.join(", ", parts);
    }

    /** Every line, for logs and the script queue. */
    @Override
    public String toString()
    {
        return scriptName + ": " + summary() + (outcomes.isEmpty() ? "" : "\n  "
            + outcomes.stream().map(RequirementOutcome::toString).collect(Collectors.joining("\n  ")));
    }
}

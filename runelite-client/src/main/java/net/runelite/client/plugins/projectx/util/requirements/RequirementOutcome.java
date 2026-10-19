package net.runelite.client.plugins.projectx.util.requirements;

import java.util.Objects;

/**
 * A {@link Requirement} and what its check found.
 */
public final class RequirementOutcome
{
    private final Requirement requirement;
    private final RequirementResult result;

    public RequirementOutcome(Requirement requirement, RequirementResult result)
    {
        this.requirement = Objects.requireNonNull(requirement, "requirement");
        this.result = Objects.requireNonNull(result, "result");
    }

    public Requirement getRequirement()
    {
        return requirement;
    }

    public RequirementResult getResult()
    {
        return result;
    }

    public RequirementStatus getStatus()
    {
        return result.getStatus();
    }

    public RequirementSeverity getSeverity()
    {
        return requirement.getSeverity();
    }

    public String getDescription()
    {
        return requirement.getDescription();
    }

    public String getDetail()
    {
        return result.getDetail();
    }

    /** "[UNMET] Magic 55: Magic 55 needed, you have 43". */
    @Override
    public String toString()
    {
        return "[" + result.getStatus() + (requirement.getSeverity() == RequirementSeverity.RECOMMENDED ? ", recommended" : "")
            + "] " + requirement.getDescription() + (result.getDetail().isEmpty() ? "" : ": " + result.getDetail());
    }
}

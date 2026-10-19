package net.runelite.client.plugins.projectx.util.requirements;

import java.util.Objects;
import java.util.function.Function;

/**
 * One thing a script needs before it starts: a description the player reads ("Magic 55"),
 * a check against the game, and whether it is {@link RequirementSeverity#REQUIRED required}
 * or only {@link RequirementSeverity#RECOMMENDED recommended}.
 *
 * <p>Build them with the factories in {@link Requirements}, or write your own check:
 * <pre>{@code
 * Requirement.of("Bones to peaches unlocked", ctx ->
 *     ctx.isLoggedIn() ? RequirementResult.met("Unlocked") : RequirementResult.unknown("Log in to check"));
 * }</pre>
 *
 * <p>Checks run on the client thread and must only read state: never click, sleep or wait.
 */
public final class Requirement
{
    private final String description;
    private final RequirementSeverity severity;
    private final Function<RequirementContext, RequirementResult> check;
    private final String key;

    private Requirement(String description, RequirementSeverity severity,
                        Function<RequirementContext, RequirementResult> check, String key)
    {
        this.description = Objects.requireNonNull(description, "description");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.check = Objects.requireNonNull(check, "check");
        this.key = key;
    }

    /** A required requirement with a custom check. */
    public static Requirement of(String description, Function<RequirementContext, RequirementResult> check)
    {
        return new Requirement(description, RequirementSeverity.REQUIRED, check, null);
    }

    /** The same requirement, but only recommended. */
    public Requirement recommended()
    {
        return new Requirement(description, RequirementSeverity.RECOMMENDED, check, key);
    }

    /** The same requirement, but required. */
    public Requirement required()
    {
        return new Requirement(description, RequirementSeverity.REQUIRED, check, key);
    }

    /** The same requirement, with a new description. */
    public Requirement describedAs(String newDescription)
    {
        return new Requirement(newDescription, severity, check, key);
    }

    /**
     * The same requirement with a key: two requirements with the same key are the same
     * thing, so a declared one replaces an inferred one.
     */
    public Requirement withKey(String newKey)
    {
        return new Requirement(description, severity, check, newKey);
    }

    /**
     * Runs the check. Never throws: a check that fails or answers nothing is reported as unknown.
     */
    public RequirementResult evaluate(RequirementContext context)
    {
        try
        {
            RequirementResult result = check.apply(context);
            return result != null ? result : RequirementResult.unknown("Couldn't check");
        }
        catch (RuntimeException e)
        {
            String why = e.getMessage();
            return RequirementResult.unknown("Couldn't check" + (why == null || why.isBlank() ? "" : ": " + why));
        }
    }

    public String getDescription()
    {
        return description;
    }

    public RequirementSeverity getSeverity()
    {
        return severity;
    }

    /** Null when the requirement has no identity beyond itself. */
    public String getKey()
    {
        return key;
    }

    @Override
    public String toString()
    {
        return description + " (" + severity + ")";
    }
}

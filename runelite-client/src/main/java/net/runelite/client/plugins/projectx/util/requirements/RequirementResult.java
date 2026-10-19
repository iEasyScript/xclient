package net.runelite.client.plugins.projectx.util.requirements;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The answer of one {@link Requirement} check: met, unmet or unknown, with a short
 * detail for the player such as "Magic 55 needed, you have 43", and optional extra
 * lines (for example each item an inventory setup is missing).
 */
public final class RequirementResult
{
    private final RequirementStatus status;
    private final String detail;
    private final List<String> notes;

    private RequirementResult(RequirementStatus status, String detail, List<String> notes)
    {
        this.status = Objects.requireNonNull(status, "status");
        this.detail = detail == null ? "" : detail;
        this.notes = notes == null ? Collections.emptyList() : Collections.unmodifiableList(notes);
    }

    public static RequirementResult met(String detail)
    {
        return new RequirementResult(RequirementStatus.MET, detail, null);
    }

    public static RequirementResult unmet(String detail)
    {
        return new RequirementResult(RequirementStatus.UNMET, detail, null);
    }

    public static RequirementResult unknown(String detail)
    {
        return new RequirementResult(RequirementStatus.UNKNOWN, detail, null);
    }

    /** A copy with extra lines shown under the detail. */
    public RequirementResult withNotes(List<String> notes)
    {
        return new RequirementResult(status, detail, notes == null ? null : List.copyOf(notes));
    }

    public RequirementStatus getStatus()
    {
        return status;
    }

    public String getDetail()
    {
        return detail;
    }

    public List<String> getNotes()
    {
        return notes;
    }

    public boolean isMet()
    {
        return status == RequirementStatus.MET;
    }

    @Override
    public String toString()
    {
        return status + (detail.isEmpty() ? "" : ": " + detail);
    }
}

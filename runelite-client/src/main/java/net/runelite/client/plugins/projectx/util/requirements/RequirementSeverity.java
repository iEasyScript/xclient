package net.runelite.client.plugins.projectx.util.requirements;

/**
 * How much a {@link Requirement} matters to the script.
 */
public enum RequirementSeverity
{
    /** The script won't work without it. */
    REQUIRED,
    /** The script works without it, but worse, or only in some modes. */
    RECOMMENDED
}

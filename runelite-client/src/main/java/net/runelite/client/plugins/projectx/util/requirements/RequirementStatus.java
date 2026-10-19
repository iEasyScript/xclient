package net.runelite.client.plugins.projectx.util.requirements;

/**
 * What a {@link Requirement} check found.
 */
public enum RequirementStatus
{
    /** The player has it. */
    MET,
    /** The player does not have it. */
    UNMET,
    /** The client can't tell yet, e.g. the bank has not been opened, or the player is logged out. */
    UNKNOWN
}

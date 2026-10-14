package net.runelite.client.plugins.projectx.util.reliability;

/** How a guarded step ended. */
public enum Outcome
{
    /** The action ran and, where a check was given, the check confirmed it. */
    SUCCESS,
    /** It did not work this time, but trying again or recovering could fix it. */
    RECOVERABLE,
    /** Carrying on cannot help: the script should stop. */
    UNRECOVERABLE
}

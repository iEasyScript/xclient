package net.runelite.client.plugins.projectx.util.walker.door.model;

/**
 * Failure outcomes returned by door interaction/traversal helpers.
 * Callers can use these values to decide between retrying, backing off, or surfacing a user-facing failure cause.
 */
public enum DoorFailureReason {
    /** Per-edge throttle active (same door edge recently attempted); short retry after edge cooldown. */
    THROTTLED_EDGE,
    /** Global throttle active (all door interactions cooled down); retry after global cooldown. */
    THROTTLED_GLOBAL,
    /** Interaction threw unexpectedly; retry only after validating target object/client state. */
    INTERACT_EXCEPTION,
    /** Interaction call returned failure/no click acknowledgement. */
    INTERACT_FAILED,
    /** Click happened but traversal was not observed within await window. */
    NOT_TRAVERSED,
    /** Door blocked by quest progression or unmet quest requirement. */
    QUEST_LOCKED
}

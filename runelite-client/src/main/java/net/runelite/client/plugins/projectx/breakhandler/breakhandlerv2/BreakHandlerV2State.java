package net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicReference;

/**
 * State machine for Break Handler V2
 * Manages all states and transitions for the break system
 */
@Getter
@RequiredArgsConstructor
public enum BreakHandlerV2State {
    WAITING_FOR_BREAK("Waiting for break"),
    BREAK_REQUESTED("Break requested"),
    INITIATING_BREAK("Initiating break"),
    LOGOUT_REQUESTED("Logout requested"),
    LOGGED_OUT("Logged out"),
    LOGIN_REQUESTED("Login requested"),
    LOGGING_IN("Logging in"),
    LOGIN_EXTENDED_SLEEP("Login extended sleep"),
    BREAK_ENDING("Break ending"),
    PROFILE_SWITCHING("Switching profile");

    private final String description;

    private static final AtomicReference<BreakHandlerV2State> currentState =
        new AtomicReference<>(WAITING_FOR_BREAK);

    /**
     * Get the current state (thread-safe)
     */
    public static BreakHandlerV2State getCurrentState() {
        return currentState.get();
    }

    /**
     * Set the current state (thread-safe)
     */
    public static void setState(BreakHandlerV2State newState) {
        currentState.set(newState);
    }

    /**
     * Check if break is currently active
     */
    public static boolean isBreakActive() {
        BreakHandlerV2State state = getCurrentState();
        return state == BREAK_REQUESTED ||
               state == INITIATING_BREAK ||
               state == LOGOUT_REQUESTED ||
               state == LOGGED_OUT ||
               state == LOGIN_REQUESTED ||
               state == LOGGING_IN ||
               state == LOGIN_EXTENDED_SLEEP ||
               state == BREAK_ENDING ||
               state == PROFILE_SWITCHING;
    }

    /**
     * Check if this is a lock state that prevents new breaks
     */
    public boolean isLockState() {
        return this == BREAK_REQUESTED ||
               this == INITIATING_BREAK ||
               this == LOGOUT_REQUESTED ||
               this == LOGGING_IN ||
               this == BREAK_ENDING ||
               this == PROFILE_SWITCHING;
    }

    @Override
    public String toString() {
        return description;
    }
}

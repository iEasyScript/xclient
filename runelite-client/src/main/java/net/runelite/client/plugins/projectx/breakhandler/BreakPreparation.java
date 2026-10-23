package net.runelite.client.plugins.projectx.breakhandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Cooperative preparation before a break handler pauses, stops, or logs out a script.
 *
 * <p>A plugin that must leave a dangerous area before a break registers a {@link Handle} on start,
 * checks {@link Handle#isRequested()} from its normal non-blocking loop, reaches safety over
 * ordinary loop iterations, then calls {@link Handle#ready()}. It closes the handle on stop,
 * including hot reload. Both break handlers wait until every registered owner is ready. Closing
 * an unready owner during a request, or a 120-second timeout, cancels that break instead of
 * claiming the player is safe. With no participants registered, break behaviour is unchanged.
 */
public final class BreakPreparation
{
    private static final long TIMEOUT_MS = 120_000;

    private static final Map<String, Handle> OWNERS = new HashMap<>();
    private static boolean requested;
    private static long requestedAt;
    private static boolean aborted;

    private BreakPreparation() {}

    /**
     * Opts an owner into preparation. Re-registering the same name replaces its handle;
     * the old handle can no longer report readiness or unregister the replacement.
     */
    public static synchronized Handle register(String owner)
    {
        Handle handle = new Handle(owner);
        OWNERS.put(owner, handle);
        return handle;
    }

    /** Returns whether any plugin has opted in; no participants means no preparation deferral. */
    public static synchronized boolean hasParticipants()
    {
        return !OWNERS.isEmpty();
    }

    /**
     * Requests preparation and reports whether an owner is still unready.
     * Handlers call this before a break starts, not while a timed break is already active.
     */
    public static synchronized boolean shouldDeferBreak()
    {
        if (!requested)
        {
            requestedAt = System.currentTimeMillis();
        }
        requested = true;
        return OWNERS.values().stream().anyMatch(handle -> !handle.ready);
    }

    /** Reports an unsafe owner shutdown or a 120-second timeout; the handler must cancel the request. */
    public static synchronized boolean isAborted()
    {
        return aborted || (requested && System.currentTimeMillis() - requestedAt >= TIMEOUT_MS
                && OWNERS.values().stream().anyMatch(handle -> !handle.ready));
    }

    /** Clears the request and readiness on completion, cancellation or handler restart, retaining owners. */
    public static synchronized void finishBreak()
    {
        requested = false;
        aborted = false;
        OWNERS.values().forEach(handle -> handle.ready = false);
    }

    /** A plugin-owned registration, closed on shutdown, including hot reload. */
    public static final class Handle implements AutoCloseable
    {
        private final String owner;
        private boolean ready;

        private Handle(String owner)
        {
            this.owner = owner;
        }

        /** Returns whether this current registration has been asked to reach safety. */
        public boolean isRequested()
        {
            synchronized (BreakPreparation.class)
            {
                return OWNERS.get(owner) == this && requested;
            }
        }

        /** Reports safety for the current request; stale handles and calls before a request are ignored. */
        public void ready()
        {
            synchronized (BreakPreparation.class)
            {
                if (OWNERS.get(owner) == this && requested)
                {
                    ready = true;
                }
            }
        }

        /** Unregisters this handle; closing while unready cancels a pending request as unsafe. */
        @Override
        public void close()
        {
            synchronized (BreakPreparation.class)
            {
                if (OWNERS.get(owner) == this && requested && !ready)
                {
                    aborted = true;
                }
                OWNERS.remove(owner, this);
            }
        }
    }
}

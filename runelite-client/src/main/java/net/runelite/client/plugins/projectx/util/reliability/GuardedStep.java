package net.runelite.client.plugins.projectx.util.reliability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * One step of a script, described before it runs: what must hold first, what to do, how
 * to tell it worked, and how hard to try. Made by {@link ScriptReliability#step(String)};
 * nothing happens until {@link #run()}.
 *
 * <pre>{@code
 * ActionResult r = reliability().step("Deposit logs")
 *     .require(Preconditions.bankOpen())
 *     .action(() -> Rs2Bank.depositAll("Logs"))
 *     .verify(() -> !Rs2Inventory.hasItem("Logs"), 3_000)
 *     .run();
 * }</pre>
 */
public final class GuardedStep
{
    /** Default wait between checks of {@link #verify}'s condition. */
    public static final long DEFAULT_POLL_MS = 100;

    private final ScriptReliability owner;
    private final String name;
    private final List<Precondition> preconditions = new ArrayList<>();
    private BooleanSupplier action;
    private BooleanSupplier verify;
    private long verifyTimeoutMs;
    private long pollMs = DEFAULT_POLL_MS;
    private int maxRetries = -1;
    private long backoffBaseMs = Backoff.DEFAULT_BASE_MS;
    private long backoffMaxMs = Backoff.DEFAULT_MAX_MS;
    private Function<Throwable, Outcome> classifier;

    GuardedStep(ScriptReliability owner, String name)
    {
        this.owner = owner;
        this.name = name == null || name.trim().isEmpty() ? "unnamed step" : name.trim();
    }

    /** Checks that must all hold before (and between) attempts. */
    public GuardedStep require(Precondition... checks)
    {
        for (Precondition p : checks)
        {
            preconditions.add(Objects.requireNonNull(p, "precondition"));
        }
        return this;
    }

    /**
     * The action. Return false when it could not even be carried out (no target found, the
     * menu option was missing); that attempt fails without waiting for {@link #verify}.
     */
    public GuardedStep action(BooleanSupplier action)
    {
        this.action = Objects.requireNonNull(action, "action");
        return this;
    }

    /** The action, for calls that return nothing. */
    public GuardedStep perform(Runnable action)
    {
        Objects.requireNonNull(action, "action");
        this.action = () ->
        {
            action.run();
            return true;
        };
        return this;
    }

    /**
     * How to tell the action worked: polled every {@link #pollEvery} ms for up to
     * {@code timeoutMs}. Without it, an action that returns true counts as success.
     */
    public GuardedStep verify(BooleanSupplier condition, long timeoutMs)
    {
        this.verify = Objects.requireNonNull(condition, "condition");
        this.verifyTimeoutMs = Math.max(0, timeoutMs);
        return this;
    }

    public GuardedStep pollEvery(long millis)
    {
        this.pollMs = Math.max(10, millis);
        return this;
    }

    /**
     * Retries for this step. Never more than the player's "Retries per action" setting:
     * a script can ask for fewer, not more.
     */
    public GuardedStep maxRetries(int retries)
    {
        this.maxRetries = Math.max(0, retries);
        return this;
    }

    /** Wait between attempts: {@code baseMs}, doubling each time, at most {@code maxMs}. */
    public GuardedStep backoff(long baseMs, long maxMs)
    {
        this.backoffBaseMs = Math.max(0, baseMs);
        this.backoffMaxMs = Math.max(this.backoffBaseMs, maxMs);
        return this;
    }

    /**
     * Decides whether an exception from the action is worth retrying. Return null to fall
     * back to the default: {@link UnrecoverableActionException} is unrecoverable, anything
     * else recoverable. An interrupt always means the script is being switched off.
     */
    public GuardedStep classifier(Function<Throwable, Outcome> classifier)
    {
        this.classifier = classifier;
        return this;
    }

    /** Runs the step on the calling script thread and returns how it went. */
    public ActionResult run()
    {
        if (action == null)
        {
            throw new IllegalStateException("Guarded step '" + name + "' has no action");
        }
        return owner.execute(this);
    }

    String getName()
    {
        return name;
    }

    List<Precondition> getPreconditions()
    {
        return Collections.unmodifiableList(preconditions);
    }

    BooleanSupplier getAction()
    {
        return action;
    }

    BooleanSupplier getVerify()
    {
        return verify;
    }

    long getVerifyTimeoutMs()
    {
        return verifyTimeoutMs;
    }

    long getPollMs()
    {
        return pollMs;
    }

    /** -1 when the step did not ask for a number of its own. */
    int getMaxRetries()
    {
        return maxRetries;
    }

    long getBackoffBaseMs()
    {
        return backoffBaseMs;
    }

    long getBackoffMaxMs()
    {
        return backoffMaxMs;
    }

    Outcome classify(Throwable t)
    {
        if (classifier != null)
        {
            Outcome o = classifier.apply(t);
            if (o != null && o != Outcome.SUCCESS)
            {
                return o;
            }
        }
        return t instanceof UnrecoverableActionException ? Outcome.UNRECOVERABLE : Outcome.RECOVERABLE;
    }
}

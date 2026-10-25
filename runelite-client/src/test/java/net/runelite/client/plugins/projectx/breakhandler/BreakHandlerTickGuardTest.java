package net.runelite.client.plugins.projectx.breakhandler;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BreakHandlerTickGuardTest
{
    @Test
    public void awaitIdleWaitsForTheRunningTick() throws Exception
    {
        BreakHandlerTickGuard guard = new BreakHandlerTickGuard();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread tick = new Thread(() -> guard.runTick(() -> {
            entered.countDown();
            awaitQuietly(release);
        }));
        tick.start();
        assertTrue(entered.await(2, TimeUnit.SECONDS));

        guard.stop();
        assertFalse(guard.awaitIdle(100));

        release.countDown();
        assertTrue(guard.awaitIdle(2000));
        tick.join(2000);
    }

    @Test
    public void ticksAfterStopDoNotRun()
    {
        BreakHandlerTickGuard guard = new BreakHandlerTickGuard();
        AtomicBoolean ran = new AtomicBoolean();

        guard.stop();
        guard.runTick(() -> ran.set(true));

        assertFalse(ran.get());
        assertTrue(guard.awaitIdle(0));
    }

    @Test
    public void startAllowsTicksAgain()
    {
        BreakHandlerTickGuard guard = new BreakHandlerTickGuard();
        AtomicBoolean ran = new AtomicBoolean();

        guard.stop();
        guard.start();
        guard.runTick(() -> ran.set(true));

        assertTrue(ran.get());
    }

    @Test
    public void awaitIdleFromTheTickThreadDoesNotDeadlock()
    {
        BreakHandlerTickGuard guard = new BreakHandlerTickGuard();
        AtomicBoolean idle = new AtomicBoolean();

        guard.runTick(() -> {
            guard.stop();
            idle.set(guard.awaitIdle(100));
        });

        assertTrue(idle.get());
    }

    private static void awaitQuietly(CountDownLatch latch)
    {
        try
        {
            latch.await(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}

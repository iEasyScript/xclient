package net.runelite.client.plugins.projectx;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScriptExecutorLeakTest {
    @Test public void idleCoreThreadsTimeOut() {
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) new Fixture().scheduledExecutorService;
        try {
            assertTrue(executor.allowsCoreThreadTimeOut());
            assertTrue(executor.getRemoveOnCancelPolicy());
            assertEquals(Script.EXECUTOR_KEEP_ALIVE_SECONDS, executor.getKeepAliveTime(TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test public void stoppedLoopReleasesItsThreadsAndCanRestart() throws Exception {
        Fixture script = new Fixture();
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) script.scheduledExecutorService;
        executor.setKeepAliveTime(100, TimeUnit.MILLISECONDS);
        try {
            CountDownLatch ran = new CountDownLatch(15);
            ScheduledFuture<?> loop = executor.scheduleWithFixedDelay(ran::countDown, 0, 10, TimeUnit.MILLISECONDS);
            assertTrue(ran.await(5, TimeUnit.SECONDS));
            assertTrue(executor.getPoolSize() > 1);
            loop.cancel(true);

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (executor.getPoolSize() > 0 && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertEquals(0, executor.getPoolSize());

            CountDownLatch restarted = new CountDownLatch(3);
            ScheduledFuture<?> again = executor.scheduleWithFixedDelay(restarted::countDown, 0, 10, TimeUnit.MILLISECONDS);
            assertTrue(restarted.await(5, TimeUnit.SECONDS));
            again.cancel(true);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test public void cancelledLongDelayLoopReleasesLastThread() throws Exception {
        Fixture script = new Fixture();
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) script.scheduledExecutorService;
        executor.setKeepAliveTime(100, TimeUnit.MILLISECONDS);
        try {
            CountDownLatch ran = new CountDownLatch(1);
            ScheduledFuture<?> loop = executor.scheduleWithFixedDelay(ran::countDown, 0, 1, TimeUnit.DAYS);
            assertTrue(ran.await(5, TimeUnit.SECONDS));
            loop.cancel(true);
            assertTrue(executor.getQueue().isEmpty());

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (executor.getPoolSize() > 0 && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertEquals(0, executor.getPoolSize());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test public void loopDelayLongerThanKeepAliveKeepsRunning() throws Exception {
        Fixture script = new Fixture();
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) script.scheduledExecutorService;
        executor.setKeepAliveTime(50, TimeUnit.MILLISECONDS);
        try {
            CountDownLatch ran = new CountDownLatch(3);
            executor.scheduleWithFixedDelay(ran::countDown, 0, 200, TimeUnit.MILLISECONDS);
            assertTrue(ran.await(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class Fixture extends Script {
    }
}

package net.runelite.client.plugins.projectx.breakhandler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class BreakHandlerTickGuard
{
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean stopping;

    public void start()
    {
        stopping = false;
    }

    public void runTick(Runnable tick)
    {
        lock.lock();
        try
        {
            if (!stopping)
            {
                tick.run();
            }
        }
        finally
        {
            lock.unlock();
        }
    }

    public void stop()
    {
        stopping = true;
    }

    public boolean awaitIdle(long timeoutMs)
    {
        try
        {
            if (lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS))
            {
                lock.unlock();
                return true;
            }
            return false;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

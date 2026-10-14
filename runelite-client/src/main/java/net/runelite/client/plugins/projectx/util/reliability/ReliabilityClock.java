package net.runelite.client.plugins.projectx.util.reliability;

/**
 * Time source and sleeper for the reliability service, so retry, backoff and stall logic
 * can be tested without real waiting. Scripts never need anything but {@link #SYSTEM}.
 */
public interface ReliabilityClock
{
    long nowMillis();

    /** Waits on the calling (script) thread. Never call this on the client thread. */
    void sleep(long millis) throws InterruptedException;

    ReliabilityClock SYSTEM = new ReliabilityClock()
    {
        @Override
        public long nowMillis()
        {
            return System.currentTimeMillis();
        }

        @Override
        public void sleep(long millis) throws InterruptedException
        {
            if (millis > 0)
            {
                Thread.sleep(millis);
            }
        }
    };
}

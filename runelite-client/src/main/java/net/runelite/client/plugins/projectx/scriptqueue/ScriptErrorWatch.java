package net.runelite.client.plugins.projectx.scriptqueue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Remembers the last few errors logged, so the queue can tell a script that stopped by
 * itself because it was done from one that stopped on an error: an error from the
 * script's own package shortly before it stopped. Errors only, and only the latest few;
 * nothing leaves the client.
 */
final class ScriptErrorWatch extends AppenderBase<ILoggingEvent>
{
    private static final int CAPACITY = 64;
    private static final int MAX_TEXT = 120;
    private static final ScriptErrorWatch INSTANCE = new ScriptErrorWatch();

    private static final class Entry
    {
        final long time;
        final String logger;
        final String text;

        Entry(long time, String logger, String text)
        {
            this.time = time;
            this.logger = logger;
            this.text = text;
        }
    }

    private final Deque<Entry> entries = new ArrayDeque<>(CAPACITY);

    private ScriptErrorWatch()
    {
        setName("PROJECTX_SCRIPT_QUEUE_ERRORS");
    }

    /** Starts watching, once. Safe to call more than once; does nothing without logback. */
    static void install()
    {
        synchronized (INSTANCE)
        {
            if (INSTANCE.isStarted() || !(LoggerFactory.getILoggerFactory() instanceof LoggerContext))
            {
                return;
            }
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            INSTANCE.setContext(context);
            INSTANCE.start();
            context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(INSTANCE);
        }
    }

    @Override
    protected void append(ILoggingEvent event)
    {
        if (!event.getLevel().isGreaterOrEqual(Level.ERROR))
        {
            return;
        }
        String text = event.getFormattedMessage();
        IThrowableProxy t = event.getThrowableProxy();
        if ((text == null || text.trim().isEmpty()) && t != null)
        {
            text = t.getClassName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
        }
        Entry entry = new Entry(event.getTimeStamp(), event.getLoggerName(), text == null ? "" : text.trim());
        synchronized (entries)
        {
            if (entries.size() >= CAPACITY)
            {
                entries.removeFirst();
            }
            entries.addLast(entry);
        }
    }

    /**
     * The latest error logged from {@code packageName} (or below it) at or after {@code sinceMillis},
     * shortened to a line; null if none.
     */
    static String latest(String packageName, long sinceMillis)
    {
        if (packageName == null || packageName.isEmpty())
        {
            return null;
        }
        synchronized (INSTANCE.entries)
        {
            Iterator<Entry> it = INSTANCE.entries.descendingIterator();
            while (it.hasNext())
            {
                Entry e = it.next();
                if (e.time < sinceMillis)
                {
                    return null;
                }
                if (e.logger != null && (e.logger.equals(packageName) || e.logger.startsWith(packageName + ".")))
                {
                    String line = e.text.isEmpty() ? "an error" : e.text.split("\\R", 2)[0];
                    return line.length() > MAX_TEXT ? line.substring(0, MAX_TEXT - 3) + "..." : line;
                }
            }
        }
        return null;
    }
}

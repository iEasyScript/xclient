package net.runelite.client.plugins.projectx;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The client's recent log, kept in memory so "Report a problem" can send what a
 * script was doing just before it went wrong. Nothing is written anywhere or sent
 * unless the player sends a report and leaves "include the log" ticked.
 */
public final class ProjectXLogBuffer extends AppenderBase<ILoggingEvent>
{
    private static final int CAPACITY = 20000;
    private static final int STACK_LINES = 12;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final ProjectXLogBuffer INSTANCE = new ProjectXLogBuffer();

    private static final class Line
    {
        final long time;
        final Level level;
        final String logger;
        final String text;

        Line(long time, Level level, String logger, String text)
        {
            this.time = time;
            this.level = level;
            this.logger = logger;
            this.text = text;
        }
    }

    private final Deque<Line> lines = new ArrayDeque<>(CAPACITY);

    private ProjectXLogBuffer()
    {
        setName("PROJECTX_RECENT_LOG");
    }

    /** Starts capturing, once. Safe to call more than once. */
    public static void install()
    {
        synchronized (INSTANCE)
        {
            if (INSTANCE.isStarted())
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
        StringBuilder text = new StringBuilder(event.getFormattedMessage() == null ? "" : event.getFormattedMessage());
        IThrowableProxy t = event.getThrowableProxy();
        int depth = 0;
        while (t != null && depth < 3)
        {
            text.append("\n    ").append(depth == 0 ? "" : "Caused by: ").append(t.getClassName()).append(": ").append(t.getMessage());
            StackTraceElementProxy[] stack = t.getStackTraceElementProxyArray();
            for (int i = 0; stack != null && i < Math.min(stack.length, STACK_LINES); i++)
            {
                text.append("\n        at ").append(stack[i].getStackTraceElement());
            }
            t = t.getCause();
            depth++;
        }
        Line line = new Line(event.getTimeStamp(), event.getLevel(), event.getLoggerName(), text.toString());
        synchronized (lines)
        {
            if (lines.size() >= CAPACITY)
            {
                lines.removeFirst();
            }
            lines.addLast(line);
        }
    }

    /** Where {@code ProjectX.log} writes: most scripts' status and debug lines go through it. */
    private static final String SHARED_LOGGER = "net.runelite.client.plugins.projectx.ProjectX";
    /** The walker, interactions and other helpers every script drives. */
    private static final String SHARED_UTIL_PREFIX = "net.runelite.client.plugins.projectx.util.";

    /**
     * The last {@code maxLines} lines from the past {@code minutes} minutes that came
     * from {@code packagePrefix} (the script itself), from the shared Project X logger
     * that scripts write through, from the client helpers a script drives (walking,
     * clicking), plus every warning and error from anywhere, which is where
     * client-side causes show up.
     */
    public static String recent(String packagePrefix, int minutes, int maxLines)
    {
        long since = System.currentTimeMillis() - minutes * 60_000L;
        List<Line> picked = new ArrayList<>();
        synchronized (INSTANCE.lines)
        {
            for (Line line : INSTANCE.lines)
            {
                if (line.time < since)
                {
                    continue;
                }
                boolean fromScript = packagePrefix != null && line.logger != null && line.logger.startsWith(packagePrefix);
                // A script's own trace usually goes through ProjectX.log, not its own
                // logger; without this a report missed exactly what the script did.
                boolean shared = line.logger != null
                    && (line.logger.equals(SHARED_LOGGER) || line.logger.startsWith(SHARED_UTIL_PREFIX));
                if (fromScript || shared || line.level.isGreaterOrEqual(Level.WARN))
                {
                    picked.add(line);
                }
            }
        }
        int from = Math.max(0, picked.size() - maxLines);
        StringBuilder out = new StringBuilder();
        for (Line line : picked.subList(from, picked.size()))
        {
            out.append(TIME.format(Instant.ofEpochMilli(line.time))).append(' ')
                .append(line.level).append(' ')
                .append('[').append(shortLogger(line.logger)).append("] ")
                .append(line.text).append('\n');
        }
        return out.toString();
    }

    private static String shortLogger(String logger)
    {
        if (logger == null)
        {
            return "?";
        }
        int dot = logger.lastIndexOf('.');
        return dot >= 0 ? logger.substring(dot + 1) : logger;
    }
}

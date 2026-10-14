package net.runelite.client.plugins.projectx;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.RuneLiteProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.zip.GZIPOutputStream;

/**
 * One log file per client launch, under {@code .runelite/logs/projectx}.
 *
 * RuneLite's own {@code client.log} is shared by every client on the machine, so with
 * two clients open, or after a restart, the lines a report needed were mixed in with
 * or rolled away by another session. Here each launch writes its own file, named for
 * when it started and its process id, and nothing is ever overwritten: a report can
 * send every session from the past day, whichever script was running in it.
 */
@Slf4j
public final class ProjectXSessionLog
{
    public static final File DIR = new File(RuneLite.LOGS_DIR, "projectx");

    private static final String PREFIX = "session_";
    private static final String PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";
    /** A session that logs more than this moves on to a second and third file. */
    private static final String PART_SIZE = "40MB";
    private static final int PARTS = 3;
    private static final long KEEP_MS = 14L * 24 * 60 * 60 * 1000;
    private static final long KEEP_BYTES = 1024L * 1024 * 1024;

    private static File current;

    private ProjectXSessionLog()
    {
    }

    /** Starts this session's file, once. Never throws: logging must not stop the client. */
    public static synchronized void install()
    {
        if (current != null)
        {
            return;
        }
        try
        {
            if (!DIR.isDirectory() && !DIR.mkdirs())
            {
                return;
            }
            prune();

            String started = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String base = PREFIX + started + "_" + ProcessHandle.current().pid();
            File file = new File(DIR, base + ".log");

            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern(PATTERN);
            encoder.setCharset(StandardCharsets.UTF_8);
            encoder.start();

            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setContext(context);
            appender.setName("PROJECTX_SESSION");
            appender.setFile(file.getAbsolutePath());
            appender.setAppend(true);
            appender.setEncoder(encoder);

            FixedWindowRollingPolicy rolling = new FixedWindowRollingPolicy();
            rolling.setContext(context);
            rolling.setParent(appender);
            rolling.setFileNamePattern(new File(DIR, base + ".part%i.log").getAbsolutePath());
            rolling.setMinIndex(1);
            rolling.setMaxIndex(PARTS - 1);
            rolling.start();

            SizeBasedTriggeringPolicy<ILoggingEvent> trigger = new SizeBasedTriggeringPolicy<>();
            trigger.setContext(context);
            trigger.setMaxFileSize(FileSize.valueOf(PART_SIZE));
            trigger.start();

            appender.setRollingPolicy(rolling);
            appender.setTriggeringPolicy(trigger);
            appender.start();
            if (!appender.isStarted())
            {
                return;
            }
            context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
            current = file;

            log.info("Session log {} | Project X {} ({}) | Java {} {} | {} {} {}",
                file.getName(),
                RuneLiteProperties.getProjectXVersion(), RuneLiteProperties.getProjectXCommit(),
                System.getProperty("java.vendor"), System.getProperty("java.runtime.version"),
                System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
        }
        catch (Exception | LinkageError e)
        {
            log.warn("Session log not started: {}", e.toString());
        }
    }

    /** This launch's log file, or null when it could not be started. */
    public static synchronized File current()
    {
        return current;
    }

    /** Session files, newest first. */
    static List<File> sessions(File dir)
    {
        File[] files = dir.listFiles((d, name) -> name.startsWith(PREFIX) && name.endsWith(".log"));
        if (files == null)
        {
            return new ArrayList<>();
        }
        List<File> list = new ArrayList<>(Arrays.asList(files));
        list.sort(Comparator.comparingLong(File::lastModified).reversed());
        return list;
    }

    /** Drops sessions older than two weeks, then the oldest until the folder is under 1 GB. */
    private static void prune()
    {
        long now = System.currentTimeMillis();
        long total = 0;
        for (File f : sessions(DIR))
        {
            if (now - f.lastModified() > KEEP_MS || total + f.length() > KEEP_BYTES)
            {
                f.delete();
            }
            else
            {
                total += f.length();
            }
        }
    }

    /** What {@link #bundle} produced: the gzipped text and what went into it. */
    public static final class Bundle
    {
        public final byte[] gzip;
        public final int files;
        public final long bytes;

        Bundle(byte[] gzip, int files, long bytes)
        {
            this.gzip = gzip;
            this.files = files;
            this.bytes = bytes;
        }
    }

    /**
     * Every session written to in the past {@code hours}, oldest first, each under a
     * header naming its file, gzipped. This launch is always in it. When the sessions
     * add up to more than {@code maxBytes}, the oldest go first, and a single session
     * bigger than what is left keeps its end, which is where the problem is.
     * {@code scrub} is applied to each file's text before it is packed.
     */
    public static Bundle bundle(int hours, long maxBytes, UnaryOperator<String> scrub) throws IOException
    {
        return bundle(DIR, current(), hours, maxBytes, scrub);
    }

    static Bundle bundle(File dir, File mine, int hours, long maxBytes, UnaryOperator<String> scrub) throws IOException
    {
        long since = System.currentTimeMillis() - hours * 3_600_000L;
        List<File> candidates = sessions(dir);
        // This launch first, then the rest newest first, so the budget goes on what
        // happened here and most recently -- other clients open at the same time
        // write just as often.
        if (mine != null && candidates.remove(mine))
        {
            candidates.add(0, mine);
        }

        List<File> picked = new ArrayList<>();
        List<Long> takes = new ArrayList<>();
        long left = maxBytes;
        for (File f : candidates)
        {
            if (left <= 0)
            {
                break;
            }
            if (f.lastModified() < since && !f.equals(mine))
            {
                continue;
            }
            long take = Math.min(f.length(), left);
            picked.add(f);
            takes.add(take);
            left -= take;
        }

        // Oldest first in the bundle, so it reads in the order things happened.
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < picked.size(); i++)
        {
            order.add(i);
        }
        order.sort(Comparator.comparingLong(i -> picked.get(i).lastModified()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long[] bytes = {0};
        try (OutputStream gz = new GZIPOutputStream(out))
        {
            for (int i : order)
            {
                File f = picked.get(i);
                long take = takes.get(i);
                write(gz, bytes, "===== " + f.getName()
                    + (f.equals(mine) ? " (the session that sent this report)" : "")
                    + (take < f.length() ? " (last " + take / 1024 + " KB of " + f.length() / 1024 + " KB)" : "")
                    + " =====\n");
                try (BufferedReader in = tail(f, take))
                {
                    String line;
                    while ((line = in.readLine()) != null)
                    {
                        write(gz, bytes, scrub.apply(line) + "\n");
                    }
                }
                write(gz, bytes, "\n");
            }
        }
        return new Bundle(out.toByteArray(), picked.size(), bytes[0]);
    }

    private static void write(OutputStream out, long[] count, String text) throws IOException
    {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        out.write(b);
        count[0] += b.length;
    }

    /** The last {@code take} bytes of {@code f}, starting at a whole line. */
    private static BufferedReader tail(File f, long take) throws IOException
    {
        long length = f.length();
        InputStream in = new BoundedInputStream(Files.newInputStream(f.toPath()), length);
        long skip = Math.max(0, length - take);
        while (skip > 0)
        {
            long n = in.skip(skip);
            if (n <= 0)
            {
                break;
            }
            skip -= n;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        if (length > take)
        {
            reader.readLine(); // the rest of a line cut in half
        }
        return reader;
    }

    /**
     * Stops at the length the file had when the bundle was planned: this launch's own
     * file keeps growing while it is read, not least with the report's own lines.
     */
    private static final class BoundedInputStream extends FilterInputStream
    {
        private long left;

        BoundedInputStream(InputStream in, long limit)
        {
            super(in);
            this.left = limit;
        }

        @Override
        public int read() throws IOException
        {
            if (left <= 0)
            {
                return -1;
            }
            int b = super.read();
            if (b >= 0)
            {
                left--;
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            if (left <= 0)
            {
                return -1;
            }
            int n = super.read(b, off, (int) Math.min(len, left));
            if (n > 0)
            {
                left -= n;
            }
            return n;
        }

        @Override
        public long skip(long n) throws IOException
        {
            long skipped = super.skip(Math.min(n, left));
            left -= skipped;
            return skipped;
        }
    }
}

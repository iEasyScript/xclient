package net.runelite.client.plugins.projectx;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectXSessionLogTest
{
    private static final long HOUR = 3_600_000L;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File session(String name, String text, long ageMs) throws IOException
    {
        File f = new File(folder.getRoot(), name);
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        f.setLastModified(System.currentTimeMillis() - ageMs);
        return f;
    }

    private static String unzip(byte[] gz) throws IOException
    {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gz)))
        {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void sendsEverySessionFromTheDayOldestFirst() throws IOException
    {
        session("session_a_1.log", "Started Tithe Farm\nTithe Farm stuck at the seed table\n", 5 * HOUR);
        session("session_b_2.log", "Started Agility\n", 3 * HOUR);
        File mine = session("session_c_3.log", "Started Fishing\n", 0);
        session("session_old_4.log", "two days ago\n", 48 * HOUR);
        session("client.log", "not ours\n", 0);

        ProjectXSessionLog.Bundle bundle = ProjectXSessionLog.bundle(folder.getRoot(), mine, 24, 1 << 20, s -> s);
        String text = unzip(bundle.gzip);

        assertEquals(3, bundle.files);
        assertTrue(text.contains("Tithe Farm stuck at the seed table"));
        assertTrue(text.indexOf("session_a_1") < text.indexOf("session_b_2"));
        assertTrue(text.indexOf("session_b_2") < text.indexOf("session_c_3"));
        assertTrue(text.contains("session_c_3.log (the session that sent this report)"));
        assertFalse(text.contains("two days ago"));
        assertFalse(text.contains("not ours"));
        assertEquals(text.getBytes(StandardCharsets.UTF_8).length, bundle.bytes);
    }

    @Test
    public void overBudgetKeepsThisSessionAndTheEndOfTheNext() throws IOException
    {
        StringBuilder older = new StringBuilder();
        for (int i = 0; i < 100; i++)
        {
            older.append("older line ").append(i).append('\n');
        }
        session("session_a_1.log", older.toString(), 2 * HOUR);
        // Another client open at the same time wrote more recently than this one.
        session("session_b_2.log", "other client\n", 0);
        File mine = session("session_c_3.log", "this client\n", HOUR);

        String text = unzip(ProjectXSessionLog.bundle(folder.getRoot(), mine, 24, 200, s -> s).gzip);

        assertTrue(text.contains("this client"));
        assertTrue(text.contains("other client"));
        assertTrue(text.contains("older line 99"));
        assertFalse(text.contains("older line 1\n"));
        assertTrue(text.contains("session_a_1.log (last 0 KB of 1 KB)"));
        // Cut on a line boundary: no half line at the start.
        String afterHeader = text.substring(text.indexOf("=====\n", text.indexOf("session_a_1")) + 6);
        assertTrue(afterHeader.startsWith("older line "));
    }

    @Test
    public void scrubsEachLine() throws IOException
    {
        File mine = session("session_a_1.log", "token abc123secret\nfine\n", 0);
        String text = unzip(ProjectXSessionLog.bundle(folder.getRoot(), mine, 24, 1 << 20,
            s -> s.replace("abc123secret", "[redacted]")).gzip);
        assertTrue(text.contains("token [redacted]"));
        assertFalse(text.contains("abc123secret"));
    }
}
